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

## D5. WriteAround stale fill: per-read tickets (leases)
**Status:** decided and implemented (phase 1, `WriteAround.read` / `write`).

**Problem:** with write-around (DB write, then invalidate), a reader that read the old value from the DB *before* a write can fill the
cache *after* that write's invalidate. The stale value stays until the next write or an eviction. Not rare: the window is about one DB
round trip, so it happens whenever a key is read and written concurrently.

**Decision:** a `ConcurrentHashMap<K, Object> tickets` holds one ticket per read in progress (no data).
- Reader: `put` a fresh `new Object()` before the DB read; after it, fill only if the entry is still **its** ticket (identity check),
  atomically inside `computeIfPresent` (mine -> fill, return `null` to remove; not mine -> return it unchanged); `finally` removes only
  its own ticket (two-argument `remove`).
- Writer: DB write, **then** tear up (`remove(key)`), **then** invalidate. Tear-up after the DB write so any reader registered later
  reads the new value; invalidate last so a fill that slipped in before the tear-up is wiped.
- `put` rather than `putIfAbsent`: the latest reader to start may fill; older readers whose ticket was replaced skip. Both are correct
  (removing or replacing a ticket can only prevent a fill, never cause a stale one); `put` never leaves a reader stuck behind a failed one.
Size of the map = reads in progress (bounded by reader threads), not keys.

**Rejected**
- *Accept + TTL:* the race is not rare here (one DB round trip), unlike D2.
- *Version per key:* the key is not in the cache at fill time, so there is nowhere to keep the version (a separate map grows with keys).
- *Per-key lock across the DB read (Caffeine-style atomic load):* holds a lock during I/O and blocks writers of that key (as in D2).
- *Latest-write map (writer stores its value, reader fills from it):* writer-vs-writer order between DB and map can differ (stale), and
  the map grows with every key written but not read, which is exactly the WriteAround workload.

**Trade-off accepted:** a raced read leaves the cache cold once (an extra miss), never wrong.

## D6. WriteThrough concurrency: striped write locks; read-fill relies on put-if-absent
**Status:** decided and implemented (phase 1, `WriteThrough`).

**Write vs write (same key):** `write` is "cache put, then DB write" (invalidate on DB failure). Two writers can interleave so the cache
ends with one value and the DB with the other, permanently. **Decision:** a fixed array of 16 lock objects; the stripe is
`floorMod(key.hashCode(), 16)`; cache put + DB write + the failure invalidate run inside `synchronized (stripe)`. Equal keys share a stripe,
so their writes serialize; different keys share one only with probability ~1/16 (extra waiting, never incorrect). The lock is held across
the DB write: accepted, because only writers of that stripe wait (unlike the earlier `synchronized` method, which serialized every write).
No deadlock: inside a stripe only the `LRUStore` lock is taken, nothing takes them in reverse, and no thread holds two stripes.

**Read vs write:** no lock and no tickets. A reader's late fill of an old DB value is skipped, because the writer already *put* the new
value in the cache and `fillCache` is put-if-absent. **Accepted gap:** the key is evicted between the writer's cache put and the reader's
fill; then the old value is filled and stays until the next write (same rare class as D2).

**Rejected**
- *`synchronized` on `write`:* one lock for all keys, held across DB I/O; also locks the public `this`.
- *Tickets (D5) in WriteThrough:* unnecessary for the common case (put-if-absent already skips the stale fill); only the rare eviction case would benefit.
- *Per-key lock map removed after the write:* removing an entry while another writer waits on it creates a second lock for the same key;
  correct only with reference counting, more code than striping.
- *`compute` around the write:* no I/O allowed inside it.

## D7. One shutdown contract for all policies
**Status:** decided and implemented (phase 1). App-level read check pending.

**Decision:** after `shutdown()`, `write` and `read` throw `IllegalStateException` for every policy.
- One flag (`AtomicBoolean`) in `WritePolicyBase`; `markShutdown()` flips it once and returns whether this call did (so a second
  `shutdown()` is a no-op); `ensureOpen()` is the single check, called first in every `read` and `write`.
- `WriteBehind` overrides `shutdown()`: marks the flag, then takes the write side of its read-write lock, stops the flusher and flushes.
  It keeps a second `ensureOpen()` inside the read lock in `write`, so no write can land after the final flush.
- `WriteThrough` / `WriteAround` need only the flag: they buffer nothing, so a write that passed the check just before shutdown and
  finishes after it still reaches the DB. **Condition:** this holds while `shutdown()` does not close the DB; if it ever does, they need the lock too.

**Known gap (accepted, to be fixed in `App`):** `App.get` serves cache hits without asking the policy, so after shutdown a cached key is
still returned while an uncached one throws. The policy cannot see hits; the check belongs in `App.get` (before `cache.get`).

**Rejected:** a flag per policy (three copies of the same rule, already diverged once); a `default` method on the interface (an interface
cannot hold the flag); Template Method (`final write` calling `doWrite`): considered, the helper was chosen as simpler.
