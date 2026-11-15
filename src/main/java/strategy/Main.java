package strategy;

import strategy.drive.NormalDrive;
import strategy.drive.OffroadDrive;

public class Main {
    public static void main(String[] args){
        System.out.println("Hello I am main");

        Vehicle sport1 =  new SportsVehicle(new OffroadDrive());

        sport1.drive();

        sport1.upgradeDriveStrategy(new NormalDrive());

        sport1.drive();

        // Way B (OPTION 1): pass a RECIPE. "OffroadDrive::new" means "a function that runs new OffroadDrive()".
        // Nothing is built here; Vehicle's constructor calls factory.create(). The client still chose the policy.
        Vehicle sport2 = new SportsVehicle(OffroadDrive::new);
        sport2.drive();   // Off Road Driving

        // Same recipe as a lambda (useful when construction needs extra steps).
        Vehicle passenger = new PassengerVehicle(() -> new NormalDrive());
        passenger.drive();   // Normal Driving

        // Swap at runtime with a recipe.
        passenger.upgradeDriveStrategy(OffroadDrive::new);
        passenger.drive();   // Off Road Driving

        // OPTION 2 client code would be identical: new OffroadVehicle(OffroadDrive::new)
    }
}
