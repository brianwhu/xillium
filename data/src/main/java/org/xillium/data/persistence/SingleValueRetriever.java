package org.xillium.data.persistence;

//import org.xillium.data.*;
//import java.math.BigDecimal;
import java.sql.*;
//import java.util.*;
//import java.lang.reflect.Field;


/**
 * A ParametricQuery.ResultSetWorker implementation that returns the first colum of the first row as a single object.
 */
public class SingleValueRetriever<T> implements ParametricQuery.ResultSetWorker<T> {
	public T process(ResultSet rs) throws Exception {
		if (rs.next()) {
			return (T)rs.getObject(1);
		} else {
			return null;
		}
	}
}

