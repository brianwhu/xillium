package org.xillium.base.etc;

import java.util.logging.*;
import java.util.Date;
import java.text.MessageFormat;


public class ShortLoggerFormatter extends Formatter {
    private static final String LOG_MSG_PREFIX = "##";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final MessageFormat formatter = new MessageFormat("{0,date,short} {0,time,short}");

    private Object args[] = new Object[]{ new Date() };
    private StringBuffer text = new StringBuffer(LOG_MSG_PREFIX);

    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record) {
        text.setLength(LOG_MSG_PREFIX.length());

        // date
        ((Date)args[0]).setTime(record.getMillis());
        text.append('|');
        formatter.format(args, text, null);

        // logging level
        text.append('|').append(record.getLevel().getLocalizedName());

        // thread id
        text.append("|t-").append(record.getThreadID());

        // source
        text.append('|');
        if (record.getSourceClassName() != null) {
            text.append(record.getSourceClassName().substring(record.getSourceClassName().lastIndexOf('.')+1));
        } else {
            text.append(record.getLoggerName());
        }
        if (record.getSourceMethodName() != null) {
            text.append('[');
            text.append(record.getSourceMethodName().substring(record.getSourceMethodName().lastIndexOf('.')+1));
            text.append(']');
        }

        // message
        text.append(' ').append(formatMessage(record)).append(LINE_SEPARATOR);

        if (record.getThrown() != null) {
            StackTraceElement[] traces = record.getThrown().getStackTrace();
            for (int i = 0; i < traces.length; ++i) {
                text.append(traces[i].toString()).append(LINE_SEPARATOR);
            }
        }

        return text.toString();
    }
}
