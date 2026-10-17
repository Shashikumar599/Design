package cache.write;

import cache.Cache;
import cache.Database;

public class WriteAround<K,V> extends WritePolicyBase<K,V> implements WritePolicy<K,V> {

    public WriteAround(Cache<K,V> cache, Database<K,V> database){
        super(cache,database);
    }

    @Override
    public void write(K key, V value) {
        invalidateCache(key);
        writeToDB(key,value);
    }

    @Override
    public void writeToDB(K key, V value) {
        database.put(key,value);
    }

    @Override
    public void writeToCache(K key, V value) {
        cache.put(key,value);
    }

    @Override
    public void invalidateCache(K key) {
        cache.invalidateKey(key);
    }

    @Override
    public void shutdown() {
        System.out.println("Cache , DB system shutdown");
    }
}
