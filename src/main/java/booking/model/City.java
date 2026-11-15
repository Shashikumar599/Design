package booking.model;

import java.security.SecureRandom;

public class City {
    private final int id;
    private final String cityCode;
    public City(String cityCode){
        this.cityCode = cityCode;

        SecureRandom secureRandom = new SecureRandom();
        id = secureRandom.nextInt(100000);

    }
}
