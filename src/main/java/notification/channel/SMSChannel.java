package notification.channel;

import notification.model.Notification;

public class SMSChannel implements NotificationChannel{
    private static volatile SMSChannel smsChannel;
    private SMSChannel(){};

    public static NotificationChannel getInstance() {
        if(smsChannel == null){
            synchronized (SMSChannel.class){
                if(smsChannel == null){
                    smsChannel = new SMSChannel();
                }
            }
        }
        return smsChannel;
    }

    @Override
    public void notifyme(Notification notification, String mobileNumber) {
        System.out.println("SMS Notification Recieved " + mobileNumber + " " + notification.getContent());
    }
}
