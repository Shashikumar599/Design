package cache;

import cache.store.LRUStore;
import cache.write.WriteBehind;

public class Main {
    public static void main(String []args){
        // K and V are written once, on Cache. The factory's types are inferred from them.
        // Switch policy by changing Policy.LRU to Policy.LFU; nothing else changes.
        Cache<Integer, String> cache = new Cache<>(3, LRUStore::new);

//        System.out.println(cache.get(1));   // null: miss on an empty cache
//
//        cache.put(1, "a");
//        cache.put(2, "b");
//        cache.put(3, "c");
//        cache.put(4, "d");
//        cache.put(5, "e");
//        cache.put(6, "f");                  // capacity is 5, so key 1 (least recently used) is evicted
//
//        System.out.println(cache.get(1));   // null: evicted
//        System.out.println(cache.get(6));   // f
//        System.out.println(cache.get(2));   // b (hit, and now the most recently used)
//
//        // cache.put("x", "y");             // COMPILE error: key must be Integer
        Database<Integer, String> database = new Database<>();
        App<Integer, String> app = new App<>(3,LRUStore::new, WriteBehind::new,database );

        app.put(1,"abc");
        app.put(2,"bcd");
        app.put(2,"bcd");
        app.put(2,"bcd");
        app.put(3,"cde");
        app.put(4,"def");
        app.put(5,"efg");
        app.put(6,"fgh");

        System.out.println("Value of 1 "+ app.get(1));
        System.out.println("Value of 2 "+ app.get(2));
        System.out.println("Value of 3 "+ app.get(3));
        System.out.println("Value of 4 "+ app.get(4));
        System.out.println("Value of 5 "+ app.get(5));
        System.out.println("Value of 6 "+ app.get(6));

        try{
            app.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
