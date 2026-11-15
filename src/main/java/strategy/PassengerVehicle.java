package strategy;

import strategy.drive.DriveStrategy;
import strategy.drive.DriveStrategyFactory;
// import java.util.function.Supplier;   // needed only for OPTION 2 below

public class PassengerVehicle extends Vehicle{
    // Way A: client hands over a ready-made strategy.
    public PassengerVehicle(DriveStrategy driveStrategy){
        super(driveStrategy);
    }

    // Way B, OPTION 1: client hands over a recipe; Vehicle builds the strategy.
    // Constructors are not inherited in Java, so each subclass re-declares the ones it offers
    // and forwards them to super(...).
    public PassengerVehicle(DriveStrategyFactory factory){
        super(factory);
    }

    // Way B, OPTION 2 (commented): enable only with the Supplier constructor in Vehicle, and disable option 1.
    // public PassengerVehicle(Supplier<DriveStrategy> factory){
    //     super(factory);
    // }
}
