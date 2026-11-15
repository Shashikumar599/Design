package redis.idgenerator;

public class IdGenerator {
    private int  counter=0;

    public int getID() throws InterruptedException {

            int curr = counter;
            Thread.sleep(10);
            counter= curr +1;
            return counter;

    }
}
