package org.xillium.core.conf;

import org.xillium.data.validation.Reifier;


/**
 * A collection of Validation registered under unique names.
 */
public class ValidationConfiguration {
    private final Reifier _reifier;

    public ValidationConfiguration(Reifier dictionary) {
        _reifier = dictionary;
    }

    public void addTypeSet(String className) throws Exception {
        _reifier.addTypeSet(Class.forName(className));
    }
}
