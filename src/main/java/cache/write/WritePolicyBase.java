package cache.write;

import cache.Cache;
import cache.Database;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WritePolicyBase<K,V> {
    protected Cache<K,V> cache;
    protected Database<K,V> database;
    private final AtomicBoolean shutdown;

    WritePolicyBase(Cache<K,V> cache, Database<K,V> database){
        this.cache = cache;
        this.database = database;
        shutdown = new AtomicBoolean();
    }
    protected abstract void fillCache(K key, V value);
    protected abstract void writeToDB(K key, V value);
    protected  abstract void writeToCache(K key, V value);
    protected  abstract void invalidateCache(K key);
    public void shutdown() throws InterruptedException{
        if(!markShutdown()){
            System.out.println("Shutdown already in progress");
            return;
        }
        System.out.println("Shutdown in progress");
    }
    protected boolean markShutdown(){
        return shutdown.compareAndSet(false,true);
    }
    protected boolean isShutdown(){
        return shutdown.get();
    }
    protected void ensureOpen(){
        if(isShutdown()){
            throw new IllegalStateException("Policy is shut down");
        }
    }
}
