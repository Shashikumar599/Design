# Cache Design

Part 1 is the **revision sheet**: what was built, why, and how to present it in an interview. Part 2 (further down) is the original
design discussion from before implementation, kept for history. Full reasoning per decision: `DECISIONS.md` (D1-D7).

---

# Part 1: Revision notes and interview prep

## 1. What was built (one line per piece)

```
 client
   | get / put / shutdown
   v
 App<K,V>  (the facade: validates nulls, counts hit/miss, routes everything)
   |  cache hit? -> return            miss / any write -> WritePolicy
   v                                        |
 Cache<K,V> ---> Store<K,V>          WritePolicy<K,V>  (write, read, shutdown)
                  LRUStore                  ^
                  (LFUStore stub)    WritePolicyBase (abstract: cache, database, shutdown flag, ensureOpen)
                                            ^
                              WriteThrough | WriteAround | WriteBehind
                                            |
                                     Database<K,V> ---> DB<K,V> ---> ActualDB (ConcurrentHashMap)
```

| Piece | What it does | Key detail |
|---|---|---|
| `Store` / `LRUStore` | entries + eviction | `HashMap` + doubly linked list; **one private lock** held for the whole of every public method (`get` mutates order too); `fillCache` = put-if-absent under that lock |
| `Cache` | owns the store, built from a factory (`LRUStore::new`) | pass-through today |
| `WritePolicy` | the **consistency strategy**: owns writes **and** cache-miss reads | interface = exactly what `App` calls: `write`, `read`, `shutdown` |
| `WritePolicyBase` | shared state for policies | `cache`, `database`, one `AtomicBoolean` shutdown flag, `markShutdown()`, `ensureOpen()` |
| `WriteThrough` | cache put, then DB write; invalidate on DB failure | 16 **striped locks** (`floorMod(hashCode, 16)`) per write (D6) |
| `WriteAround` | DB write, tear up ticket, invalidate | per-read **tickets / leases** stop stale fills (D5) |
| `WriteBehind` | cache + buffer now, DB every 2 s | `map.compute` for cache+buffer (D4); flusher: DB put then `remove(key, value)`; read: buffer -> DB (D1); fill via `computeIfAbsent` (D2); shutdown: read-write lock + final flush (D7) |
| `App` | facade | rejects null key/value (D3); `get`: cache hit -> return, miss -> `writePolicy.read` |
| `Main` | four commented demos | LRU, WriteBehind, WriteAround basic, WriteAround race (`SlowDatabase`) |

## 2. Request flows

**`get(k)`** (all policies): `App` -> `cache.get` -> hit: return. Miss: `writePolicy.read(k)`:
- WriteThrough: DB read -> if not null, `fillCache` (put-if-absent).
- WriteAround: take ticket -> DB read -> fill **only if my ticket survived** (inside `computeIfPresent`) -> `finally` remove my ticket.
- WriteBehind: buffer -> DB -> fill via `computeIfAbsent` on the buffer (fill only if no pending write).

**`put(k, v)`**: `App` validates -> `writePolicy.write`:
- WriteThrough: lock stripe -> cache put -> DB write (fail: invalidate, rethrow with cause).
- WriteAround: DB write -> `tickets.remove(k)` -> invalidate.
- WriteBehind: check flag -> read lock -> check again -> `map.compute(k, cache.put inside)`.

**`shutdown()`**: flag flips once (`compareAndSet`); every later `read`/`write` throws. WriteBehind additionally takes the write lock,
stops the flusher (`awaitTermination` 5 s, else `FlushTimeoutException`) and does a final flush.

## 3. Decisions in one line each (details: `DECISIONS.md`)

| # | Problem | Decision |
|---|---|---|
| D1 | WriteBehind: key evicted before flush -> read returns null/stale | reads go cache -> **buffer** -> DB (buffer miss implies DB has it, because flush = DB put **then** remove) |
| D2 | reader fills an old DB value after a newer write | put-if-absent fill + buffer check, now atomic via `computeIfAbsent`; accepted: write **and** flush inside one read's gap |
| D3 | nulls (`ConcurrentHashMap` rejects them, "absent" ambiguity) | reject null key/value at `App`; `null` from `get` always means absent |
| D4 | two writers, same key: cache and buffer end with different values | `ConcurrentHashMap.compute`: cache + buffer update as one atomic step per key |
| D5 | WriteAround stale fill (reader read before a write, fills after its invalidate); **not rare** (one DB round trip) | per-read tickets: reader registers before DB read, fills only if its ticket survived; writer tears up after its DB write |
| D6 | WriteThrough two writers, same key: cache and DB disagree | 16 striped locks across cache put + DB write; reads need no lock (writer's put beats the stale fill) |
| D7 | shutdown meant different things per policy | one flag in the base; all `read`/`write` rejected after; lock only where a flush must finish first |

## 4. Consistency model (what each policy promises)

| Policy | After `put(k, v)` returns | A concurrent `get(k)` | On crash |
|---|---|---|---|
| WriteThrough | cache and DB have `v` | may see `v` before the DB has it (cache first); if the DB write then fails, a reader may have seen a value never stored | nothing lost |
| WriteAround | DB has `v`, no stale `k` in cache | may see the old value until the put finishes; a raced read leaves the cache **cold, never stale** | nothing lost |
| WriteBehind | cache + buffer have `v`; DB within ~2 s | sees `v` (cache or buffer) | up to one flush interval of writes lost |

**Accepted gaps (say them before the interviewer finds them):** eviction inside a reader's gap can fill a stale value (D2 WriteBehind:
only if the write was also flushed; D6 WriteThrough); cache hits are served after shutdown (check belongs in `App`, D7); WriteBehind
buffer is unbounded during a DB outage (would add a size limit + backpressure); single process only.

## 5. Concurrency toolbox (the Java behind it)

| Tool | Used for | Rule to remember |
|---|---|---|
| one private lock object | `LRUStore` | `get` mutates LRU order, so a read-write lock gains nothing; private so nobody else can lock it |
| `ConcurrentHashMap.compute` / `computeIfAbsent` / `computeIfPresent` | D2, D4, D5 | runs atomically **per key**; keep it short, **no I/O**, never touch the same map inside; return `null` = remove / insert nothing |
| two-arg `remove(key, value)` | flusher, ticket cleanup | removes only if the value is still the one you expect |
| tickets (`new Object()`, compared with `==`) | D5 | identity says *whose* ticket; map size = reads in progress (Little's law), not keys |
| striped locks | D6 | `Math.floorMod` (not `%`, not `Math.abs`: `abs(MIN_VALUE)` is negative); sharing a stripe = waiting, never wrong |
| `ReentrantReadWriteLock` | WriteBehind shutdown | writes = read side, shutdown = write side: no write lands after the final flush |
| `AtomicBoolean.compareAndSet` | shutdown once | returns who won, so a second call does nothing |
| `ScheduledExecutorService` | flusher | `shutdown` + bounded `awaitTermination`; unlock in `finally` |

**Why not `synchronized(key)`:** locks the key *object*; two equal keys can be different objects -> two locks, race back.
**Why not CAS (`replace(k, old, new)`) for D4:** needs a retry loop, fails on first write (`null`), and does not cover the second structure (cache).
**Deadlock check:** a deadlock needs two locks taken in opposite orders. Order here is always outer (shutdown lock / map key / stripe)
-> `LRUStore` lock, and `LRUStore` never calls out.

## 6. Design lessons (the "why" behind the shape)

1. **Design an interface from the caller, not from the steps.** `WritePolicy` once listed every internal step; `App` hard-coded
   WriteBehind's read path and every new policy forced changes everywhere. Now it has the 3 methods `App` calls (Strategy pattern).
2. **Reads must follow the write strategy.** Every hard bug (D2, D4, D5) was a read meeting a write; only the policy knows how a read
   stays correct with its writes. So the "write policy" is really a consistency policy.
3. **Prefer the order whose failure heals itself:** WriteAround = DB write, then invalidate (the reverse leaves a permanent stale value).
4. **`abstract` only where behaviour differs** (`fillCache`); identical bodies belong once in the base.
5. **Static type decides what you may call, dynamic type decides what runs** (why removing `shutdown` from the interface broke `App`).
6. Interfaces are public contracts: no `protected` members; `private abstract` is a contradiction; overrides may widen visibility, never narrow.

## 7. The 45-minute interview version

| Time | Do | Notes |
|---|---|---|
| 5 min | **Ask** | capacity + eviction (LRU/LFU)? backing DB? which write policy? thread-safe? single node? TTL? null values? |
| 5 min | **Draw** | facade -> `Store` (eviction strategy); facade -> policy (consistency strategy) -> DB |
| 20 min | **Code** | `LRUStore` (map + doubly linked list + one lock), `CachePolicy` interface, **one** policy (WriteAround: shortest, and it has the best race story), the facade |
| 10 min | **Talk** | the other two policies; D4 (same-key writers), D5 (stale set + tickets), shutdown; draw one interleaving table |
| 5 min | **Extend** | TTL, LFU, metrics, distributed (consistent hashing, Redis, invalidation over the network) |

Rule: a working core + clear talk about the hard parts beats a half-finished everything.

## 8. Likely questions, one-line answers

1. *Why separate `Store` from the policy?* Eviction and consistency change independently; any store works with any policy.
2. *Why not `synchronized` everywhere?* One global lock serializes all keys and is held across DB I/O; striping / per-key atomics keep keys independent.
3. *DB is down?* WriteThrough: put fails, cache invalidated. WriteBehind: entries stay buffered, retried each flush (add a bound + backpressure).
4. *Two threads write one key?* One atomic step per key (D4 `compute`, D6 stripes), otherwise layers disagree.
5. *A read races a write?* The **stale set** problem; tickets/leases (Facebook memcache), or per-key atomic load (Caffeine), or accept + TTL.
6. *Distributed?* Shard by consistent hashing; invalidations over the network or from the DB commit log (CDC); leases/versions for stale sets.
7. *How do you test concurrency?* A slow fake DB to force interleavings (`SlowDatabase`), multi-thread stress loops, assert invariants.
8. *What would you change?* Naming (`App` -> facade, `WritePolicy` -> `CachePolicy`), drop pass-through layers (`Cache`, `Database`), inject the `DB`, add tests.

## 9. Not built, explain in two minutes

- **TTL:** store an expiry timestamp per entry; on `get`, if expired, remove and treat as a miss (lazy); optionally a background sweep
  to free memory. Interaction: expiry is also the safety net for the accepted stale-fill gaps.
- **LFU in O(1):** `key -> node` map, `frequency -> doubly linked list` map, and a `minFreq` pointer. `get` moves the node to the
  `freq+1` list; eviction removes the LRU node of the `minFreq` list; a new key starts at frequency 1 (`minFreq = 1`).
- **`delete(k)`:** a 4th interface method; each policy differs (WriteBehind: the key may still be in the buffer, so a delete must be
  buffered too, as a *tombstone*).
- **Metrics:** hit ratio, load latency, evictions, buffer size.

## 10. Still open in code (phase B cleanup)

- B2: delete `App.storeToCache` / `storeToDB` (they bypass every guarantee above).
- B4: inject the `DB` into `Database` (or drop `Database`); delete unused `CacheDB`; `delete` stubs; print in `Cache.invalidateKey`.
- B3: `App` fields `private final` (`final` also gives safe publication across threads).
- B1: base-class helpers: concrete where identical, remove the empty ones.
- Tests: the D4 two-thread test was skipped.

---

# Part 2: Original design discussion (before implementation)

Goal: understand the current design, then extend it with **write policies** and **concurrency** in small phases, each one working before the next starts.

No full solutions here. Each phase has a goal, the design decision, hints, and a "done when" check, so you write the code.

---

## 1. Where we are today

```
 Client
   |  get(k) / put(k, v)
   v
+-----------------------+
| Cache<K,V>            |   owns the store; builds it once from a Function<Integer, Store<K,V>>
|  - Store<K,V> store   |
+-----------+-----------+
            |  get / put (just forwards)
            v
+-----------------------+
| Store<K,V> (interface)|   the eviction strategy
+-----------+-----------+
     |             |
 LRUStore      LFUStore (stub)
 HashMap +     not implemented yet
 doubly linked
 list
```

What the pieces do:

| Piece | Responsibility |
|---|---|
| `Cache<K,V>` | The public API. Today it only forwards calls. |
| `Store<K,V>` | **Where entries live and who gets evicted** (LRU, LFU). Memory only. |
| `LRUStore` | Strategy: evict the least recently used entry. |
| `Function<Integer, Store<K,V>>` | Lets the client choose the strategy while `Cache` builds and owns the store. |

What it does **not** have yet:
1. No backing data source (a DB, a file, a remote service). Everything lives in memory only, so "write policy" has nothing to write *to*.
2. No thread safety. Two threads calling `put` can corrupt the linked list.
3. `LFUStore` is a stub, and `LRUStore` still has open items (see `FEEDBACK.md`).

---

## 2. Three separate concerns (keep them apart)

The most common design mistake is mixing these. Each answers a different question:

| Concern | Question it answers | Lives in | Status |
|---|---|---|---|
| **Eviction policy** | "Memory is full. Which entry goes?" | `Store` | done (LRU), LFU stub |
| **Write policy** | "When a write arrives, when does the backing data source see it?" | new, near `Cache` | not started |
| **Concurrency** | "What happens when many threads call us at once?" | `Cache` (outer layer) | not started |

Rule of thumb: **`Store` should never know about the database or about threads.** It is a plain in-memory structure. `Cache` is where the decisions are combined.

---

## 3. Write policies

### 3.1 What problem do they solve?
A cache usually sits in front of something slower and more permanent. Call it the **backing source** (DB, disk, remote API). Then two questions appear:
- On a **read miss**, who loads the value?
- On a **write**, do we update the cache, the source, or both, and in what order?

### 3.2 New abstraction: the backing source
Add a second interface, separate from `Store`. Suggested name: `DataSource<K,V>` (avoid reusing "Store", which already means in-memory entries).

Capabilities it needs (in plain words): load a value by key, and save a value by key.

Hint: for testing you can write a tiny in-memory implementation backed by a `HashMap` that sleeps a few milliseconds and counts calls. A call counter will make the differences between the policies **visible**.

### 3.3 The write policies

| Policy | On `put(k, v)` | Pros | Cons |
|---|---|---|---|
| **Write-through** | Write to source **and** cache, caller waits for both | Simple, cache and source never disagree | Writes are as slow as the source |
| **Write-back** (write-behind) | Write to cache only, mark entry **dirty**, flush to source later (on eviction, on a timer, or on shutdown) | Fastest writes, can batch | Data loss if we crash before flushing, more complex |
| **Write-around** | Write to source only, skip the cache (and invalidate any cached copy) | Avoids filling the cache with data that is rarely read | The next read is a miss |

Related read-side terms people mix up with write policies:

| Name | Meaning |
|---|---|
| **Cache-aside** | The *application* checks the cache, and on a miss loads from the source and puts in the cache itself. The cache is passive. |
| **Read-through** | The *cache* loads from the source on a miss. The caller never touches the source. |

Write-through and write-back are usually paired with read-through.

### 3.4 Where does the policy plug in?
Same trick you already used for eviction: **Strategy pattern**. A `WritePolicy<K,V>` interface, and `Cache` calls it from `put` (and maybe `get` for read-through).

```
Cache.put(k, v)
   |
   v
WritePolicy.write(k, v, store, source)
        |                 |
 WriteThrough       WriteBack          WriteAround
 store.put +        store.put +        source.save +
 source.save        mark dirty         invalidate in store
```

Questions to decide before coding (write your answers in this file):
1. Does the policy get the store and the source passed in each call, or hold them as fields? Which is easier to test?
2. Write-back needs to remember **which keys are dirty**. Does that belong in `Store`, in the policy, or in a new wrapper? (Hint: `Store` should stay ignorant of the source. What does that imply?)
3. With write-back, an LRU eviction must **save the evicted entry first** if it is dirty. `Store.put` evicts silently today. What does `Store` need to tell the outside world so the cache can react? (Hint: look at what `put` returns, or consider an eviction listener.)

### 3.5 Failure modes to be able to discuss
- Write-through: source write succeeds, cache write fails (or the reverse). What do you do? Which order is safer?
- Write-back: process crashes with dirty entries. How do you reduce the window? (periodic flush, flush on shutdown, a write-ahead log.)
- Write-around: reading just after a write. Why must we invalidate the cached copy?

---

## 4. Concurrency

### 4.1 Why the current code is unsafe
`LRUStore.get` **changes** the structure (moves a node to the front), so even a "read" is a write. Two threads can interleave inside `addtoHead` / `removeNode` and corrupt `prev`/`next` pointers, or let the `HashMap` and the list disagree. `HashMap` itself is not thread-safe either.

### 4.2 Options, from simplest to most complex

| Option | Idea | Throughput | Complexity | Verdict |
|---|---|---|---|---|
| **A. Coarse lock** | One lock around every `get`/`put` (`synchronized` or `ReentrantLock`) | Low under heavy contention | Very low | **Start here** |
| **B. Read-write lock** | Many readers, one writer | No gain for LRU | Low | **Does not work well**: `get` mutates the order, so it needs the write lock anyway |
| **C. Lock striping / sharding** | Split keys into N independent caches, each with its own lock | Good | Medium | Natural next step |
| **D. `ConcurrentHashMap` + approximate LRU** | Lock-free-ish reads, record access in a buffer and apply it in batches (what Caffeine does) | Very high | High | Know it exists; do not build it |

### 4.3 Where to put the lock?
Candidates: inside `LRUStore`, or in `Cache`.

| Place | Good | Bad |
|---|---|---|
| Inside each `Store` | Each store protects itself | Every new store must remember to do it |
| In `Cache` | One place, and it can cover **store + source + write policy together** | Locks are held while calling the slow source (see below) |

Recommendation: in `Cache`, because write policies need **one critical section across several steps** (for example "update store, then mark dirty").

### 4.4 Interaction between write policies and locks (the tricky part)
- Holding a lock while calling a slow source (write-through, read-through) blocks every other thread. For phase 1 accept it; know the cost.
- Better later: release the lock around the source call, then re-check state. This introduces races (two threads loading the same key), which leads to:
  - **Thundering herd / cache stampede**: many threads miss the same key and all hit the source. Fix: per-key "in flight" loading so only one loads.
- Write-back needs a **background flusher thread** (or scheduled executor) that reads dirty entries while others write. This is a second thread using the same structures, so the lock must cover it too.

### 4.5 How to prove it is broken (and later fixed)
Write a small stress test: N threads doing random `put`/`get` on a small capacity for a few seconds, then assert invariants (size never above capacity, no exception, list length equals map size). Run it **before** adding the lock and watch it fail; run it again **after**. Without this you only believe it is safe.

---

## 5. Which first? Recommended order

You asked which can be picked first and how to build slowly. Recommendation:

> **Finish the single-thread core, add a coarse lock early, then write policies, then finer concurrency.**

Reasoning:
1. A coarse lock is a tiny change, and doing it early means every later feature is built on a correct base instead of retrofitted.
2. Write policies change what a "single operation" means. They decide what the lock must cover, so build them **after** the lock exists but **before** optimising the lock.
3. Optimising concurrency (striping) before the feature set is stable means redoing it.

---

## 6. Phases (each ends in something you can run)

### Phase 0: Make the base trustworthy
- Finish `LRUStore` open items from `FEEDBACK.md`: map vs list responsibilities, capacity validation, `put` updating an existing key.
- Implement `LFUStore` (or park it and use FIFO as the second policy).
- Replace printing in `Main` with checks (assertions or a small test class).
- **Done when:** a test covering miss, hit, eviction order, update of existing key, capacity 1 passes, for every store.

### Phase 1: Coarse thread safety
- One lock in `Cache` around `get` and `put`.
- Write the stress test from 4.5 first and watch it fail.
- **Done when:** the stress test passes repeatedly (run it 20 times).
- Decide: `synchronized` or `ReentrantLock`? Write a 3-line justification here (hint: `tryLock`, fairness, interruptibility).

### Phase 2: Backing source + write-through + read-through
- Add `DataSource<K,V>` and an in-memory fake with a call counter and a small delay.
- Add `WritePolicy` interface and **write-through** only.
- On a read miss, `Cache` loads from the source and puts into the store.
- **Done when:** a test proves the source count equals the number of writes, and that a read miss triggers exactly one load.

### Phase 3: More write policies
- **Write-around** first (smallest: skip the cache and invalidate).
- Then **write-back** with dirty tracking and flush-on-eviction (needs the eviction hook from 3.4).
- Then add a manual `flush()` and flush on shutdown.
- **Done when:** a test shows that after N writes, write-back made far fewer source calls than write-through, and that nothing is lost after `flush()`.

### Phase 4: Concurrency, round two
- Background flusher for write-back (scheduled, with a clean shutdown).
- Avoid holding the lock during slow source calls, plus per-key loading to prevent a stampede.
- Optional: sharding (option C) with N independent caches.
- **Done when:** the stress test passes with a slow source, and throughput improves versus Phase 2 in a rough timing test.

### Phase 5: Extras (only if time remains)
- TTL / expiry (lazy expiry on `get` is the simplest way to start).
- Stats (hits, misses, evictions).
- A builder for `Cache` so the constructor stays readable as options grow.

---

## 7. What to practice saying (1-hour interview)

You do not need all phases coded. A strong answer is Phase 0 + Phase 1 in code, then spoken answers for the rest:

- "Eviction, write policy and concurrency are separate concerns, so I keep each behind its own interface or layer."
- "Write-through is simple and consistent. Write-back is fast but can lose data, so I would flush on eviction, on a timer and on shutdown. Write-around avoids polluting the cache."
- "A read-write lock does not help LRU because `get` reorders. I would start with one lock, then shard if contention shows up."
- "Holding a lock during a slow source call blocks everyone, so I would move to per-key loading to avoid a stampede."

---

## 8. Open decisions (write your answer under each)

1. Should `Cache.get` on a miss load from the source (read-through), or leave that to the caller (cache-aside)?
2. Is the write policy chosen at construction time, or can it change at runtime? What breaks if it can change?
3. For write-back, is the dirty flag stored per entry (a field on `Node`) or in a separate set of keys? Trade-offs?
4. What should `get` return on a miss: `null`, `Optional<V>`, or the loaded value? (This affects `Store` too.)
5. Do we need `delete(k)`? Every write policy has to say what it does for deletes.


Check 