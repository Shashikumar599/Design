package cache.write;

import cache.Cache;
import cache.Database;
import cache.exception.FlushTimeoutException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WriteBehind<K,V> extends WritePolicyBase<K,V> implements WritePolicy<K,V>{
    private final Map<K,V> map;
    private final ScheduledExecutorService executorService;

    private final AtomicBoolean shutdown;
    private final ReentrantReadWriteLock shutdownLock;

    public WriteBehind(Cache<K,V> cache, Database<K,V> database){
        super(cache,database);
        shutdown = new AtomicBoolean(false);
        shutdownLock = new ReentrantReadWriteLock();

        map = new ConcurrentHashMap<>();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                flush();
            }
        },2000, 2000,TimeUnit.MILLISECONDS);
    }

    @Override
    public void write(K key, V value) {
        if(shutdown.get()){
            throw new IllegalStateException("App is shutting down, Writes are blocked");
        }
        shutdownLock.readLock().lock();
        try{
            if(shutdown.get()){
                throw new IllegalStateException("App is shutting down, Writes are blocked");
            }

            // cache + buffer update as one atomic step per key, so concurrent writers cannot leave them different.
            // Lock order is always buffer key lock -> LRUStore lock, never reversed, so no deadlock. See DECISIONS.md D4.
            map.compute(key, (k, old) -> {
                writeToCache(key,value);
                return value;});
        }
        finally {
            shutdownLock.readLock().unlock();
        }
    }

    @Override
    public V read(K key) {
        V value = bufferRead(key);
        if(value==null){
            value = database.get(key);
            if(value!=null){
                fillCache(key,value);
            }
        }
        return value;
    }

    private void flush(){
        if(map.isEmpty()){
            return;
        }
        for(K key : map.keySet()){
            try{
                V value = map.get(key);
                database.put(key, value);
                map.remove(key,value);

            }
            catch (Exception e){
                System.out.println("Exception occurred failed to write " + e.getMessage());
            }
        }
    }

    @Override
    protected void writeToDB(K key, V value) {

    }

    @Override
    protected void writeToCache(K key, V value) {
        cache.put(key,value);

    }

    @Override
    protected void invalidateCache(K key) {

    }

    @Override
    public void shutdown() throws InterruptedException {
        if(!shutdown.compareAndSet(false,true)){
            System.out.println("Shutdown already in progress");
            return;
        }
        System.out.println("Starting shutdown");
        shutdownLock.writeLock().lock();

        try{
            executorService.shutdown();
            boolean  awaitTermination = executorService.awaitTermination(5,TimeUnit.SECONDS);
            if(!awaitTermination){
                executorService.shutdownNow();
                // cannot try manual flush if existing flush didn't fail in intermittent state
                throw new FlushTimeoutException("Unable to finish Flush tasks , some updates may not be applied to db");
            }
            System.out.println("Calling Flush to DB, final cleanup");
            //this is also best effort as it swallows exception
            flush();
        }
        finally {

            shutdownLock.writeLock().unlock();
            System.out.println("Current items in cache , not written to db , will not be attempted " + map.size());
            System.out.println("Cache , DB system shutdown completed");
        }

    }


    private V bufferRead(K key) {
        return map.get(key);
    }

    @Override
    protected void fillCache(K key, V value) {
        map.computeIfAbsent(key ,(K  k) -> {
            cache.fillCache(key,value);
            return null;
        });

    }
}
