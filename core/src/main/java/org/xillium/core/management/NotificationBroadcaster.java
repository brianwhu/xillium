package org.xillium.core.management;

import java.util.concurrent.Executor;
import javax.management.*;


public class NotificationBroadcaster extends NotificationBroadcasterSupport {
	public NotificationBroadcaster(Executor executor) {
		super(executor);
	}

	public MBeanNotificationInfo[] getNotificationInfo() {
		return new MBeanNotificationInfo[] {
			new MBeanNotificationInfo(
				new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE },
				AttributeChangeNotification.class.getName(),
				"Bean attribute change"
			),
			new MBeanNotificationInfo(
				new String[] { Manageable.Severity.ALERT.toString(), Manageable.Severity.NOTICE.toString() },
				Notification.class.getName(),
				"Alert"
			)
		};
	}
}
