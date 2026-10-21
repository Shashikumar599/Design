package cache.write;

import cache.Cache;
import cache.Database;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WriteAround<K,V> extends WritePolicyBase<K,V> implements WritePolicy<K,V> {
    private final Map<K,Object> tickets;

    public WriteAround(Cache<K,V> cache, Database<K,V> database){
        super(cache,database);
        tickets = new ConcurrentHashMap<>();
    }

    @Override
    public void write(K key, V value) {
        writeToDB(key, value);
        tickets.remove(key);
        invalidateCache(key);
    }

    @Override
    public V read(K key) {
        Object ticket = new Object();
        tickets.put(key, ticket);
        try {


            V value = database.get(key);
            if (value != null) {
                tickets.computeIfPresent(key, (K k, Object old) ->
                        {
                            if (ticket == old) {
                                fillCache(key, value);
                                return null;
                            }
                            return old;
                        }
                );
            }
            return value;
        }
        finally {
            tickets.remove(key, ticket);
        }
    }

    @Override
    protected void writeToDB(K key, V value) {
        database.put(key,value);
    }

    @Override
    protected void writeToCache(K key, V value) {
        cache.put(key,value);
    }

    @Override
    protected void invalidateCache(K key) {
        cache.invalidateKey(key);
    }

    @Override
    public void shutdown() {
        System.out.println("Cache , DB system shutdown");
    }


    @Override
    protected void fillCache(K key, V value) {
//        map.put(key, value);
        cache.fillCache(key,value);
    }
}
