package cache;

import cache.store.LRUStore;
import cache.write.WriteAround;
import cache.write.WriteBehind;

public class Main {

    // Used by the WriteAround race demo: reads the value, then sleeps, like a DB reply that arrives late.
    static class SlowDatabase<K,V> extends Database<K,V> {
        volatile boolean slow = false;

        @Override
        public V get(K key){
            V value = super.get(key);
            if(slow){
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return value;
        }
    }

    public static void main(String []args) throws InterruptedException {
        // Uncomment one demo block at a time.

        // ---------- Cache only (LRU) ----------
        // K and V are written once, on Cache. The factory's types are inferred from them.
        // Switch policy by changing Policy.LRU to Policy.LFU; nothing else changes.
//        Cache<Integer, String> cache = new Cache<>(3, LRUStore::new);
//
//        System.out.println(cache.get(1));   // null: miss on an empty cache
//
//        cache.put(1, "a");
//        cache.put(2, "b");
//        cache.put(3, "c");
//        cache.put(4, "d");
//        cache.put(5, "e");
//        cache.put(6, "f");                  // capacity is 3, so keys 1-3 are evicted
//
//        System.out.println(cache.get(1));   // null: evicted
//        System.out.println(cache.get(6));   // f
//        System.out.println(cache.get(4));   // d (hit, and now the most recently used)
//
//        // cache.put("x", "y");             // COMPILE error: key must be Integer

        // ---------- WriteBehind ----------
//        App<Integer, String> app = new App<>(3, LRUStore::new, WriteBehind::new, new Database<>());
//
//        app.put(1, "abc");
//        app.put(2, "bcd");
//        app.put(3, "cde");
//        app.put(4, "def");
//        app.put(5, "efg");
//        app.put(6, "fgh");                  // capacity 3: keys 1-3 evicted from the cache, still in the buffer
//
//        System.out.println("1 (from buffer): " + app.get(1));     // abc, not yet flushed to the DB
//        System.out.println("6 (cache hit): " + app.get(6));       // fgh
//
//        Thread.sleep(2500);                 // wait past the 2 s flush: buffer is now empty, DB has everything
//        System.out.println("2 (from DB, then cached): " + app.get(2));   // bcd
//        System.out.println("2 (cache hit): " + app.get(2));              // bcd
//
//        app.put(7, "ghi");
//        app.shutdown();                     // final flush: 7 reaches the DB
//        try {
//            app.put(8, "hij");
//        } catch (IllegalStateException e) {
//            System.out.println("put after shutdown rejected: " + e.getMessage());
//        }

        // ---------- WriteAround: basic ----------
//        App<Integer, String> app = new App<>(3, LRUStore::new, WriteAround::new, new Database<>());
//
//        app.put(1, "a");
//        app.put(2, "b");
//        System.out.println("1 (miss, from DB): " + app.get(1));   // a
//        System.out.println("1 (hit): " + app.get(1));             // a
//        app.put(1, "a2");                   // DB write, tear up tickets, invalidate
//        System.out.println("1 after update: " + app.get(1));      // a2, not the old cached a
//        System.out.println("99 (missing): " + app.get(99));       // null, and nothing cached
//        app.shutdown();

        // ---------- WriteAround: stale fill race (D5) ----------
        // A reader reads "old" from a slow DB; a write of "new" lands before the reader fills. The ticket stops the stale fill.
        // To see the bug: comment out tickets.remove(key) in WriteAround.write and run again ("cache after reader" shows old).
//        SlowDatabase<Integer, String> slowDb = new SlowDatabase<>();
//        App<Integer, String> app = new App<>(3, LRUStore::new, WriteAround::new, slowDb);
//
//        app.put(1, "old");
//        slowDb.slow = true;
//        Thread reader = new Thread(() -> System.out.println("reader got: " + app.get(1)));   // old: read before the write
//        reader.start();
//        Thread.sleep(100);                  // the reader is now waiting for its slow DB reply
//        slowDb.slow = false;
//        app.put(1, "new");
//        reader.join();
//        System.out.println("cache after reader: " + app.cache.get(1));   // null: stale fill skipped
//        System.out.println("next get: " + app.get(1));                   // new
//        app.shutdown();
    }
}
