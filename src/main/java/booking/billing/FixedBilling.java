package booking.billing;

import booking.Show;
import booking.model.Bill;
import booking.model.Seat;

import java.util.List;

public class FixedBilling implements BillingAgent{
    @Override
    public Bill calculateBill(Show show, List<Seat> seats) {
        double cost = seats.size()*500;
        double extraCharge = cost*0.1;
        double tax = cost*0.18;
        double totalAmount = cost + extraCharge + tax;
        Bill bill = new Bill(totalAmount, extraCharge, tax, cost);
        return bill;
    }
}
