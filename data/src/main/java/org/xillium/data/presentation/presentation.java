package org.xillium.data.presentation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.xillium.data.Transformer;


/**
 * Annotation to provide an object presentation.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface presentation {
    Class<? extends Transformer> value();
}
