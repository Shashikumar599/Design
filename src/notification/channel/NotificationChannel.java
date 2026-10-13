package notification.channel;

import notification.model.Notification;

public interface NotificationChannel {
    public void notifyme(Notification notification, String id);
}
