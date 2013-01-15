package org.xillium.data.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotation to indicate a regex pattern
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface pattern {
    public static final String COMMON_CURRENCY = "^[+-]?[0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?$";

    String value();
}
