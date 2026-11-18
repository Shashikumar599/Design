package concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String []args) throws InterruptedException {
        System.out.println("Hello");
        Processor processor = new Processor();

        processor.reg_cb();

        ExecutorService service = Executors.newFixedThreadPool(10);

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    processor.reg_cb();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    processor.ProcessEvent();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        for(int i=0;i<5;i++){
            service.submit(t1);
        }
        service.submit(t2);


        service.shutdown();

    }
}
