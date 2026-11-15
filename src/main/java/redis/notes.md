# Redis Notes

Brief reference. Part 1 is Redis itself, part 2 is Redis as a distributed lock. Hands-on code for part 2 is in this folder (`RedisClient`, `RedisLock`, `Client`, `IdGenerator`).

---

## 1. Redis basics

**What it is.** An in-memory data-structure server. Data lives in RAM; disk is only for persistence. Typical latency is microseconds, so the network round trip (RTT) dominates every call.

**Execution model.** Commands run one at a time on a single thread, so every command is atomic and there are no locks inside Redis. Slow commands (`KEYS *`, huge `ZRANGE`, long Lua) block everyone.

**Data types.** String (also counters via `INCR`), Hash, List, Set, Sorted Set (`ZSET`), Bitmap, HyperLogLog, Stream, Geo.

**Keys and expiry.** `SET k v EX 30` / `PX 30000` set a TTL; `TTL` / `PTTL` read it. Keys without TTL live forever. Name keys `type:id:field` (`user:42:profile`).

**Atomic building blocks.** `INCR/DECR`, `SET NX`, `SETEX`, `GETSET`, `MULTI/EXEC` (queued, not rolled back on error), `WATCH` (optimistic check-and-set), Lua `EVAL` (run a script as one atomic unit), pipelining (batch commands, fewer RTTs).

**Memory.** `maxmemory` (default 0 = unlimited) plus `maxmemory-policy`: `noeviction` (default, writes fail when full), `allkeys-lru/lfu`, `volatile-lru/lfu/ttl`, `*-random`. Inspect with `INFO memory`, `MEMORY USAGE key`. Never let an eviction policy touch data you can't lose (such as lock keys).

**Persistence.** RDB snapshots (compact, may lose recent writes) and AOF log (`appendonly yes`, fsync policy trades speed for durability). Either, both, or none (pure cache).

**Scaling and availability.**
- Replication: async primary to replicas (replicas can serve reads, may be slightly stale).
- Sentinel: monitors, auto-failover for a single shard.
- Cluster: 16384 hash slots spread across shards; a key lives on one shard; multi-key commands need the same slot (`{tag}`).

**Placement.** Same region and VPC (about 1 ms RTT) is normal; cross-AZ 1-2 ms; cross-region 20-150 ms (avoid for hot paths).

**Operations.** `redis-cli ping | info | monitor | --scan | config get/set | config rewrite | shutdown`; `brew services start|stop|restart redis`; config file `/opt/homebrew/etc/redis.conf`.

**Wire protocol.** RESP: request `*argc\r\n$len\r\narg...`; reply first byte `+` simple, `-` error, `:` integer, `$` bulk (`$-1` = nil). This is what `RedisClient.java` speaks over a raw socket.

**Common uses.** Cache, session store, counters and rate limits, queues/streams, pub/sub, leaderboards (sorted sets), distributed locks.

---

## 2. Redis as a distributed lock

**Problem.** Several processes/servers must not run a critical section on the same resource at once. `synchronized` only works inside one JVM, so the lock state must live somewhere shared. Redis fits because it is shared and each command is atomic.

**Do you need a lock at all?** First look for one atomic command (`INCR` for ids, `DECR` for stock) or a database guarantee (transaction, `SELECT ... FOR UPDATE`, conditional `UPDATE ... WHERE version = ?`). Use a lock for multi-step work: leader election, single job runner, dedupe processing, cache stampede, calls to resources without transactions.

**Acquire.**
```
SET lock:<resource> <ownerToken> NX PX <ttl>
```
- `NX`: set only if absent, which is the mutual exclusion; check and set are one command.
- `PX`: expiry, so a crashed holder cannot block forever.
- Reply `OK` = got it; nil = held by someone else, so wait and retry.
- Two commands (`SETNX` then `EXPIRE`) are wrong: a crash in between leaves a lock with no expiry.

**Release.** Delete only if the value is still my token, atomically (Lua):
```lua
if redis.call("get", KEYS[1]) == ARGV[1] then return redis.call("del", KEYS[1]) else return 0 end
```
Blind `DEL` is wrong: if my lease expired and B took the lock, my `DEL` frees B's lock. `GET` then `DEL` as two commands has the same gap. Return value 0 tells the caller its lease was already lost.

**Key vs value.** Key = what is protected (smallest unit that must not be concurrent, `lock:order:123`); value = who owns it (random UUID).

**TTL sizing.** TTL is a crash-recovery timeout, not hold time (normal release is immediate). Size to the worst-case critical section plus RTT and GC pauses; too short loses exclusion, too long blocks everyone after a crash.

**Waiting.** Spin with sleep is simplest; better is `tryLock(maxWait)` with exponential backoff and jitter, so waiters do not hammer Redis or stampede in sync.

**Reentrancy.** Plain `SET NX` lock is not reentrant (same thread asking twice deadlocks itself). Reentrant versions keep an owner and a hold count (Redisson uses a hash).

**Failure modes (why it is never 100%).**
- Holder stalls longer than the TTL (GC pause, slow call): lease expires, second holder enters, two run at once. The owner token only stops the late one from releasing the other's lock.
- Failover: primary grants lock, dies before replicating, promoted replica grants it again.
- Network timeout on `SET` is ambiguous (applied or not); the TTL cleans up a stray lock.
- Clock/latency assumptions break under pauses and partitions.

**Mitigations.**
- Lease renewal (watchdog): extend TTL while holder is alive (Lua: if value == token then `PEXPIRE`). Does not help if the whole JVM is paused.
- Fencing token: a number that only increases per grant; the resource rejects writes carrying a lower number. Needs a resource that can compare (DB version column). Useless when the resource is itself a counter. Side effects (email, payment) need idempotency keys instead.
- Redlock: acquire on a majority of N (5) independent Redis nodes within a time budget, validity = TTL minus elapsed minus drift. Survives single-node failover but still rests on timing assumptions (Kleppmann's critique). Controversial; acceptable for efficiency, not for strict safety.
- Optimistic alternative: `WATCH key` ... `MULTI` ... `EXEC` aborts if the key changed; retry. No lock held.
- Stronger systems: ZooKeeper / etcd (consensus, sessions, ordered numbers) when a stale holder would corrupt data.

**Rule of thumb.** A Redis lock is an efficiency tool (avoid duplicate work). For correctness, enforce the invariant at the data layer (transaction, version check, idempotency) and treat the lock as an optimisation.

**Lock location.** Client-side locking is voluntary: every caller must lock; the resource does not enforce it. Keep the DB check as the real guard.

**Where the code stands (`src/redis`).**
| File | Role |
|---|---|
| `RedisClient` | shared RESP socket client; one connection, not thread safe |
| `idgenerator/RedisLock` | `lock()` returns token (`SET NX PX`, retry 5 ms); `unlock(token)` Lua check-and-delete, returns true/false |
| `idgenerator/IdGenerator` | intentionally racy shared counter |
| `idgenerator/Client` | runs a batch with a 10-thread pool, lock around `getID()` |
| `idgenerator/Main` | runs 3 clients in parallel, prints unique id count |
| `ratelimiter/RateLimiter` | interface: `allow()` |
| `ratelimiter/FixedWindowRateLimiter` | Redis counter: one Lua call does `INCR`, `EXPIRE` when created, compare with limit; pooled `RedisClient`s |
| `ratelimiter/SlidingWindowRateLimiter` | stub, to be designed |
| `ratelimiter/Server`, `RateMain` | demo API calling the limiter from many threads |

Measured: no lock gives duplicate ids (993/1000); with lock 1000/1000; TTL shorter than the critical section brings overlap back. Fixed-window limiter: 250 requests on 50 threads, limit 100 gives exactly 100 allowed.
