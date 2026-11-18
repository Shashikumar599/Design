package concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Processor {
    private volatile boolean isReadAllowed;
    private AtomicInteger readers;
    private AtomicInteger writers;
    private ReentrantReadWriteLock lock;
    private ReentrantLock reentrantLock;

    Processor(){
        isReadAllowed = true;
        readers = new AtomicInteger(0);
        writers = new AtomicInteger(0);
        reentrantLock = new ReentrantLock();
//        lock = new ReentrantReadWriteLock(true);
    }



    public void reg_cb() throws InterruptedException {


       synchronized (this){
        while(!isReadAllowed){
            wait(2000);
        }
//        lock.readLock().lock();

        readers.incrementAndGet();
       }

        try{
            System.out.println(Thread.currentThread().getName() + " : work started");
            Thread.sleep(1000);
            System.out.println(Thread.currentThread().getName() + " : work completed");
        }
        finally {
            synchronized (this){
                readers.decrementAndGet();
                if(readers.get()==0){
                    notifyAll();
                }
            }
        }
    }
    public  void  ProcessEvent() throws InterruptedException {
//        lockRead();

            writers.incrementAndGet();
            isReadAllowed = false;
            synchronized (this) {
                isReadAllowed = false;
                while (readers.get() > 0) {
                    wait(2000);
                }

            }

        reentrantLock.lock();
        try{
        System.out.println(Thread.currentThread().getName()+" : Event Started");
        Thread.sleep(2000);
        System.out.println(Thread.currentThread().getName()+" : Event Completed");}
        finally {
//            unlockRead();
//            lock.writeLock().unlock();
            reentrantLock.unlock();


            synchronized (this) {
                writers.decrementAndGet();

                if (writers.get() == 0) {
                    isReadAllowed = true;
                    notifyAll();
                }
            }

        }
    }
//    private void lockRead(){
//        isReadAllowed = false;
//    }
//    private void unlockRead(){
//        isReadAllowed = true;
//        notifyAll();
//    }
}
