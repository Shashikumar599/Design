package cache.store;

import cache.DB.DB;

import java.util.HashMap;
import java.util.Map;

public class LRUStore<K, V> implements Store<K, V>{
    DB<K,V> cacheDb;


    class Node{
        K key;
        V value;
        Node prev;
        Node next;

        Node(K key, V value){
            this.key = key;
            this.value = value;
        }
    }
    private int capacity;
    private Map<K, Node> map;
    private Node head,tail;

    public LRUStore(int capacity){
        //input validation
        this.capacity = capacity;
        map = new HashMap<>();
        head = new Node(null,null);
        tail = new Node(null,null);

        head.next=tail;
        tail.prev=head;

//        cacheDb = new CacheDB<>();

    }

    @Override
    public V get(K key) {
        Node node = map.get(key);
        if(node!=null){
            moveHead(node);
            return node.value;
        }
        //To be better handled
        return null;
    }

    @Override
    public void put(K key, V value) {
        Node node = map.get(key);
        if(node!=null){
            node.value = value;
            moveHead(node);
        }
        else{
            node = new Node(key, value);
            int size = map.size();
            if(size<capacity){
                addtoHead(node);
                map.put(key,node);
            }
            else{
                removeTail();
                addtoHead(node);
                map.put(key,node);
            }
        }

    }

    @Override
    public void invalidateKey(K key) {
        if(map.containsKey(key)){
            removeNode(map.get(key));
            map.remove(key);
        }
    }

    private void removeTail(){
        Node prev = tail.prev.prev;

        map.remove(tail.prev.key);
        prev.next= tail;
        tail.prev = prev;
    }
    private void addtoHead(Node node){
        Node next = head.next;
        map.put(node.key, node);


        next.prev=node;
        node.next=next;
        node.prev=head;
        head.next=node;
    }

    private void moveHead(Node node){
        removeNode(node);
        addtoHead(node);
    }
    private void removeNode(Node node){
        map.remove(node.key);

        Node prev = node.prev;
        Node next = node.next;

        prev.next = node.next;
        next.prev = node.prev;
    }
}
