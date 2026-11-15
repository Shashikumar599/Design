package notification.channel;

import notification.model.Notification;

public class MobileChannel implements NotificationChannel{
    private static volatile MobileChannel mobileChannel;
    private MobileChannel(){};

    public static NotificationChannel getInstance() {
        if(mobileChannel == null){
            synchronized (MobileChannel.class){
                if(mobileChannel == null){
                    mobileChannel = new MobileChannel();
                }
            }
        }
        return mobileChannel;
    }
    @Override
    public void notifyme(Notification notification, String appCode) {
        System.out.println("Mobile Notification Recieved " + appCode + " " + notification.getContent());
    }
}
