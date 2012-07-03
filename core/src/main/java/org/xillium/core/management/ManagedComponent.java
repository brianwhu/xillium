package org.xillium.core.management;

//import java.util.*;
//import java.util.concurrent.Executor;
import javax.annotation.Resource;
import javax.management.*;


public abstract class ManagedComponent implements Manageable {
	@Resource
	private NotificationBroadcasterSupport jmxBroadcaster;

	private Status _status = Status.HEALTHY;

	public Status getStatus() {
		return _status;
	}

	protected void setStatus(Manageable.Status status) {
		if (jmxBroadcaster != null) jmxBroadcaster.sendNotification(new AttributeChangeNotification(
			this,
			0,
			System.currentTimeMillis(),
			"Status Change",
			"Status",
			Manageable.Status.class.getName(),
			_status,
			status
		));
		_status = status;
	}

    public void sendAlert(Manageable.Severity severity, String message, long sequence) {
		if (jmxBroadcaster != null) jmxBroadcaster.sendNotification(new AlertNotification(
            severity,
			this,
			sequence,
			message
		));
    }
}
