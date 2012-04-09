package org.xillium.core.conf;

import org.xillium.data.persistence.*;
import java.util.*;


/**
 * A collection of ParametricStatements registered under unique names.
 */
public class StorageConfiguration {
    private final Map<String, ParametricStatement> _statements;
    private final String _namespace;

    public StorageConfiguration(Map<String, ParametricStatement> registry, String namespace) {
        _statements = registry;
        _namespace = namespace;
    }

    public void addParametricStatement(ParametricStatement statement, String name) {
        _statements.put(_namespace + '/' + name, statement);
    }
}
