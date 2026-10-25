package redis.ratelimiter;

import redis.RedisClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SlidingWindowRateLimiter implements RateLimiter {
    // ZSET per key: member = one request (unique), score = its time in ms (from the Redis clock).
    // Room left: add. Full: drop expired entries first, then add if there is room now.
    private static final String SCRIPT =
            "local t = redis.call('TIME') " +
            "local now = t[1] * 1000 + math.floor(t[2] / 1000) " +
            "if redis.call('ZCARD', KEYS[1]) >= tonumber(ARGV[2]) then " +
            "  redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - tonumber(ARGV[1]) * 1000) " +
            "end " +
            "if redis.call('ZCARD', KEYS[1]) < tonumber(ARGV[2]) then " +
            "  redis.call('ZADD', KEYS[1], now, ARGV[3]) " +
            "  return 1 " +
            "end " +
            "return 0";

    private final String key;
    private final int windowSeconds;
    private final int limit;
    private final BlockingQueue<RedisClient> pool;

    public SlidingWindowRateLimiter(String key, int windowSeconds, int limit, int poolSize) {
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
            // unique member per request, so two requests in the same millisecond both count
            Object reply = client.command("EVAL", SCRIPT, "1", key,
                    String.valueOf(windowSeconds), String.valueOf(limit), UUID.randomUUID().toString());
            return Long.valueOf(1).equals(reply);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            pool.add(client);
        }
    }
}
