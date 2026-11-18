package redis.idgenerator;

import java.util.concurrent.atomic.AtomicInteger;

public class IdGenerator {
    private AtomicInteger counter;
    IdGenerator(){
        counter = new AtomicInteger(0);
    }

    public int getID() throws InterruptedException {

//            int curr = counter;
//            Thread.sleep(10);
//            counter= curr +1;
            return counter.incrementAndGet();

    }
}
