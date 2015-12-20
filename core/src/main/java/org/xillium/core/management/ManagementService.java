package org.xillium.core.management;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.xillium.data.DataBinder;
import org.xillium.core.Service;


/**
 * An implementation of Service.Secured, this abstract service authorizes invocation using an injected Authorizer.
 */
public abstract class ManagementService implements Service {
    protected static final long SYNC_DRIFTING_TOLERANCE = 65537L;

    protected boolean sync(String realm, long encoding) {
        if (realm != null) {
            try {
                byte[] bytes = realm.getBytes("UTF-8");
                ByteBuffer buffer = ByteBuffer.allocate(8);
                for (int i = 0; i < bytes.length; i += 8) {
                    buffer.clear();
                    encoding -= buffer.put(bytes, i, Math.min(8, bytes.length - i)).getLong(0);
                }
            } catch (UnsupportedEncodingException x) {}
            if (encoding == 0L) {
                return true;
            } else {
                throw new ManagementRealmEncodingException("InvalidRealmEncoding");
            }
        } else {
            throw new ManagementRealmEncodingException("MissingRealmEncoding");
        }
    }
}
