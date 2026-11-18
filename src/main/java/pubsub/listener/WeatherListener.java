package pubsub.listener;

import pubsub.event.Event;
import pubsub.event.ScoreEvent;
import pubsub.event.WeatherEvent;

public class WeatherListener implements EventListener{
    @Override
    public void onEvent(Event event) {
        // if event is instance of WeatherEvent
        if(event instanceof WeatherEvent)
            System.out.println("weather event");
    }
}
