package pubsub;

import pubsub.event.Event;
import pubsub.listener.EventListener;
import pubsub.topic.Topic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class TopicHandler {

    public static final TopicHandler topicHandler = new TopicHandler();
    private TopicHandler(){}


    ConcurrentHashMap<Topic, CopyOnWriteArraySet<EventListener>> listenerQueue = new ConcurrentHashMap<>();

    public void subscribe(Topic topic, EventListener eventListener){
        synchronized (topic){
            CopyOnWriteArraySet<EventListener> listeners = listenerQueue.get(topic);
            if(listeners == null){
                listeners = new CopyOnWriteArraySet<>();
                listenerQueue.put(topic, listeners);
            }
            listeners.add(eventListener);
        }

    }
    public void unsubscribe(Topic topic, EventListener eventListener){
        synchronized (topic){
            CopyOnWriteArraySet<EventListener> listeners = listenerQueue.get(topic);
            if(listeners == null){
                return;
            }
            listeners.remove(eventListener);
            if(listeners.isEmpty()){
                listenerQueue.remove(topic);
            }
        }

    }
    public void handleEvent(Event event, Topic topic){
        CopyOnWriteArraySet<EventListener> listeners = listenerQueue.get(topic);

        if(listeners == null || listeners.isEmpty()){
            return;
        }

        for(EventListener listener : listeners){
            try{
            listener.onEvent(event);
            }
            catch (Exception e){
                System.out.println("Failed to Publish to " + listener +  " " +e.getMessage());
            }
        }

    }
}
