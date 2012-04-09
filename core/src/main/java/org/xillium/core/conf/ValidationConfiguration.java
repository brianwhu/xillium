package org.xillium.core.conf;

import org.xillium.core.*;
import org.xillium.data.validation.Dictionary;


/**
 * A collection of Validation registered under unique names.
 */
public class ValidationConfiguration {
    private final Dictionary _dictionary;

    public ValidationConfiguration(Dictionary dictionary) {
        _dictionary = dictionary;
    }

    public void addTypeSet(String className) throws Exception {
        _dictionary.addTypeSet(Class.forName(className));
    }
}
