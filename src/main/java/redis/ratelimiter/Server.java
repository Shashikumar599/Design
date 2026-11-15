package redis.ratelimiter;

import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private final RateLimiter rateLimiter;

    private final AtomicInteger counter;

    public Server(RateLimiter rateLimiter){
        this.rateLimiter = rateLimiter;
        counter = new AtomicInteger(0);
    }
    public int api(){
        if(!rateLimiter.allow()){
            System.out.println("Request denied 429");
            return -1;
        }
        int x = counter.incrementAndGet();
        System.out.println("Resource accessed " + x);
        return x;
    }
}
