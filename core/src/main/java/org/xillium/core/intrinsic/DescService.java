package org.xillium.core.intrinsic;

import java.sql.*;
import java.util.Map;
import java.util.logging.*;
import org.xillium.core.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
//import org.xillium.data.persistence.*;
//import org.springframework.transaction.annotation.*;
//import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * Service description.
 */
public class DescService implements Service {
    private static final Logger _logger = Logger.getLogger(DescService.class.getName());
    private final Map<String, String> _descs;

    public DescService(Map<String, String> descs) {
        _descs = descs;
    }

    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
        String name = binder.get("name");
        if (name != null) {
            binder.put("parameters", _descs.get(name));
        } else {
            binder.put("parameters", null);
        }
        return binder;
    }
}
