package pubsub.listener;

import pubsub.event.Event;

public interface EventListener {
    void onEvent(Event event);
}
