package org.xillium.core;

import org.xillium.data.*;
import org.xillium.data.validation.Dictionary;


/**
 * A Service interface. Transactions should be managed using Spring's @org.springframework.transaction.annotation.Transactional
 * annotation on the implementation class or method.
 */
public interface Service {
    public static final String REQUEST_CLIENT_ADDR = "#client_addr#";
    public static final String REQUEST_CLIENT_PORT = "#client_port#";
    public static final String REQUEST_SERVER_PORT = "#server_port#";
    public static final String REQUEST_SERVER_PATH = "#server_path#";
    public static final String REQUEST_HTTP_METHOD = "#http_method#";

    public static final String SERVICE_DATA_BINDER = "#data_binder#";
    public static final String SERVICE_POST_ACTION = "#post_action#";
    public static final String SERVICE_PAGE_TARGET = "#page_target#";
    public static final String SERVICE_JSON_TUNNEL = "_json_tunnel_";
    public static final String SERVICE_HTTP_HEADER = "#http_header#";
    public static final String SERVICE_HTTP_STATUS = "#http_status#";
    public static final String SUPPRESS_EX_LOGGING = "#suppress_ex#";

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
	 * @param dict - a Dictionary for data validation/parsing
	 * @param persist - a Persistence for data persistence
	 * @return response data in a DataBinder; This can be the same data binder passed in as the first argument
	 * @throws ServiceException if the service fails for any reason. If the service is wrapped inside a transaction, the transaction
	 *         is rolled back.
	 */
	public DataBinder run(DataBinder parameters, Dictionary dict, Persistence persist) throws ServiceException;

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
         * An extra step that runs before the service transaction starts.
         */
        public void filtrate(DataBinder parameters) throws ServiceException;

        /**
         * An extra step that runs after the service is successful and the associated transaction committed.
         * It will NOT run if the service has failed.
         */
        public void successful(DataBinder parameters);

        /**
         * An extra step that runs after the service has failed and the associated transaction rolled back.
         * It will NOT run if the service is successful.
         */
        public void aborted(DataBinder parameters, Throwable throwable);

        /**
         * An extra step that always runs after the service has been completed, disregard whether the associated transaction is committed or rolled back.
         */
        public void complete(DataBinder parameters);
    }

	/**
	 * Interface to indicate a service that has the same extra steps as defined in the Filter interface.
	 */
	public static interface Extended extends Service, Filter {
	}

    /**
     * Interface to indicate an asynchronous service.
     */
    public static interface Asynchronous extends Service {
    }
}
