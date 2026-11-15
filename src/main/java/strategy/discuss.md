Q1. Without generics, how can the client still choose the store while Cache creates it? Is there a factory approach?
Yes. Generics were never what made the factory work. They only made it type-safe. Assume Store is non-generic (say Store with String get(String) and void put(String, String)). You have four options:

1. A small factory interface of your own (classic Factory pattern).

interface StoreFactory { Store create(int capacity); }
The client passes capacity -> new LRUStore(capacity) or LRUStore::new. Cache calls factory.create(capacity). This is the same idea as Function<Integer, Store>, just with a name that explains itself.

Pro: most readable in an interview, and you can add more methods later.
Con: one extra file or interface.
2. Function<Integer, Store>. The same as option 1, using the built-in interface. Minimal code, which is why the Cache I wrote uses it.

3. An enum that knows how to build (a factory method on the enum).

enum Policy { LRU, LFU }
The client passes Policy.LRU and Cache switches on it: case LRU -> new LRUStore(capacity).

Pro: very easy for the client, with nothing to learn.
Con: Cache (or the enum) now names every concrete class, so adding FIFO means editing it. This breaks the open-closed principle. Fine for 2 or 3 policies.
4. Pass a Class and create by reflection. Cache(int capacity, Class<? extends Store> type), then type.getConstructor(int.class).newInstance(capacity).

Con: errors show up at runtime instead of compile time, and it is slow and brittle. Don't use it here. Mention it only so you know it exists.
Which one for an interview? Option 1 or 2 if asked to be extensible, option 3 if asked to keep the API dead simple. In every case the client decides and the cache builds, which is exactly what you wanted.

Extra note: the same four options work with generics, and the generics only add <K,V>. The factory approach is independent of them.

help me with implementation using 1,2 

implement constructor for 2 just keep it commented as likely client code would be same implementation specific to 2 can be commented 
throughly add comments for understanding , work in context of strategy folder