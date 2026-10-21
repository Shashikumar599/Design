package cache.write;

import cache.Cache;
import cache.Database;

public abstract class WritePolicyBase<K,V> {
    protected Cache<K,V> cache;
    protected Database<K,V> database;
    WritePolicyBase(Cache<K,V> cache, Database<K,V> database){
        this.cache = cache;
        this.database = database;
    }
    protected abstract void fillCache(K key, V value);
    protected abstract void writeToDB(K key, V value);
    protected  abstract void writeToCache(K key, V value);
    protected  abstract void invalidateCache(K key);
}
