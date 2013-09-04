#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.${rootArtifactId}.${module1name};

import java.util.logging.*;
import org.xillium.core.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.data.persistence.*;
import ${package}.${rootArtifactId}.*;
import ${package}.${rootArtifactId}.${module1name}.util.*;


/**
 * Service description.
 */
public class Hello implements Service {
    private static final Logger _logger = Logger.getLogger(Hello.class.getName());

    public static class Request implements DataObject {
        public String name;
    }

    /**
     * Service
     */
    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
        try {
            Request request = dict.collect(new Request(), binder);
            binder.put("greeting", "Hello " + (request.name != null ? request.name : "World"));
        } catch (Exception x) {
            throw new ServiceException(x.getMessage(), x);
        }

        return binder;
    }
}
