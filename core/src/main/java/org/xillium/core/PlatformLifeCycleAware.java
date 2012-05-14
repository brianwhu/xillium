package org.xillium.core;


/**
 * The PlatformLifeCycleAware interface marks a "bean" to receive a notification upon service module loading completion.
 */
public interface PlatformLifeCycleAware {
	/**
     * Invoked immediately after all modules are loaded, this method should gather configuration parameters
     * so the object can be initialized in the next step.
	 */
	public void configure();

    /**
     * Invoked immediately after all PlatformLifeCycleAware objects are configured, this method should initialize the
     * object with the configuration information already in place.
     */
	public void initialize();
}
