package org.xillium.core.management;

import java.lang.reflect.*;
import java.util.concurrent.Executor;
import java.util.logging.*;
import javax.management.*;
import org.xillium.base.util.Objects;
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

    @Override
    public ObjectName assignObjectName(ObjectName name) {
        if (_name != null) throw new IllegalStateException("ObjectNameAlreadyAssigned");
        return _name = name;
    }

    public ObjectName getObjectName() {
        return _name;
    }

    @Override
    public String getProperty(String name) throws AttributeNotFoundException {
        try {
            return String.valueOf(Objects.getProperty(this, name));
        } catch (NoSuchFieldException x) {
            throw new AttributeNotFoundException(x.getMessage() + ": " + name);
        }
    }

    @Override
    public void setProperty(String name, String value) throws AttributeNotFoundException {
        try {
            Objects.setProperty(this, name, value);
        } catch (NoSuchFieldException x) {
            throw new AttributeNotFoundException(x.getMessage() + ": " + name);
        }
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
    public void emit(Level level, String message, long sequence) {
        if (_broadcaster != null) _broadcaster.sendNotification(new Notification(
            level.toString(),
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
            Level.WARNING.toString(),
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
    public void emit(Level level, String message, long sequence, Logger logger) {
        emit(level, message, sequence);
        logger.log(level, message);
    }

    /**
     * Emits an alert for a caught Throwable through this manageable, entering the alert into a logger along the way.
     */
    @Override
    public <T extends Throwable> T emit(T throwable, String message, long sequence, Logger logger) {
        message = message == null ? Throwables.getFullMessage(throwable) : message + ": " + Throwables.getFullMessage(throwable);
        emit(Level.WARNING, message, sequence, logger);
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

    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        if (_broadcaster != null) _broadcaster.addNotificationListener(listener, filter, handback);
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return (_broadcaster != null) ? _broadcaster.getNotificationInfo() : new MBeanNotificationInfo[0];
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        if (_broadcaster != null) _broadcaster.removeNotificationListener(listener);
    }

    @Override
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        if (_broadcaster != null) _broadcaster.removeNotificationListener(listener, filter, handback);
    }
}
