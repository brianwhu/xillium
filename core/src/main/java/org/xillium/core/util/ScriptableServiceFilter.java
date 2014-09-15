package org.xillium.core.util;

import java.util.Map;
import javax.script.*;

import org.xillium.base.beans.Throwables;
import org.xillium.data.DataBinder;
import org.xillium.core.Service;
import org.xillium.core.ServiceException;


/**
 * A Service.Filter that allows its filter methods to be implemented in JavaScript.
 */
public class ScriptableServiceFilter extends Scriptable implements Service.Filter {
    private String _filtrate, _acknowledge, _successful, _aborted, _complete;

    /**
     * Provides a script for the "filtrate" method.
     */
    public void setFiltrate(String script) {
        _filtrate = script;
    }

    /**
     * Provides a script for the "acknowledge" method.
     */
    public void setAcknowledge(String script) {
        _acknowledge = script;
    }

    /**
     * Provides a script for the "successful" method.
     */
    public void setSuccessful(String script) {
        _successful = script;
    }

    /**
     * Provides a script for the "aborted" method.
     */
    public void setAborted(String script) {
        _aborted = script;
    }

    /**
     * Provides a script for the "complete" method.
     */
    public void setComplete(String script) {
        _complete = script;
    }

    @Override
    public void filtrate(DataBinder parameters) throws ServiceException {
        if (_filtrate != null) {
            call(parameters, _filtrate);
        }
    }

    @Override
    public void acknowledge(DataBinder parameters) throws Exception {
        if (_acknowledge != null) {
            call(parameters, _acknowledge);
        }
    }

    @Override
    public void successful(DataBinder parameters) throws Exception {
        if (_successful != null) {
            call(parameters, _successful);
        }
    }

    @Override
    public void aborted(DataBinder parameters, Throwable throwable) throws Exception {
        if (_aborted != null) {
            js.put("throwable", throwable);
            call(parameters, _aborted);
        }
    }

    @Override
    public void complete(DataBinder parameters) throws Exception {
        if (_complete != null) {
            call(parameters, _complete);
        }
    }

    @Override
    public String toString() {
        return "ScriptableServiceFilter\n"
            + "\t   filtrate: " + _filtrate + '\n'
            + "\tacknowledge: " + _acknowledge + '\n'
            + "\t successful: " + _successful + '\n'
            + "\t    aborted: " + _aborted + '\n'
            + "\t   complete: " + _complete;
    }

    private void call(DataBinder parameters, String script) {
        try {
            js.put("binder", parameters);
            js.eval(script);
        } catch (Throwable t) {
            String message = getScriptingExceptionMessage(t);
            if (parameters.get("Service.SERVICE_STACK_TRACE") != null) parameters.put("_script_exception_", message);
            throw new ServiceException(message);
        }
    }

}
