package pubsub.publisher;

import pubsub.event.Event;
import pubsub.listener.EventListener;
import pubsub.topic.Topic;

public interface EventPublisher {
    void publish(Event event);
    public void stop();
    public void start()throws InterruptedException;
}
