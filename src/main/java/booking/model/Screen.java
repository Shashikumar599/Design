package booking.model;

import java.util.List;

public class Screen {
    private List<Seat> seats;
    private Threature threature;

    public Threature getThreature() {
        return threature;
    }

    public void setThreature(Threature threature) {
        this.threature = threature;
    }
    public List<Seat> getSeats(){
        return seats;
    }
}
