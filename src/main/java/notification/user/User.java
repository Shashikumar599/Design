package notification.user;

import notification.channel.NotificationChannel;
import notification.channel.NotificationChannelFactory;
import notification.channel.NotificationChannelType;
import notification.model.Notification;
import notification.model.NotificationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class User {
    private String userId;
    private String emailId;
    private String appCode;
    private String mobileNumber;
    private NotificationChannelFactory channelFactory;
    private Map<NotificationChannelType, String> notificationChannelTypeIdMap;
    private List<NotificationType> notificationTypes;
    //demonstrate stratergy pattern
    private List<NotificationChannelType> notificationChannelTypes;
    public User(String emailId, String mobileNumber, NotificationChannelFactory channelFactory){
        userId = String.valueOf(ThreadLocalRandom.current().nextInt(0,10000));
        this.emailId = emailId;
        appCode = String.valueOf(ThreadLocalRandom.current().nextInt(0,10000));
        this.mobileNumber = mobileNumber;
        notificationChannelTypes= new ArrayList<>();
        notificationTypes =  new ArrayList<>(List.of(NotificationType.BUSSINESS, NotificationType.MARKETING, NotificationType.OFFER));
        this.channelFactory = channelFactory;
        notificationChannelTypeIdMap = new HashMap<>();
        notificationChannelTypeIdMap.put(NotificationChannelType.EMAIL, emailId);
        notificationChannelTypeIdMap.put(NotificationChannelType.SMS, mobileNumber);
        notificationChannelTypeIdMap.put(NotificationChannelType.MOBILE, appCode);
        //Above map need update if we allow email and phone number update , missing implementation
    }

    public void notify(Notification notification){
        if(!notificationTypes.contains(notification.getType())){
            return;
        }
        for(NotificationChannelType notificationChannelType : notificationChannelTypes){
            NotificationChannel notificationChannel = channelFactory.getNotificationChannel(notificationChannelType);
            try{
            notificationChannel.notifyme(notification, notificationChannelTypeIdMap.get(notificationChannelType));}
            //id check handling responsibility of channel , to be implemented
            catch (Exception e) {
                System.out.println("Unable to send "+ notificationChannelType + " notification for " + userId);
            }
        }
    }
    public String getNotificationId(NotificationChannelType channelType){
        if(channelType.equals(NotificationChannelType.EMAIL)){
            if(emailId==null){
                throw new RuntimeException("Invalid email");
            }
            return this.emailId;
        }
        else if(channelType.equals(NotificationChannelType.MOBILE)){
            if(appCode==null){
                throw new RuntimeException("Invalid appCode");
            }
            return this.appCode;
        }
        else if(channelType.equals(NotificationChannelType.SMS)){
            if(mobileNumber==null){
                throw new RuntimeException("Invalid mobileNumber");
            }
            return this.mobileNumber;
        }
        else{
            throw new RuntimeException("Channel Type not supported");
        }
    }

    public String getUserId() {
        return userId;
    }
    public void setChannel(NotificationChannelType notificationChannelType){
        if(!notificationChannelTypes.contains(notificationChannelType)){
            notificationChannelTypes.add(notificationChannelType);
        }
    }
    public void unsetChannel(NotificationChannelType notificationChannelType){
        notificationChannelTypes.remove(notificationChannelType);
    }

    public String getEmailId() {
        return emailId;
    }
    public void subscribe(NotificationType notificationType){
        if(!notificationTypes.contains(notificationType)){
            notificationTypes.add(notificationType);
        }
    }
    public void unsubscribe(NotificationType notificationType){
        notificationTypes.remove(notificationType);
    }

}

