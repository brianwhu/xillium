package org.xillium.core;

import org.xillium.data.*;
import org.xillium.data.validation.Dictionary;


/**
 * A Service interface. Transactions should be managed using Spring's @org.springframework.transaction.annotation.Transactional
 * annotation on the implementation class or method.
 */
public interface Service {
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
