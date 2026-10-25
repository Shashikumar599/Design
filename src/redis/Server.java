package redis;

import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private final RateLimiter rateLimiter;

    private final AtomicInteger counter;

    public Server(){
        rateLimiter = new RateLimiter("rate",1,5,10);
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
