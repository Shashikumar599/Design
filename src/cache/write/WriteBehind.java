package cache.write;

import cache.Cache;
import cache.Database;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WriteBehind<K,V> extends WritePolicyBase<K,V> implements WritePolicy<K,V>{
    private final Map<K,V> map;

    public WriteBehind(Cache<K,V> cache, Database<K,V> database){
        super(cache,database);
        map = new ConcurrentHashMap<>();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                flush();
            }
        },2000, 2000,TimeUnit.MILLISECONDS);
    }

    @Override
    public void write(K key, V value) {
        writeToCache(key, value);
        map.put(key,value);
        //bg handler flushing to db , we can create an executor and have intialize it in init method ,
        // that thready can continuously write to db , we can maintain a temp map and that map can be loaded to db



    }
    private void flush(){
        System.out.println("Flushing to db");
        if(map==null || map.isEmpty()){
            System.out.println("Map is empty");
            return;
        }
        for(K key : map.keySet()){
            try{
                //Race can happen between below two lines
                V value = map.get(key);
                database.put(key, value);
                map.remove(key,value);

            }
            catch (Exception e){
                System.out.println("Exception occured failed to write " + e.getMessage());
            }
        }
    }

    @Override
    public void writeToDB(K key, V value) {

    }

    @Override
    public void writeToCache(K key, V value) {
        cache.put(key,value);

    }

    @Override
    public void invalidateCache(K key) {

    }
}
