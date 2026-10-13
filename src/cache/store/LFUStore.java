package cache.store;


import java.util.HashMap;
import java.util.Map;

public class LFUStore<K, V> implements Store<K, V>{
    int capacity;
    Map<K,V> keyMap;

    public LFUStore(int capacity){
        this.capacity = capacity;
        keyMap = new HashMap<>();
    }
    @Override
    public V get(K key) {
        return null;
    }

    @Override
    public void put(K key, V value) {


    }

    @Override
    public void invalidateKey(K key) {

    }
}
