# Design

A learning repo for low-level design and distributed-systems practice in plain Java. Each folder under `src/` is a self-contained exercise with its own `Main`. There is no build tool; open the project in IntelliJ, or compile a package with `javac`.

## Projects

| Package | What it is | Status |
|---|---|---|
| [`cache`](src/cache) | Cache with pluggable eviction (`LRUStore`; `LFUStore` is a stub) and write policies (`WriteThrough`, `WriteAround`, `WriteBehind`) over a simulated DB. Notes: `DESIGN.md` (revision sheet), `DECISIONS.md` (D1-D7). | Working |
| [`redis`](src/redis) | Hands-on distributed locking with Redis: a socket-based `RedisClient` (RESP), a `RedisLock` (`SET NX PX` acquire, Lua token-checked release), and a unique-id race demo. | In progress |
| [`notification`](src/notification) | Notification service with channels (email, SMS, mobile), single/mass dispatchers, and factories. | Working |
| [`strategy`](src/strategy) | Strategy pattern: vehicles with swappable drive behaviour. Design Q&A in `discuss.md`. | Working |
| [`logger`](src/logger) | Logger singleton. | Stub |
| [`urlShorterner`](src/urlShorterner) | URL shortener. | Not started |

## Running

Each package has its own `Main` (for example `cache.Main`, `notification.Main`, `strategy.Main`).

```sh
# from the repo root
javac -d out $(find src/cache -name '*.java')
java -cp out cache.Main
```

In IntelliJ, open the folder and run the `Main` class of the package you want. `out/` and `.idea/` are git-ignored.

### Redis exercise prerequisites

```sh
brew install redis
brew services start redis      # or: redis-server
redis-cli ping                 # expect PONG
javac -d out src/redis/*.java && java -cp out redis.Main
```

The client connects to `127.0.0.1:6379`.

## Conventions

- Design notes live next to the code (`DESIGN.md`, `DECISIONS.md`, `discuss.md`).
