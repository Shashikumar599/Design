package redis.ratelimiter;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RateMain {
    public static void main(String[] args) throws InterruptedException {
        RateLimiter rateLimiter = new SlidingWindowRateLimiter("rate:sliding", 1, 5, 10);
        Server server = new Server(rateLimiter);
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        SecureRandom random = new SecureRandom();
        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<20;i++){
                    int sleep = random.nextInt(50);
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    executorService.submit(
                            new Runnable() {
                                @Override
                                public void run() {



                                    while(true){
                                        int x= server.api();
                                        if(x>0){
                                            break;
                                        }
                                        System.out.println("Request denied trying again after 50ms");
                                        try {
                                            Thread.sleep(50);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            }

                    );
                }
            }
        };


        Thread thread = new Thread(runnable1);
        Clock clock = Clock.systemDefaultZone();
        Instant startTime = clock.instant();
        thread.start();


        //join main , block further execution of main
        thread.join();



        executorService.shutdown();
        boolean finished = executorService.awaitTermination(50, TimeUnit.SECONDS);
        if(finished){
            System.out.println("Producer finished it's work, all task were submitted and complete");
        }
        else{
            System.out.println("Producer did not finished it's work, tasks are still running");
        }


        Instant endTime = clock.instant();

        Duration duration = Duration.between(startTime, endTime);
        System.out.println("Total time taken for producer " + duration.getSeconds());


    }
}
