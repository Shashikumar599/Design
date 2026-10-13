package cache.write;

import cache.Cache;
import cache.Database;

public class WritePolicyBase<K,V> {
    protected Cache<K,V> cache;
    protected Database<K,V> database;
    WritePolicyBase(Cache<K,V> cache, Database<K,V> database){
        this.cache = cache;
        this.database = database;
    }
}
