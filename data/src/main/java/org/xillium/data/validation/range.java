package org.xillium.data.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotation to indicate a range.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface range {
    String min() default "";
    String max() default "";
    boolean inclusive() default true;
}
