package notification;

import notification.dispatcher.DispatcherFactory;
import notification.dispatcher.NotificationDispatcher;
import notification.model.Notification;
import notification.user.User;

import java.util.List;

//Singleton keyword not available , claude why
public class NotificationService {
    private DispatcherFactory dispatcherFactory;
    private static volatile NotificationService notificationService;
    private NotificationService(){
        dispatcherFactory = new DispatcherFactory();
    }
    public static NotificationService getInstance(){
        if(notificationService == null){
            synchronized (NotificationService.class){
                if(notificationService == null){
                    return notificationService = new NotificationService();
                }
            }
        }
        return notificationService;
    }
    public void sendNotification(List<User> users, Notification notification){
        NotificationDispatcher dispatcher = dispatcherFactory.getDispatcher(notification.getScope());
        System.out.println("Sending notification , notifcation service");
        dispatcher.sendNotification(users, notification);
    }
}
