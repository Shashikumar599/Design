package notification;

import notification.channel.NotificationChannelFactory;
import notification.channel.NotificationChannelType;
import notification.model.Notification;
import notification.model.NotificationScope;
import notification.model.NotificationType;
import notification.user.User;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String args[]){
        System.out.println("Hi , main here");

        NotificationService notificationService = NotificationService.getInstance();



        Notification n1 =  new Notification("hey users there is 50% discount on our site",
                NotificationType.MARKETING, NotificationScope.MULTI);

        List<User> users = new ArrayList<>();
        NotificationChannelFactory notificationChannelFactory = new NotificationChannelFactory();
        User u1 = new User("abc@gmail.com", "3456",notificationChannelFactory);
        u1.setChannel(NotificationChannelType.EMAIL);
        u1.setChannel(NotificationChannelType.SMS);
        User u2 = new User("efg@gmail.com", "7890",notificationChannelFactory);
        u2.setChannel(NotificationChannelType.MOBILE);
        User u3 = new User("ijk@gmail.com", "4523",notificationChannelFactory);
        u3.setChannel(NotificationChannelType.EMAIL);
        u3.setChannel(NotificationChannelType.SMS);
        u3.setChannel(NotificationChannelType.MOBILE);

        u2.unsubscribe(NotificationType.MARKETING);


        users.add(u1);
        users.add(u2);
        users.add(u3);
        notificationService.sendNotification(users, n1);
//using email as name
        Notification n2 =  new Notification("hey" + u2.getEmailId() +"there is 50% discount on our site",
                NotificationType.OFFER, NotificationScope.SINGLE);
        notificationService.sendNotification(new ArrayList<>(List.of(u2)),n2);

    }
}
