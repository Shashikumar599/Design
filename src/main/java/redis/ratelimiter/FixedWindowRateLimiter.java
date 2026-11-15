package redis.ratelimiter;

import redis.RedisClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class FixedWindowRateLimiter implements RateLimiter {
    // INCR creates the key at 1 if missing; only the call that created it attaches the TTL
    private static final String SCRIPT =
            "local c = redis.call('INCR', KEYS[1]) " +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "if c > tonumber(ARGV[2]) then return 0 else return 1 end";

    private final String key;
    private final int windowSeconds;
    private final int limit;
    private final BlockingQueue<RedisClient> pool;

    public FixedWindowRateLimiter(String key, int windowSeconds, int limit, int poolSize) {
        this.key = key;
        this.windowSeconds = windowSeconds;
        this.limit = limit;
        this.pool = new ArrayBlockingQueue<>(poolSize);
        try {
            for (int i = 0; i < poolSize; i++) {
                pool.add(new RedisClient());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean allow() {
        return checkLimit();
    }

    private boolean checkLimit() {
        RedisClient client;
        try {
            client = pool.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            Object reply = client.command("EVAL", SCRIPT, "1", key,
                    String.valueOf(windowSeconds), String.valueOf(limit));
            return Long.valueOf(1).equals(reply);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            pool.add(client);
        }
    }
}
