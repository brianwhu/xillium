package org.xillium.core.intrinsic;

import java.util.Map;
import org.xillium.core.*;
import org.xillium.data.*;
import org.xillium.data.persistence.Persistence;
import org.xillium.data.validation.*;


/**
 * Service description.
 */
public class DescService extends SecuredService {
    private final Map<String, String> _descs;

    public DescService(Map<String, String> descs) {
        _descs = descs;
    }

    public DataBinder run(DataBinder binder, Reifier dict, Persistence persist) throws ServiceException {
        String name = binder.get("name");
        if (name != null) {
            binder.put("parameters", _descs.get(name));
        } else {
            binder.put("parameters", null);
        }
        return binder;
    }
}
