package cache.write;

public interface WritePolicy<K,V> {
    public  void write(K key, V value);
    public  void writeToDB(K key, V value);
    public  void writeToCache(K key, V value);
    public  void invalidateCache(K key);
    public void shutdown() throws InterruptedException;

}
