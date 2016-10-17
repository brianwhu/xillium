package org.xillium.core.intrinsic;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;
import org.xillium.base.beans.JSONBuilder;
import org.xillium.base.util.Pair;
import org.xillium.data.*;
import org.xillium.data.persistence.Persistence;
import org.xillium.core.*;
import org.xillium.data.validation.*;


/**
 * Service listing.
 */
public class ListService extends SecuredService {
    private final Map<String, Pair<Service, Persistence>> _services;

    public ListService(Map<String, Pair<Service, Persistence>> services) {
        _services = services;
    }

    public DataBinder run(DataBinder binder, Reifier dict, Persistence persist) throws ServiceException {
        Set<String> keys = new TreeSet<String>(_services.keySet());
        for (Iterator<String> it = keys.iterator(); it.hasNext();) {
            if (it.next().startsWith("x!/")) it.remove();
        }
        binder.put("services", new JSONBuilder(keys.size()*16).append("json:").serialize(keys).toString());
        return binder;
    }
}
