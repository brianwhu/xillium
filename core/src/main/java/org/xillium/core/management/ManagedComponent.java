package org.xillium.core.management;

import javax.management.*;
import org.xillium.base.beans.Throwables;


public abstract class ManagedComponent implements Manageable, NotificationEmitter {
	private NotificationBroadcaster _broadcaster;
	private ObjectName _name;
	private Status _status = Status.INITIALIZING;

	protected void setStatus(Manageable.Status status) {
		if (_broadcaster != null) _broadcaster.sendNotification(new AttributeChangeNotification(
			_name != null ? _name : this,
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

    /**
     * Creates a ManagedComponent whose NotificationBroadcaster is to be wired via DI.
     */
    public ManagedComponent() {
    }

    /**
     * Creates a ManagedComponent whose NotificationBroadcaster is to be provided directly.
     */
    public ManagedComponent(NotificationBroadcaster broadcaster) {
        _broadcaster = broadcaster;
    }

    public ObjectName assignObjectName(ObjectName name) {
        if (_name != null) throw new IllegalStateException("ObjectNameAlreadyAssigned");
        return _name = name;
    }

    public ObjectName getObjectName() {
        return _name;
    }

    public NotificationBroadcaster getNotificationBroadcaster() {
        return _broadcaster;
    }

    public void setNotificationBroadcaster(NotificationBroadcaster broadcaster) {
        _broadcaster = broadcaster;
    }

	public Status getStatus() {
		return _status;
	}

    public void sendAlert(Manageable.Severity severity, String message, long sequence) {
		if (_broadcaster != null) _broadcaster.sendNotification(new Notification(
            severity.toString(),
			_name != null ? _name : this,
			sequence,
			message
		));
    }

    public <T extends Throwable> T sendAlert(T throwable, long sequence) {
        sendAlert(Manageable.Severity.ALERT, Throwables.getFullMessage(throwable), sequence);
        return throwable;
    }

    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        _broadcaster.addNotificationListener(listener, filter, handback);
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return _broadcaster.getNotificationInfo();
    }

    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        _broadcaster.removeNotificationListener(listener);
    }

    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        _broadcaster.removeNotificationListener(listener, filter, handback);
    }
}
