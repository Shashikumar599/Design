package notification.dispatcher;

import notification.model.Notification;
import notification.user.User;

import java.util.List;

public class MassDispatcher implements NotificationDispatcher{
    @Override
    public void sendNotification(List<User>users, Notification notification) {
        for(User user: users){
            System.out.println("Sending Notification to " + user.getUserId());
            user.notify(notification);
        }

    }
}
