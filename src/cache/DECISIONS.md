# Cache: Decision Log

Format: decision, why, rejected alternatives, trade-offs accepted. Chat history lives in `FEEDBACK.md`; roadmap in `PLAN.md`.

## D1. Reads consult the write-behind buffer (cache -> buffer -> DB)
**Status:** direction agreed, implementation pending.

**Problem:** with `WriteBehind`, a key can be evicted from the cache before the 2 s flush. It then exists only in the
pending `map`, and `App.get` (cache, then DB) returns null or a stale value.

**Decision:** on a cache miss, look in the pending buffer before the DB.

**Why this order:** `flush()` does `database.put` then `map.remove`. A buffer miss therefore implies the DB already has
the value. DB-first can miss both. The buffer is also newer than the DB, so it must win.

**Rejected**
- *Write-back on eviction (dirty entries):* DB I/O on the synchronized store path blocks all readers and writers.
- *Bigger cache than buffer:* heuristic only. The buffer is unbounded (DB outage) and LRU orders by access, not dirtiness.
- *Document and accept (read-your-writes violated):* returns null/stale for a value we accepted.

**Accepted trade-offs**
- Cache and buffer are in memory: unflushed writes are lost on a crash (crash window = flush interval). Use `WriteThrough`
  or a write-ahead log if that is not acceptable.
- Single process only. With several servers each has its own cache and buffer; that needs a shared cache layer.

**Open:** read-through repopulation of the cache (stale-overwrite race), who reads the buffer (`App` vs `WritePolicy`),
behaviour during a DB outage.

## D2. Read-fill race (stale DB value put into the cache after a newer write)
**Status:** decided (phase 1: LRU + WriteBehind). Common race closed by an atomic put-if-absent on the store (one lock, check inside the lock) plus the pending-buffer check in `WriteBehind.fillCache`, which since D4 runs inside `map.computeIfAbsent` (buffer check and cache fill are one atomic step per key; a concurrent `write` waits). That closes the "write finished, then evicted, before the fill" gap. The remaining rare case (write **and flush** both finish inside one reader's gap, so buffer and cache both look empty) is accepted as a known limitation; a TTL is the safety net if added later.

**Problem:** a reader misses cache and buffer, reads `(k, a)` from the DB, a newer `write(k, b)` happens (and may be evicted and
flushed), then the reader puts `a` into the cache. The cache is now stale for that key, and nothing signals it. Separate from D1.

**Options considered**
- *TTL only:* does not prevent the wrong entry, only bounds how long it lives.
- *Global write counter gate (+ TTL):* loader reads the counter before the DB read and fills the cache only if it is unchanged.
  Safe but imprecise (an unrelated write cancels a fill). No per-key state. Needs "check counter, then put" atomic with `write()`.
- *No fill from DB reads:* no race; cold reads always hit the DB.

**Rejected**
- *Per-key lock across the DB read:* slow work under a lock, serializes that key.
- *Conditional put (only if absent):* evicted key looks absent, so a stale put still succeeds.
- *Per-key versions in cache entry + DB:* DB-side check orders flushes (useful for overlapping flushes or multi-server) but does
  not protect the cache, and an evicted key's version is forgotten, so the stale put is accepted.
- *In-flight load marker:* a second shared structure with its own races and cleanup; more complexity than benefit.

**Constraint on the design:** the "fill cache after DB read" step must be one isolated place in the read path so the choice can be
changed without touching the rest of `get`.

## Scope
Phase 1 is LRU + WriteBehind. `LFUStore.fillCache` and read-through for `WriteThrough`/`WriteAround` are placeholders, to be done in phase 2.

## D3. Null keys and values are rejected at the `App` boundary
**Status:** decided (phase 1).

`App.get` and `App.put` throw `IllegalArgumentException` for a null key (and for a null value in `put`) as the first statement, before any
state is touched (this was item X: `WriteBehind.write()` used to update the cache before `ConcurrentHashMap.put` threw on null).
Consequence: `get` returning `null` always means "absent". Inner layers (`WritePolicy`, `Store`) trust `App`; they are public, so a
direct caller could still bypass the check. Accepted for phase 1.

## D4. Two concurrent writers to the same key: one atomic per-key step
**Status:** decided and implemented (phase 1, `WriteBehind.write` with `compute`, `WriteBehind.fillCache` with `computeIfAbsent`). Not verified by a concurrency test (skipped by choice).

**Problem:** `write` did `writeToCache(k, v)` then `map.put(k, v)`, each thread-safe alone. Two writers could interleave so the
cache ends with one value and the buffer with the other; a reader then sees `v2`, and after an eviction `v1`, with no third write.

**Decision:** both steps run inside `map.compute(key, ...)` (`ConcurrentHashMap` locks that key while the function runs). The
function updates the cache and returns the new value, so the buffer and the cache always end with the same winner.
Lock order: shutdown read-lock, then the map's per-key lock, then the `LRUStore` lock. `LRUStore` never calls back into the policy
or the buffer, so there is no reverse order and no deadlock.

**Rejected**
- *`synchronized (key)`:* locks the key object, not its value; two equal keys that are different objects do not exclude each other.
- *Compare-and-set (`get` then `replace(k, old, new)`) without a retry loop:* throws on the first write (`null`), silently loses a
  write when the compare fails, and does not cover the cache update at all.
- *Option 4, invalidate the cache on write:* changes the whole read/write flow and brings its own invalidate-vs-fill race.
- *Option 5, cache takes whatever the buffer holds:* still two separate steps, so still racy.
- *Striped locks / one global lock:* both correct; `compute` was chosen because it needs no extra lock objects and no identity rule.

**Checked against the other paths**
- *Flusher* (`get`, `database.put`, `remove(key, value)`): `remove` deletes only if the buffer still holds the flushed value, so a
  newer different value stays for the next flush. If the newer write has the same value, removing it is harmless: the DB has it.
- *Read-fill* (D2): `LRUStore.fillCache` re-checks presence under the store lock, so a write that completed in between wins.
