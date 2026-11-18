package pubsub.event;

public class WeatherEvent implements Event{
    double temperature;
    public WeatherEvent(double temperature){
        this.temperature = temperature;
    }

    public double getTemperature() {
        return temperature;
    }
}
