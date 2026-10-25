package redis.ratelimiter;

import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RateMain {
    public static void main(String[] args) throws InterruptedException {
        Server server = new Server();
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        SecureRandom random = new SecureRandom();
        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<100;i++){
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
                                    server.api();
                                }
                            }

                    );
                }
            }
        };


        Thread thread = new Thread(runnable1);
        thread.start();

        Thread.sleep(5000);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);


    }
}
