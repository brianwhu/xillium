package org.xillium.data.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotation to indicate the maximum length of a String value
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface size {
    int value();
    boolean truncate() default false;
}
