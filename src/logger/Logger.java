package logger;

public class Logger {
    private Logger(){
        System.out.println("logger intialized");
    }

    // WHAT IS `volatile`?
    //   A keyword for a field that several threads read/write. It gives two guarantees:
    //   1. VISIBILITY: a write by one thread is immediately visible to all other threads.
    //      Without it, each thread may keep a stale copy in its CPU cache/register.
    //   2. ORDERING: the JVM/CPU may not reorder instructions around reads/writes of it.
    //   What it does NOT give: ATOMICITY. `count++` on a volatile int is still unsafe
    //   (read, add, write are 3 steps), and "check null then assign" is still a race.
    //   So volatile is NOT a replacement for synchronized; it only fixes visibility/ordering.
    //   Use it for a flag or a reference published once (like this singleton, with DCL);
    //   don't sprinkle it on every shared resource - mutable shared state that is updated
    //   in several steps needs synchronized, a lock, or an Atomic* class.
    //
    // REVIEW (field):
    // - Should be declared BEFORE the constructor (Java convention: fields, constructors, methods).
    // - For getLogger3 (double-checked locking) this field MUST be `volatile`.
    //   Without it, `logger = new Logger()` can be reordered by the CPU/JIT into:
    //     1. allocate memory  2. assign reference to `logger`  3. run constructor
    //   Another thread could then see a non-null `logger` that is NOT fully constructed.
    //   `volatile` forbids that reordering and guarantees visibility across threads.
    // - Should be `private static volatile Logger logger;` (the "=null" is redundant).
    private static Logger logger=null;

    // ------------------------------------------------------------------------------------
    // getLogger()  -- NOT thread safe
    // ------------------------------------------------------------------------------------
    // (FIXED) The check is now `logger == null`, i.e. "create only if not created yet".
    //   (Earlier it was `!= null`, so the constructor never ran and callers got null.)
    //
    // REMAINING BUG (thread safety): "check-then-act" race condition.
    //   Thread A checks logger == null -> true
    //   Thread B checks logger == null -> true   (A hasn't assigned yet)
    //   Both call `new Logger()` -> TWO instances. Singleton broken.
    //   Also, with no synchronization/volatile there is no guarantee one thread ever sees
    //   the value another thread wrote (visibility problem).
    //   You'd see "logger intialized" printed more than once under concurrency.
    public static Logger getLogger(){
        if(logger == null){
            logger = new Logger();
        }
        return logger;
    }

    // ------------------------------------------------------------------------------------
    // getLogger2()  -- thread safe, but slowest
    // ------------------------------------------------------------------------------------
    // `static synchronized` takes the lock on the Class object (Logger.class). Only one thread
    // at a time can be inside, so the check and the creation happen atomically, and the lock
    // also gives a happens-before guarantee (visibility), so `volatile` isn't needed here.
    //
    // Pros: simplest correct lazy singleton.
    // Cons: EVERY call locks, even long after the instance exists. If getLogger2 is called a
    //       lot (logging usually is!) threads queue up for no reason -> contention.
    public static synchronized  Logger getLogger2(){
        if(logger == null){
            logger = new Logger();
        }
        return logger;
    }

    // ------------------------------------------------------------------------------------
    // getLogger3()  -- double-checked locking (DCL): almost there
    // ------------------------------------------------------------------------------------
    // The idea of DCL: only lock while the instance doesn't exist yet.
    //   1st check (no lock)  -> fast path once initialized, no contention.
    //   2nd check (in lock)  -> needed because two threads can pass the 1st check together;
    //                           the second one in must not create another instance.
    //
    // (FIXED) `synchronized` removed from the method signature, so the lock is only taken
    //   while the instance doesn't exist yet. The outer check is now `== null`.
    //
    // Still to fix:
    // 1. The INNER check (inside the synchronized block) is still `!= null`, so the instance
    //    is never created and this method returns null. Both checks must be `== null`.
    // 2. `logger` is not volatile (see field comment) -> a thread may see a half-built object.
    // Correct DCL = non-synchronized method + `== null` twice + volatile field.
    public static Logger getLogger3(){
        if(logger == null){
            synchronized (Logger.class){
                if(logger != null){
                    logger = new Logger();
                }
            }
        }
        return logger;
    }

    // ------------------------------------------------------------------------------------
    // OTHER WAYS to write a singleton (methods/approaches you could add and compare)
    // ------------------------------------------------------------------------------------
    // 1. Eager initialization
    //      private static final Logger INSTANCE = new Logger();
    //      public static Logger getInstance(){ return INSTANCE; }
    //    The JVM guarantees class initialization is thread safe, so no locks at all.
    //    Downside: created at class-load time even if never used (fine for a cheap object).
    //
    // 2. Initialization-on-demand holder (static inner class)  <-- usually the best choice
    //      private static class Holder { static final Logger INSTANCE = new Logger(); }
    //      public static Logger getInstance(){ return Holder.INSTANCE; }
    //    Lazy (Holder loads only on first getInstance call), lock-free, thread safe because
    //    of the class-loading guarantee. No volatile / synchronized needed.
    //
    // 3. Enum singleton (Joshua Bloch's recommendation, Effective Java)
    //      public enum Logger { INSTANCE; public void log(String m){...} }
    //    Thread safe, and the only version that is also safe against reflection and
    //    serialization attacks (those can create a 2nd instance of the classes above).
    //
    // 4. AtomicReference + compareAndSet (lock-free)
    //    Threads race to set the reference; the loser's object is thrown away. Thread safe
    //    but may construct extra objects, so only OK when the constructor is cheap/side-effect free.
    //
    // Also note: a singleton can be broken by reflection (setAccessible on the private
    // constructor), serialization (deserializing creates a new object unless readResolve is
    // defined) and clone(). Note: in the approaches above (1, 2, 3) the instance field is
    // `static final` or an enum constant, so `volatile` is unnecessary - the JVM already
    // guarantees safe publication for class initialization. Method names like getLogger2/getLogger3 should really be getInstance.

}