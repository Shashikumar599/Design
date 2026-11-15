package cache.DB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActualDB<K,V> implements DB<K,V>{
    Map<K,V> db;
    public ActualDB(){
        db = new ConcurrentHashMap<>();
    }
    @Override
    public void insert(K key, V value) {
        db.put(key,value);

    }

    @Override
    public void delete(K key) {

    }

    @Override
    public V fetch(K key) {
        return db.get(key);
    }
}
