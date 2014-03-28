package org.xillium.core;

import java.util.*;
import org.xillium.data.DataBinder;


/**
 * Persistent.
 *
 * An implementation of this interface will be wired through Spring and transaction enabled.
 */
public interface Persistent {
    /**
     * A Persistent task that can be wrapped in a Transaction.
     */
    public static interface Task<T, F> {
        public T run(F facility, Persistence persistence) throws Exception;
    }

    /**
     * Executes a task within a read-only transaction, which might be rolled back but only when a runtime exception is thrown by the task.
     */
    public <T, F> T doReadOnly(F facility, Task<T, F> task);

    /**
     * Executes a task within a read-write transaction, which might be rolled back but only when a runtime exception is thrown by the task.
     */
    public <T, F> T doReadWrite(F facility, Task<T, F> task);
}
