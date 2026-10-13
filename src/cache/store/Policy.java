package cache.store;

// The eviction policies the client can choose from.
// Adding a policy = add a constant here + one case in DefaultStoreFactory.create().
public enum Policy {
    LRU,
    LFU
}
