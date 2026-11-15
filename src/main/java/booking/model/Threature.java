package booking.model;

import java.util.List;

public class Threature {
    City city;
    int id;
    List<Screen> screenList;

    public void addScreen(Screen screen){
        screenList.add(screen);
        screen.setThreature(this);
    }

    public City getCity() {
        return city;
    }
}
