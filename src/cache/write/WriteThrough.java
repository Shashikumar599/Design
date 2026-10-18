package cache.write;

import cache.Cache;
import cache.Database;

public class WriteThrough<K,V> extends WritePolicyBase<K,V> implements WritePolicy<K,V>{

    public WriteThrough(Cache<K,V> cache, Database<K,V> database){
        super(cache,database);
    }

    @Override
    public void write(K key, V value) {
        writeToCache(key, value);
        try{
            writeToDB(key, value);
        }
        catch (Exception e){
            invalidateCache(key);
            throw new RuntimeException("Database Write Failed");
        }

    }

    @Override
    public void writeToDB(K key, V value) {
        database.put(key,value);

    }

    @Override
    public void writeToCache(K key, V value) {
        cache.put(key, value);

    }

    @Override
    public void invalidateCache(K key) {
        cache.invalidateKey(key);

    }

    @Override
    public void shutdown() {
        System.out.println("Cache , DB system shutdown");
    }

    @Override
    public V bufferRead(K key) {
        return null;
    }

    @Override
    public void fillCache(K key, V value) {

    }
}
