package booking;

import booking.billing.BillingAgent;
import booking.model.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Show {
    private final int id;
    private Map<Seat, SeatState> seatMap;
    private BillingAgent billingAgent;
    private Screen screen;

    private ConcurrentHashMap<Seat, Object> seatLock;

    public Show(BillingAgent billingAgent){
        SecureRandom secureRandom = new SecureRandom();
        id = secureRandom.nextInt(100000);

    }

    public Bill book(List<Seat> seats){
        Map<Seat,Object> seatThreadLock =  new HashMap<>();
        try {
            for (Seat seat : seats) {
                seatLock.computeIfAbsent(seat, (seat1) ->
                        {
                            Object obj = new Object();
                            seatThreadLock.put(seat, obj);
                            return obj;
                        }
                        );

            }
            return billingAgent.calculateBill(this, seats);
        }
        catch (Exception e){
            System.out.println("Cannot complete operation");
            return null;
        }
        finally {
            for(Seat seat : seatThreadLock.keySet()){
                seatLock.remove(seat, seatThreadLock.get(seat));
            }
        }

    }
    public City getCity(){
        return this.screen.getThreature().getCity();
    }

}
