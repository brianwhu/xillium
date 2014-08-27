package org.xillium.core.intrinsic;

import org.xillium.data.*;
import org.xillium.core.*;
import org.xillium.data.validation.*;


/**
 * Ping &amp; echo.
 */
public class PingService implements Service {
    @Override
    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
        binder.put("echo", String.valueOf(System.currentTimeMillis()));
        return binder;
    }
}
