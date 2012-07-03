package org.xillium.core.management;

import java.util.*;
import javax.management.*;


/**
 * An alert notification.
 */
public class AlertNotification extends Notification {
    public static final String ALERT = "jmx.xillium.alert";

    private final Manageable.Severity _severity;

	public AlertNotification(Manageable.Severity severity, Object source, long sequence, String message) {
        super(ALERT, source, sequence, message);
        _severity = severity;
    }

    public Manageable.Severity getSeverity() {
        return _severity;
    }
}
