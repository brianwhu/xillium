package org.xillium.core.management;

import java.util.concurrent.Executor;
import java.util.logging.*;
import javax.management.*;
import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Throwables;
import org.xillium.core.util.*;


public abstract class ManagedComponent implements Manageable, Reporting, NotificationEmitter {
    private NotificationBroadcaster _broadcaster;
    private MessageChannel _mchannel;
    private ObjectName _name;
    private Status _status = Status.INITIALIZING;
    private boolean _active = true;

    protected ManagedComponent setStatus(Manageable.Status status) {
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
        return this;
    }

    protected ManagedComponent setActive(boolean active) {
        _active = active;
        return this;
    }

    protected Executor getExecutor() {
        return _broadcaster != null ? _broadcaster.getExecutor() : null;
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

    /**
     * Sets(adds) a MessageChannel.
     */
    public void setMessageChannel(MessageChannel channel) {
        if (_mchannel == null) {
            _mchannel = channel;
        } else {
            _mchannel = new CompoundChannel(_mchannel, channel);
        }
    }

    @Override
    public Status getStatus() {
        return _status;
    }

    @Override
    public boolean isActive() {
        return _active;
    }

    /**
     * Emits a notification through this manageable.
     */
    @Override
    public void emit(Severity severity, String message, long sequence) {
        if (_broadcaster != null) _broadcaster.sendNotification(new Notification(
            severity.toString(),
            _name != null ? _name : this,
            sequence,
            message
        ));
    }

    /**
     * Emits an alert for a caught Throwable through this manageable.
     */
    @Override
    public <T extends Throwable> T emit(T throwable, String message, long sequence) {
        if (_broadcaster != null) _broadcaster.sendNotification(new Notification(
            Severity.ALERT.toString(),
            _name != null ? _name : this,
            sequence,
            message == null ? Throwables.getFullMessage(throwable) : message + ": " + Throwables.getFullMessage(throwable)
        ));
        return throwable;
    }

    /**
     * Emits a notification through this manageable, entering the notification into a logger along the way.
     */
    @Override
    public void emit(Severity severity, String message, long sequence, Logger logger) {
        emit(severity, message, sequence);
        logger.log(severity == Severity.NOTICE ? Level.INFO : Level.WARNING, message);
    }

    /**
     * Emits an alert for a caught Throwable through this manageable, entering the alert into a logger along the way.
     */
    @Override
    public <T extends Throwable> T emit(T throwable, String message, long sequence, Logger logger) {
        message = message == null ? Throwables.getFullMessage(throwable) : message + ": " + Throwables.getFullMessage(throwable);
        emit(Severity.ALERT, message, sequence, logger);
        return throwable;
    }

    @Deprecated
    public void sendAlert(Severity severity, String message, long sequence) {
        emit(severity, message, sequence);
    }

    @Deprecated
    public void sendAlert(Logger logger, String message, long sequence) {
        logger.log(Level.WARNING, message);
        sendAlert(Severity.ALERT, message, sequence);
    }

    @Deprecated
    public <T extends Throwable> T sendAlert(T throwable, long sequence) {
        sendAlert(Severity.ALERT, Throwables.getFullMessage(throwable), sequence);
        return throwable;
    }

    @Deprecated
    public <T extends Throwable> T sendAlert(Logger logger, String message, T throwable) {
        logger.log(Level.WARNING, message, throwable);
        sendAlert(Severity.ALERT, message + ": " + Throwables.getFullMessage(throwable), 0);
        return throwable;
    }

    @Override
    public void send(final String subject, final String text) {
        if (_mchannel != null) {
            if (_broadcaster != null) _broadcaster.getExecutor().execute(new Runnable() {
                public void run() { _mchannel.sendMessage(subject, text); }
            });
            else _mchannel.sendMessage(subject, text);
        }
    }

    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        if (_broadcaster != null) _broadcaster.addNotificationListener(listener, filter, handback);
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return (_broadcaster != null) ? _broadcaster.getNotificationInfo() : new MBeanNotificationInfo[0];
    }

    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        if (_broadcaster != null) _broadcaster.removeNotificationListener(listener);
    }

    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        if (_broadcaster != null) _broadcaster.removeNotificationListener(listener, filter, handback);
    }
}
