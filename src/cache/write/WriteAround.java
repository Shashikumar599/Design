package cache.write;

import cache.Cache;
import cache.Database;

public class WriteAround<K,V> extends WritePolicyBase<K,V> implements WritePolicy<K,V> {
//    private final Map<K,V> map;

    public WriteAround(Cache<K,V> cache, Database<K,V> database){
        super(cache,database);
//        map = new ConcurrentHashMap<>();
    }

    @Override
    public void write(K key, V value) {
        writeToDB(key,value);
        invalidateCache(key);
    }

    @Override
    public V read(K key) {

        V value = database.get(key);
        //race to be fixed here
        fillCache(key, value);
        return value;
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
        cache.fillCache(key, value);
    }
}
