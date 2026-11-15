package booking.billing;

import booking.Show;
import booking.model.Bill;
import booking.model.Seat;

import java.util.List;

public interface BillingAgent {
    public Bill calculateBill(Show show, List<Seat> seats);
}
