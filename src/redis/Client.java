package redis;

import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.*;

public class Client implements Runnable {
    private IdGenerator idGenerator;
    ConcurrentSkipListSet<Integer> set;
    private int count;
    public Client(IdGenerator idGenerator, int x, ConcurrentSkipListSet<Integer> set) throws InterruptedException {
        this.idGenerator = idGenerator;
        this.set = set;
        count = x;
    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newScheduledThreadPool(10);


        RedisLock lock = new RedisLock("lock:Id", 5000);
        for(int i=0;i<count;i++){
            executor.execute(new Runnable() {
                @Override
                public void run() {


                    int id = 0;
                    String token;
                    try {
                         token = lock.lock();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {

                        id = idGenerator.getID();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    finally {
                        boolean unlockStatus = lock.unlock(token);
                        if(unlockStatus){
                            System.out.println("Failed to unlock, Anomaly detected");
                        }

                    }
                    System.out.println(Thread.currentThread().getName() + " my id is "   + id);
                    set.add(id);
                }
            });

        }
        executor.shutdown();
        try {
            executor.awaitTermination(500000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
