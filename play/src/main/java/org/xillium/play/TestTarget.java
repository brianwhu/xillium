package org.xillium.play;

/**
 * Subclass must define a public constructor with the following signature
 * <pre>{@code
 *  <init>(TestSuite suite, String[] args, int offset)
 * }</pre>
 */
public interface TestTarget {
    public static interface Request {
        /**
         * Sets a request parameter.
         * 
         * @param key - the key of the parameter, possiblly prefixed with the type, e.g. "bool:deleteAfterwards"
         * @param value - the string representation of the parameter value
         * @return the request object itself
         */
        public Request set(String key, String value) throws Exception;
    }
    
    public static interface Response {
        
    }

    /**
     * Creates a new request object.
     * 
     * @param path - a path that identifies the service to call
     * @return a new request object
     */
    public Request createRequest(String path);

    /**
     * Fires the request to the target and returns the response.
     * 
     * @param request
     * @return the response from the target
     */
    public Response fire(Request request) throws TestFailureException;
}
