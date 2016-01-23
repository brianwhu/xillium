package org.xillium.core.management;

import javax.management.*;


/**
 * A JMX bean that can be reloaded and unloaded.
 */
@MXBean
public interface Reloadable {

    /**
     * Reloads this component. If the component is already reloaded, subsequent invocations shall not have any effect.
     *
     * @return a message indicating invocation result
     */
    public String reload();

    /**
     * Unloads the component. If the component is already unloaded, subsequent invocations shall not have any effect.
     *
     * @return a message indicating invocation result
     */
    public String unload();
}
