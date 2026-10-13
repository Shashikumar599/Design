package cache;

import cache.DB.ActualDB;
import cache.DB.DB;

public class Database<K,V> {
    DB<K,V> db;
    Database(){
        db = new ActualDB<>();
    }
    public V get(K key){
        return db.fetch(key);
    }

    public void put(K key, V value){
        db.insert(key,value);
    }
}
