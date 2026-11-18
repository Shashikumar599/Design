package pubsub.publisher;

import pubsub.TopicHandler;
import pubsub.event.Event;
import pubsub.event.WeatherEvent;
import pubsub.topic.Topic;

import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherPublisher implements EventPublisher{

    TopicHandler topicHandler = TopicHandler.topicHandler;
    Topic topic;
    SecureRandom secureRandom;
    ExecutorService executor = Executors.newFixedThreadPool(5);
    private volatile boolean shutdown;
    public WeatherPublisher(Topic topic) throws InterruptedException {
        secureRandom = new SecureRandom();
        shutdown = false;
        this.topic = topic;
//        start();
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
                    double temperature =  50 + 50*secureRandom.nextDouble();
                    Event event =  new WeatherEvent(temperature);
                    publish(event);
                }
            });
            Thread.sleep(1000);
        }
    }
    public void stop(){
        System.out.println("Shutdown triggered ");
        shutdown = true;
        executor.shutdown();
    }
}
