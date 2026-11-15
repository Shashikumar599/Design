package notification.dispatcher;

import notification.model.Notification;
import notification.user.User;

import java.util.List;

public interface NotificationDispatcher {
    public void sendNotification(List<User> users, Notification notification);
}
