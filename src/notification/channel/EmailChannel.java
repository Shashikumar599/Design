package notification.channel;

import notification.model.Notification;

public class EmailChannel implements NotificationChannel{
    private static volatile EmailChannel emailChannel;
    private EmailChannel(){};

    public static NotificationChannel getInstance() {
        if(emailChannel == null){
            synchronized (EmailChannel.class){
                if(emailChannel == null){
                    emailChannel = new EmailChannel();
                }
            }
        }
        return emailChannel;
    }

    @Override
    public void notifyme(Notification notification, String emailId) {
        System.out.println("Email Recieved to email"+ emailId  + " " + notification.getContent());
    }
}
