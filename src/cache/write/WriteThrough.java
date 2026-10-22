package cache.write;

import cache.Cache;
import cache.Database;

public class WriteThrough<K,V> extends WritePolicyBase<K,V> implements WritePolicy<K,V>{

    private final Object[] lockList;
    private final int lockCount;

    public WriteThrough(Cache<K,V> cache, Database<K,V> database){
        super(cache,database);
        lockCount = 16;
        lockList = new Object [lockCount];

        for(int i=0;i<lockCount;i++){
            lockList[i] = new Object();
        }
    }

    @Override
    public void  write(K key, V value) {
        ensureOpen();
        int lockIndex = Math.floorMod(key.hashCode(), lockCount);
        synchronized (lockList[lockIndex]){
            writeToCache(key, value);
            try{
                writeToDB(key, value);
            }
            catch (Exception e){
                invalidateCache(key);
                throw new RuntimeException("Database Write Failed " + e.getMessage(), e);
            }
        }
    }

    @Override
    public V read(K key) {
        ensureOpen();
        V value = database.get(key);
        if(value!=null){
            fillCache(key,value);
        }
        return value;
    }

    @Override
    protected void writeToDB(K key, V value) {
        database.put(key,value);

    }

    @Override
    protected void writeToCache(K key, V value) {
        cache.put(key, value);

    }

    @Override
    protected void invalidateCache(K key) {
        cache.invalidateKey(key);

    }

    @Override
    protected void fillCache(K key, V value) {
        cache.fillCache(key,value);
    }
}
