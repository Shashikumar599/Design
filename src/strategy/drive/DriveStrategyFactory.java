package strategy.drive;

// OPTION 1 from discuss.md: a small factory interface of our own (classic Factory pattern).
//
// WHAT IS A FACTORY?  A "recipe" for building an object. Instead of handing Vehicle a ready-made
// DriveStrategy, the client hands it a recipe, and Vehicle decides WHEN to cook it (in its constructor).
//
// WHY BOTHER?  The client still CHOOSES the strategy (it picks which recipe), but Vehicle OWNS the
// creation. Vehicle never writes "new NormalDrive()", so it never depends on a concrete class.
//
// One abstract method => "functional interface": a lambda or method reference fits wherever a
// DriveStrategyFactory is expected:
//     OffroadDrive::new           -> constructor reference
//     () -> new OffroadDrive()    -> same thing as a lambda
//
// Same shape as java.util.function.Supplier<DriveStrategy> (OPTION 2, see commented code in Vehicle).
//   Pro of our own interface: the name explains intent, and we can add parameters later
//   (e.g. create(int horsePower)) without changing callers' types.
//   Con: one extra file.
@FunctionalInterface   // compile error if someone adds a 2nd abstract method
public interface DriveStrategyFactory {
    DriveStrategy create();
}
