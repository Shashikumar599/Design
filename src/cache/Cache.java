package cache;

import cache.store.Store;

import java.util.function.Function;

public class Cache<K,V> {

    // Interface type with <K,V> (never LRUStore / LFUStore), so Cache does not know the policy
    // and the compiler keeps checking key/value types. final: built once, owned by this Cache.
    private final Store<K,V> cacheStore;

    // The client passes a StoreFactory<K,V>. It is the ONLY time the factory is used:
    // create(capacity) builds the store, and from then on Cache just forwards get/put to it.
    // The client picks the policy by picking the factory (e.g. new DefaultStoreFactory<>(Policy.LRU)).
    public Cache(int capacity, Function<Integer, Store<K,V>> storeFactory){
        this.cacheStore = storeFactory.apply(capacity);
    }

    public V get(K key){
        return cacheStore.get(key);
    }

    public void put(K key, V value){
        cacheStore.put(key,value);
    }

    public void invalidateKey(K key){
        System.out.println("Invalidating key "+ key);
        cacheStore.invalidateKey(key);
    }

}
