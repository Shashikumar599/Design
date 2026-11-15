package notification.channel;

import java.util.HashMap;
import java.util.Map;

//This to be singlton
public class NotificationChannelFactory {
    private Map<NotificationChannelType, NotificationChannel> channelMap;
    public NotificationChannelFactory(){
        channelMap = new HashMap<>();
        channelMap.put(NotificationChannelType.EMAIL, EmailChannel.getInstance());
        channelMap.put(NotificationChannelType.SMS, SMSChannel.getInstance());
        channelMap.put(NotificationChannelType.MOBILE, MobileChannel.getInstance());
    }
    public NotificationChannel getNotificationChannel(NotificationChannelType notificationChannelType){
        NotificationChannel notificationChannel = channelMap.get(notificationChannelType);
        if(notificationChannel==null){
            throw new RuntimeException("Invalid Notification Channel Type, it may require registartion");
        }
        return notificationChannel;
    }
}
