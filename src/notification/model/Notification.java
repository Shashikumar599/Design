package notification.model;

public class Notification {
    private String content;
    private NotificationType type;
    private NotificationScope scope;

    public Notification(String content, NotificationType type, NotificationScope scope){
        this.content = content;
        this.type = type;
        this.scope = scope;
    }

    public String getContent() {
        return content;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationScope getScope() {
        return scope;
    }
}
