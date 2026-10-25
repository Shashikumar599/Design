package redis.idgenerator;

import redis.RedisClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Redis lock: SET key token NX PX ttl to acquire, token-checked delete (Lua) to release.
 * Safe to share between threads: each thread uses its own connection.
 * The caller owns the token: lock() returns it, unlock(token) must be given it back.
 * Not reentrant.
 */
public class RedisLock {
    private static final long RETRY_DELAY_MS = 5;

    private final String key;
    private final long ttlMillis;
    private final ThreadLocal<RedisClient> client = ThreadLocal.withInitial(() -> {
        try {
            return new RedisClient();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    });

    // Delete the key only if it still holds my token; check + delete run as one atomic unit in Redis
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) else return 0 end";

    public RedisLock(String key, long ttlMillis) {
        this.key = key;
        this.ttlMillis = ttlMillis;
    }

    /** @return the owner token; pass it to unlock() */
    public String lock() throws InterruptedException {
        String myToken = UUID.randomUUID().toString();
        try {
            // "OK" if we got the lock, null (nil) if someone else holds it
            while (client.get().command("SET", key, myToken, "NX", "PX", String.valueOf(ttlMillis)) == null) {
                Thread.sleep(RETRY_DELAY_MS);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return myToken;
    }

    /** @return true if the lock was released, false if it was no longer mine (expired or taken over) */
    public boolean unlock(String token) {
        try {
            Object deleted = client.get().command("EVAL", UNLOCK_SCRIPT, "1", key, token);
            return Long.valueOf(1).equals(deleted);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
