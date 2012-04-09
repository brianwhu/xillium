package org.xillium.data.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotation to indicate a regex pattern
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface pattern {
    String value();
}
