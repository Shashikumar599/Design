package booking.model;

public class Bill {
    double totalAmount;
    double extraCharge;
    double tax;
    double cost;

    public Bill(double totalAmount, double extraCharge, double tax, double cost){
        this.totalAmount = totalAmount;
        this.extraCharge = extraCharge;
        this.tax = tax;
        this.cost = cost;
    }

}
