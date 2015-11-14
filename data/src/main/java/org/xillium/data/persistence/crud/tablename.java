package org.xillium.data.persistence.crud;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotation to associate a table name with a Field mapped from a column.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface tablename {
    String value();
}
