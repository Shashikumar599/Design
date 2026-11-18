package pubsub.publisher;

import pubsub.TopicHandler;
import pubsub.event.Event;
import pubsub.event.ScoreEvent;
import pubsub.event.WeatherEvent;
import pubsub.topic.Topic;

import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CricketPublisher implements EventPublisher{

    TopicHandler topicHandler = TopicHandler.topicHandler;
    SecureRandom secureRandom;
    ExecutorService executor = Executors.newFixedThreadPool(5);
    private volatile boolean shutdown;
    Topic topic;
    public CricketPublisher(Topic topic){
        secureRandom =  new SecureRandom();
        this.topic = topic;
        shutdown = false;
    }

    @Override
    public void publish(Event event) {
        topicHandler.handleEvent(event, topic);
    }
    public void start() throws InterruptedException {
        while(true){
            if(shutdown){
                return;
            }
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    int  score =  secureRandom.nextInt()%100;
                    Event event =  new ScoreEvent(score);
                    publish(event);
                }
            });
            Thread.sleep(2000);
        }
    }
    public void stop(){
        System.out.println("Shutdown triggered ");
        shutdown = true;
        executor.shutdown();
    }
}
