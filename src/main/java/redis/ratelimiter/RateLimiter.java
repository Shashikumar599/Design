package redis.ratelimiter;

public interface RateLimiter {
    boolean allow();
}
