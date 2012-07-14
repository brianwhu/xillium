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
    public static final String REQUEST_HTTP_METHOD = "#http_method#";
    public static final String SERVICE_POST_ACTION = "#post_action#";
    public static final String SERVICE_JSON_TUNNEL = "_json_tunnel_";

    /**
     * A failure message is a string suitable for display to an end user. This value is only present when the service fails.
     */
    public static final String FAILURE_MESSAGE = "_message_";
    public static final String FAILURE_STACK   = "_stack_";

	/**
	 * Processes a service request. This method runs in a thread parallel to other service calls, and must return as quickly as possible.
	 *
	 * @param parameters - request parameters in a DataBinder
	 * @param env - an ExecutionEnvironment
	 * @return response data in a DataBinder
	 * @throws ServiceException if the service fails for any reason. If the service is wrapped inside a transaction, the transaction
	 *         is rolled back.
	 */
	public DataBinder run(DataBinder parameters, Dictionary dict, Persistence persist) throws ServiceException;

	/**
	 * Marker interface to indicate a secured service.
	 */
	public static interface Secured extends Service {
		public void authorize(String deployment, DataBinder parameters, Persistence persist) throws AuthorizationException;
	}
}
