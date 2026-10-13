package notification.dispatcher;

import notification.model.NotificationScope;

public class DispatcherFactory {
    public NotificationDispatcher getDispatcher(NotificationScope scope){
        if(scope.equals(NotificationScope.SINGLE)){
            return new SingleDispatcher();
        }
        else{
            return new MassDispatcher();
        }
    }

}
