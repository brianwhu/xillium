package org.xillium.core;

import org.xillium.data.*;


/**
 * A DynamicService interface that exposes its request interface via a public method.
 */
public interface DynamicService extends Service {
    /**
     * Returns the request type of this service, an implementation of DataObject. This method should 
     * never return null. Use DataObject.Empty.class to represent a request type with no parameters.
     */
    public Class<? extends DataObject> getRequestType();
}
