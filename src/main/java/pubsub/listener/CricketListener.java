package pubsub.listener;

import pubsub.event.Event;
import pubsub.event.ScoreEvent;

public class CricketListener implements EventListener{
    @Override
    public void onEvent(Event event) {
        // instance of cricketEvent
        if(event instanceof ScoreEvent)
          System.out.println("cricket event");
    }
}
