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

import java.security.SecureRandom;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("main");

        TopicHandler topicHandler =  TopicHandler.topicHandler;

        EventListener cricketListener = new CricketListener();
        EventListener weatherListener1  =  new WeatherListener();
        EventListener weatherListener2 =  new WeatherListener();

        Topic cricket =   Cricket.cricket;
        Topic weather =   Weather.weather;

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

        t1.start();
        t2.start();
        SecureRandom secureRandom =  new SecureRandom();
        int i=0;
        while(i<50){
            boolean a = secureRandom.nextBoolean();
            boolean b = secureRandom.nextBoolean();
            boolean c = secureRandom.nextBoolean();
            if(a){
                topicHandler.subscribe(weather , weatherListener1);
            }
            else{
                topicHandler.unsubscribe(weather , weatherListener1);
            }

            if(b){
                topicHandler.subscribe(weather , weatherListener2);
            }
            else{
                topicHandler.unsubscribe(weather , weatherListener2);
            }

            if(c){
                topicHandler.subscribe(cricket , cricketListener);
            }
            else{
                topicHandler.unsubscribe(cricket , cricketListener);
            }
            int sleep = secureRandom.nextInt(500);
            Thread.sleep(sleep);
            i++;
        }

        Thread.sleep(5000);

        cricketPublisher.stop();
        weatherPublisher.stop();
    }
}
