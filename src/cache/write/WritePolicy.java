package cache.write;

public interface WritePolicy<K,V> {
    public  void write(K key, V value);
    public V read(K key);
    public void shutdown() throws InterruptedException;


}
