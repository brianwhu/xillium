package lab.data.persistence;

import org.xillium.data.persistence.*;
import java.util.*;


/**
 * A data storage facade.
 */
public class StorageConfiguration {
    private static final Map<String, ParametricStatement> _statements = new HashMap<String, ParametricStatement>();

    public void addParametricStatement(ParametricStatement statement, String name) {
        _statements.put(name, statement);
    }

    public static ParametricStatement getParametricStatement(String name) {
        return _statements.get(name);
    }
}
