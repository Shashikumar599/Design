package notification.dispatcher;

import notification.model.Notification;
import notification.user.User;

import java.util.List;

public class SingleDispatcher implements NotificationDispatcher{
    @Override
    public void sendNotification(List<User> users, Notification notification) {
        // single user and null checks to be added
        User user = users.get(0);
        System.out.println("Sending Notification to " + user.getUserId());
        user.notify(notification);
    }
}
