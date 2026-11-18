package pubsub;

import pubsub.listener.CricketListener;
import pubsub.listener.EventListener;
import pubsub.listener.WeatherListener;
import pubsub.publisher.CricketPublisher;
import pubsub.publisher.EventPublisher;
import pubsub.publisher.WeatherPublisher;
import pubsub.topic.Cricket;
import pubsub.topic.Topic;
import pubsub.topic.Weather;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("main");

        TopicHandler topicHandler =  TopicHandler.topicHandler;

        EventListener cricketListener = new CricketListener();
        EventListener weatherListener1  =  new WeatherListener();
        EventListener weatherListener2 =  new WeatherListener();

        Topic cricket =  new Cricket();
        Topic weather =  new Weather();

        EventPublisher cricketPublisher = new CricketPublisher(cricket);
        EventPublisher weatherPublisher = new WeatherPublisher(weather);

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cricketPublisher.start();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    weatherPublisher.start();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        topicHandler.subscribe(weather , weatherListener1);
        topicHandler.subscribe(weather, weatherListener2);
        topicHandler.subscribe(cricket, cricketListener);



        t1.start();
        t2.start();

        Thread.sleep(5000);

        cricketPublisher.stop();
        weatherPublisher.stop();
    }
}
