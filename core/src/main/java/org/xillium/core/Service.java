package org.xillium.core;

import org.xillium.data.DataBinder;
import org.xillium.data.persistence.Persistence;
import org.xillium.data.validation.Reifier;


/**
 * A Service interface. Transactions should be managed using Spring's @org.springframework.transaction.annotation.Transactional
 * annotation on the implementation class or method.
 */
public interface Service {
    public static final String REQUEST_CLIENT_ADDR = "#client_addr#";
    public static final String REQUEST_CLIENT_PORT = "#client_port#";
    public static final String REQUEST_CLIENT_PHYS = "#client_phys#";
    public static final String REQUEST_SERVLET_REQ = "#servlet_req#";
    public static final String REQUEST_SERVER_PORT = "#server_port#";
    public static final String REQUEST_SERVER_PATH = "#server_path#";
    public static final String REQUEST_TARGET_PATH = "#target_path#";
    public static final String REQUEST_HTTP_METHOD = "#http_method#";
    public static final String REQUEST_HTTP_COOKIE = "#http_cookie#";
    public static final String REQUEST_HTTP_SECURE = "#http_secure#";
    public static final String REQUEST_JS_CALLBACK = "_js_callback_";
    public static final String REQUEST_HTTP_STATUS = "_http_status_";

    public static final String SERVICE_DATA_BINDER = "#data_binder#";
    public static final String SERVICE_POST_ACTION = "#post_action#";
    public static final String SERVICE_PAGE_TARGET = "#page_target#";
    public static final String SERVICE_JSON_TUNNEL = "_json_tunnel_";
    public static final String SERVICE_HTTP_HEADER = "#http_header#";
    public static final String SERVICE_HTTP_STATUS = "#http_status#";
    public static final String SERVICE_DO_REDIRECT = "#do_redirect#";
    public static final String SERVICE_STACK_TRACE = "#stack_trace#";
    public static final String SERVICE_XML_CONTENT = "#xml_content#";

    /**
     * A failure message is a string suitable for display to an end user. This value is only present when the service fails.
     */
    public static final String FAILURE_MESSAGE = "_message_";

    /**
     * A failure stack trace is only present when the FAILURE_MESSAGE is.
     */
    public static final String FAILURE_STACK   = "_stack_";

	/**
	 * Processes a service request. This method runs in a thread parallel to other service calls, and must return as quickly as possible.
     *
     * If the service is wrapped inside a transaction, a runtime exception (including ServiceException) by this method rolls back the
     * transaction, while a checked exception does not.
	 *
	 * @param parameters - request parameters in a DataBinder
	 * @param dict - a Reifier for data validation/parsing
	 * @param persist - a Persistence for data persistence
	 * @return response data in a DataBinder; This can be the same data binder passed in as the first argument
	 * @throws ServiceException if the service fails for any reason. If the service is wrapped inside a transaction, the transaction
	 *         is rolled back.
	 */
	public DataBinder run(DataBinder parameters, Reifier dict, Persistence persist) throws ServiceException;

	/**
	 * Interface to indicate a secured service.
	 */
	public static interface Secured extends Service {
		public void authorize(String deployment, DataBinder parameters, Persistence persist) throws AuthorizationException;
	}

    /**
     * A service filter.
     */
    public static interface Filter {
        /**
         * Performs request filtration at the very beginning of the service. A runtime exception thrown by this method stops the service.
         */
        public void filtrate(DataBinder parameters) throws ServiceException;

        /**
         * Performs request acknowledgement just before the service transaction starts.
         *
         * Anything thrown from this method is silently ignored.
         */
        public void acknowledge(DataBinder parameters) throws Exception;

        /**
         * Performs extra processes after the service is successful and the associated transaction has been committed.
         * It will NOT run if the service has failed.
         *
         * Anything thrown from this method is silently ignored.
         */
        public void successful(DataBinder parameters) throws Exception;

        /**
         * Performs extra processes after the service has failed and the associated transaction has been rolled back.
         * It will NOT run if the service is successful.
         *
         * Anything thrown from this method is silently ignored.
         */
        public void aborted(DataBinder parameters, Throwable throwable) throws Exception;

        /**
         * Performs extra processes after the service has completed either successfully or with failures.
         *
         * Anything thrown from this method is silently ignored.
         */
        public void complete(DataBinder parameters) throws Exception;
    }

	/**
	 * Interface to indicate a service that has the same extra steps as defined in the Filter interface.
	 */
	public static interface Extended extends Service, Filter {
	}

    /**
     * Interface to indicate an Extended service that calls installed filters when its own filter methods are called by the service platform.
     */
    public static interface Extendable extends Extended {
        /**
         * Installs a service filter. Multiple invocations of this method should install <i>all</i> filters provided.
         */
        public void setFilter(Filter filter);
    }

    /**
     * Interface to indicate an asynchronous service.
     */
    public static interface Asynchronous extends Service {
    }
}
