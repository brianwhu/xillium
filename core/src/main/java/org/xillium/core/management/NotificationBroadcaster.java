package org.xillium.core.management;

import java.util.concurrent.Executor;
import java.util.logging.Level;
import javax.management.*;


public class NotificationBroadcaster extends NotificationBroadcasterSupport {
    private final Executor _executor;

    public NotificationBroadcaster(Executor executor) {
        super(executor);
        _executor = executor;
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(
                new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE },
                AttributeChangeNotification.class.getName(),
                "Bean attribute change"
            ),
            new MBeanNotificationInfo(
                new String[] { Level.SEVERE.toString(), Level.WARNING.toString(), Level.INFO.toString() },
                Notification.class.getName(),
                "Alert"
            )
        };
    }

    Executor getExecutor() {
        return _executor;
    }
}
