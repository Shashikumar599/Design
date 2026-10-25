package redis;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.*;

public class Main {
    public static void main(String []args) throws IOException, InterruptedException {
        try (RedisClient client = new RedisClient()) {
            client.put("stock", "100");
            System.out.println(client.get("stock"));
            System.out.println(client.get("missing"));
        }
        IdGenerator idGenerator = new IdGenerator();
        ConcurrentSkipListSet<Integer> set =  new ConcurrentSkipListSet<>();

        Client c1 = new Client(idGenerator, 300, set);
        Client c2 = new Client(idGenerator, 300, set);
        Client c3 = new Client(idGenerator, 400, set);

        ExecutorService executor = Executors.newScheduledThreadPool(10);

        executor.submit(c1);
        executor.submit(c2);
        executor.submit(c3);

        executor.shutdown();
        executor.awaitTermination(50000, TimeUnit.MILLISECONDS);




//        Thread.sleep(5000);

        System.out.println("Total ids issued " + set.size());

    }
}
