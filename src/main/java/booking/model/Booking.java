package booking.model;

import java.time.Instant;
import java.util.List;

public class Booking {
    private Instant createdAt;
    private int id;
    private List<Seat> seatsList;
    private Bill bill;
    private PaymentStatus paymentStatus;
}
