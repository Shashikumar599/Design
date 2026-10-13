package cache.store;

public interface Store<K,V>{
    public V get(K key);
    public void put(K key, V value);
    public void invalidateKey(K key);

}
