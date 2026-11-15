package strategy;

import strategy.drive.DriveStrategy;
import strategy.drive.DriveStrategyFactory;
// import java.util.function.Supplier;   // needed only if you enable OPTION 2 below

// REVIEW (Strategy pattern) - hints, not solutions:
// 1. [DONE] Spelling "Stratergy"/"Stratrgy" -> "Strategy" fixed in package, interface, field and method names.
// 2. [DONE] drive() now delegates to the strategy - this was the core of the pattern.
// 3. Subclasses (Passenger/Sports/Offroad) only differ by the strategy passed to super(...).
//    Is subclassing really needed here? If Vehicle should never be created directly, what keyword fits?
// 4. [DONE] Sports and Offroad sharing OffroadDrive is intentional: it shows the reuse Strategy gives
//    (one behaviour class shared by many vehicles). My earlier "copy-paste bug" note was wrong.
//    Tip: a short comment where the strategy is shared would make the intent obvious to readers.
// 5. [DONE] Runtime swapping is now possible via upgradeDriveStrategy(). Is "upgrade" the best verb
//    for swapping to any strategy? Consider a more neutral name.
// 6. Minor: DriveStrategy has a redundant "public" on the interface method; missing space before "{"
//    in the subclasses; no main() anywhere to actually run and see the pattern work.
public class Vehicle {
    // REVIEW: now private, good. It is reassigned by upgradeDriveStrategy(), so it can't be final.
    private DriveStrategy driveStrategy;

    // ---- Way A: client passes a READY-MADE strategy (the original way) ----
    //   new SportsVehicle(new OffroadDrive())
    // The client creates the object, so it must know the concrete class.
    public Vehicle(DriveStrategy driveStrategy){
        // REVIEW: what happens if someone passes null here? Is that acceptable?
        this.driveStrategy = driveStrategy;
    }

    // ---- Way B, OPTION 1: client passes a RECIPE (our own factory interface) ----
    //   new SportsVehicle(OffroadDrive::new)
    // The client still decides WHICH strategy (it picks the recipe), but Vehicle decides WHEN to build
    // it: factory.create() runs right here. Vehicle never writes "new OffroadDrive()", so it stays
    // decoupled from every concrete strategy class.
    // (Way A vs B compile fine side by side: DriveStrategy.drive() is void and OffroadDrive::new
    //  produces an object, so only the factory interface can accept the constructor reference.)
    public Vehicle(DriveStrategyFactory factory){
        this.driveStrategy = factory.create();
    }

    // ---- Way B, OPTION 2 (kept commented): same idea with the built-in Supplier<DriveStrategy> ----
    // Supplier<T> = "takes nothing, returns a T", exactly DriveStrategyFactory.create().
    // Client code is IDENTICAL to option 1 (OffroadDrive::new); only the parameter type changes.
    // Pro: no extra interface file. Con: the name says nothing about intent; can't add methods.
    //
    // Why commented: next to Option 1 it is a second overload of the same shape, so
    // new Vehicle(OffroadDrive::new) fails with "reference to Vehicle is ambiguous". Pick ONE.
    //
    // public Vehicle(Supplier<DriveStrategy> factory){
    //     this.driveStrategy = factory.get();   // Supplier's method is get(), not create()
    // }
    //
    // (discuss.md also uses Function<Integer, Store>: same idea when the recipe needs an input,
    //  e.g. Function<Integer, DriveStrategy> would be called with factory.apply(horsePower).)

    public void upgradeDriveStrategy(DriveStrategy driveStrategy){
        this.driveStrategy = driveStrategy;
    }

    // Swap at runtime using a recipe instead of a ready-made object.
    public void upgradeDriveStrategy(DriveStrategyFactory factory){
        this.driveStrategy = factory.create();
    }

    public void drive(){
        driveStrategy.drive();
    }
}
