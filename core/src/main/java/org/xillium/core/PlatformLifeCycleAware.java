package org.xillium.core;


/**
 * The PlatformLifeCycleAware interface marks a "bean" to receive a notification upon service module loading completion.
 */
public interface PlatformLifeCycleAware {
	/**
     * Invoked immediately after all modules are loaded, this method should gather configuration parameters
     * so the object can be initialized in the next step.
     *
     * @param applName - the Xillium application name (servlet context path without the leading '/')
     * @param moduleName - the Xillium module name in which this PlatformLifeCycleAware is instantiated
	 */
	public void configure(String applName, String moduleName);

    /**
     * Invoked immediately after all PlatformLifeCycleAware objects are configured, this method should initialize the
     * object with the configuration information already in place.
     *
     * @param applName - the Xillium application name (servlet context path without the leading '/')
     * @param moduleName - the Xillium module name in which this PlatformLifeCycleAware is instantiated
     */
	public void initialize(String applName, String moduleName);

    /**
     * Invoked immediately before the service environment is shutdown.
     *
     * @param applName - the Xillium application name (servlet context path without the leading '/')
     * @param moduleName - the Xillium module name in which this PlatformLifeCycleAware is instantiated
     */
	public void terminate(String applName, String moduleName);
}
