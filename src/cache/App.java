package cache;

import cache.store.Store;
import cache.write.WritePolicy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

public class App<K,V> {
    Database<K,V> database;
    Cache<K,V> cache;
    WritePolicy<K,V> writePolicy;

    private AtomicInteger cacheHit;
    private AtomicInteger cacheMiss;

    App(int cacheCapacity, Function<Integer,Store<K,V>> storeFactory
        , BiFunction<Cache<K,V>, Database<K,V>, WritePolicy<K,V>> writePolicyFactory, Database<K,V> database){
        // db to be independent of app, persistance
        this.database = database;

        //Cache to be created on db startup
        // I am using top down approach will slowly build on this , How to pass store type here eviction policy lfu,lru ??
        this.cache = new Cache<>(cacheCapacity, storeFactory);
        this.writePolicy = writePolicyFactory.apply(cache,database);
        cacheHit = new AtomicInteger(0);
        cacheMiss = new AtomicInteger(0);
    }
    public V get(K key){
        if(key== null){
            throw new IllegalArgumentException("key must not be null");
        }
        V value = cache.get(key);
        if(value==null){
            cacheMiss.incrementAndGet();
            value = writePolicy.read(key);
            return value;
        }
        cacheHit.incrementAndGet();
        return value;
    }

    public void put(K key, V value){
        if(key== null){
            throw new IllegalArgumentException("key must not be null");
        }
        if(value == null){
            throw new IllegalArgumentException("value must not be null");
        }
        writePolicy.write(key,value);
    }
    public void storeToCache(K key, V value){
        cache.put(key, value);
    }
    public void storeToDB(K key, V value){
        database.put(key, value);
    }

    public void shutdown() throws InterruptedException {
        writePolicy.shutdown();
    }
}
