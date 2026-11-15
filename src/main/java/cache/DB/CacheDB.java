package cache.DB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheDB<K,V> implements DB<K,V> {
    Map<K,V> db;
    public CacheDB(){
        db = new ConcurrentHashMap<>();
    }

    // what if i have to make these synchronized
    @Override
    public void  insert(K key, V value) {
        db.put(key, value);
    }

    @Override
    public void delete(K key) {

    }

    @Override
    public V fetch(K key) {
        return db.get(key);
    }
}
