package cache.store;

import java.util.HashMap;
import java.util.Map;

public class LRUStore<K, V> implements Store<K, V>{

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
    private final int capacity;
    private final Map<K, Node> map;
    private final Node head;
    private final Node tail;
    // Guards map and the linked list (head..tail). Every public method holds it for its WHOLE body, because get() also
    // moves a node and put() does check-then-act (size<capacity, then evict + insert).
    // A private lock object (not `synchronized` methods on `this`) so no outside code can take the same lock.
    // `this` would also be acceptable while only Cache holds the store; switch to / keep this if the store gets exposed.
    private final Object lock;

    public LRUStore(int capacity){
        //input validation
        if(capacity<=0){
            throw new IllegalArgumentException("Capacity should be positive");
        }
        this.capacity = capacity;
        lock = new Object();
        map = new HashMap<>();
        head = new Node(null,null);
        tail = new Node(null,null);

        head.next=tail;
        tail.prev=head;

    }

    @Override
    public V get(K key) {
        synchronized (lock){
        Node node = map.get(key);
        if(node!=null){
            moveHead(node);
            return node.value;
        }
        //To be better handled
        return null;
        }
    }

    @Override
    public void put(K key, V value) {
        synchronized (lock) {
            Node node = map.get(key);
            if (node != null) {
                node.value = value;
                moveHead(node);
            } else {
                node = new Node(key, value);
                int size = map.size();
                if (size < capacity) {
                    addtoHead(node);
                    map.put(key, node);
                } else {
                    removeTail();
                    addtoHead(node);
                    map.put(key, node);
                }
            }
        }

    }

    @Override
    public void invalidateKey(K key) {
        synchronized (lock) {
            if (map.containsKey(key)) {
                removeNode(map.get(key));
                map.remove(key);
            }
        }
    }

    @Override
    public void fillCache(K key, V value) {
            synchronized (lock){
                if(!map.containsKey(key)){
                    put(key, value);
                }
            }
    }

    // Helpers below do NOT lock: callers must already hold `lock`.
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
