package org.xillium.data.persistence;

import org.xillium.data.*;
import java.sql.*;


/**
 * All query results are passed to a ResultSetWorker, which processes the result set and produces an object of type T.
 */
public interface ResultSetWorker<T> {
    public T process(ResultSet rs) throws Exception;
}
