package booking;

import booking.model.Booking;
import booking.model.City;
import booking.model.Seat;

import java.util.*;

public class BookingManager {
    List<Show> shows;
    List<Booking> bookings;
    ArrayDeque<Booking> pendingBookings;
    Map<City,List<Show>> cityShows;
//    Map<Movie, List<Show>> movieShows;

    public BookingManager(){
        shows = new ArrayList<>();
        bookings =  new ArrayList<>();
        pendingBookings =  new ArrayDeque<>();
        cityShows =  new HashMap<>();
    }

    public void addShow(Show show){
        //Add validator if show already exist
        shows.add(show);
        City city = show.getCity();
        if(!cityShows.containsKey(show.getCity())){
            cityShows.put(city, new ArrayList<>());
        }
        cityShows.get(city).add(show);
    }
    public void bookShow(Show show, List<Seat> seats){
        
    }

}
