package org.xillium.core.intrinsic;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import org.xillium.base.beans.JSONBuilder;
import org.xillium.data.*;
import org.xillium.core.*;
//import org.xillium.data.validation.*;
//import org.xillium.data.persistence.*;
//import org.springframework.transaction.annotation.*;
//import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * Service listing.
 */
public class ListService implements Service {
    private static final Logger _logger = Logger.getLogger(ListService.class.getName());
    private final Map<String, Service> _services;

    public ListService(Map<String, Service> services) {
        _services = services;
    }

    public DataBinder run(DataBinder binder, ExecutionEnvironment env) throws ServiceException {
        Set<String> keys = _services.keySet();
        binder.put("services", new JSONBuilder(keys.size()*16).append("json:").serialize(keys).toString());
        return binder;
    }
}
