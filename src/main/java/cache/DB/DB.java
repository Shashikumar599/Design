package cache.DB;

public interface DB <K, V>{

    public void insert(K key, V value);
    public void delete(K key);
    public V fetch(K key);
}
