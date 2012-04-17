package org.xillium.core;

import org.xillium.data.*;


/**
 * A Service
 */
public interface Service {
    //public interface Bean {
        /**
         * Processes a service request.
         *
         * Services should implement one of the following 3 interfaces.
         * <ul>
         * <li>Service.ReadOnly - Implement this interface to create a read-only service that should be run inside
         *     a read-only transaction (for maximum read integrity)
         * <li>Service.ReadWrite - Implement this interface to create a read-write service
         * <li>Service.NonTransactional - Implement this interface to create a read-only service that requires no
         *     transactions (for better performance)
         * </ul>
         */
        public DataBinder run(DataBinder parameters, ExecutionEnvironment env) throws ServiceException;
    //}

    /**
     * Implement Service.ReadOnly to create a read-only service.
     */
    //public interface ReadOnly extends Bean {
    //}

    /**
     * Implement Service.ReadWrite to create a read-write service.
     */
    //public interface ReadWrite extends Bean {
    //}

    /**
     * Implement Service.NonTransactional to create a non-transactional service.
     */
    //public interface NonTransactional extends Bean {
    //}

    /*!
     * XML specification object
     */
/*
    public Service(String name, String className) {
        this.name = name;
        this.className = className;
    }

    public String name, className;
*/
}
