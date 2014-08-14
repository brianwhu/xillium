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
public class ScriptableServiceFilter implements Service.Filter {
    private static final String JAVASCRIPT_LINE_INFO = " *\\([^()]+\\)$";

    private static final ScriptEngine js = new ScriptEngineManager().getEngineByName("JavaScript");
    private String _filtrate, _acknowledge, _successful, _aborted, _complete;

    /**
     * Provides a "system" object to the JavaScript engine.
     */
    public void setSystem(Object system) {
        js.put("system", system);
    }

    /**
     * Provides objects to the JavaScript engine under various names.
     */
    public void setObjects(Map<String, Object> objects) {
        for (Map.Entry<String, Object> entry: objects.entrySet()) {
            js.put(entry.getKey(), entry.getValue());
        }
    }

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
            try {
                js.put("binder", parameters);
                js.eval(_filtrate);
            } catch (Throwable t) {
                t = Throwables.getRootCause(t);
                String s = t.getMessage();
                throw new ServiceException(s != null ? s.replaceAll(JAVASCRIPT_LINE_INFO, "") : "***UnknownError", t);
            }
        }
    }

    @Override
    public void acknowledge(DataBinder parameters) throws Exception {
        if (_acknowledge != null) {
            js.put("binder", parameters);
            js.eval(_acknowledge);
        }
    }

    @Override
    public void successful(DataBinder parameters) throws Exception {
        if (_successful != null) {
            js.put("binder", parameters);
            js.eval(_successful);
        }
    }

    @Override
    public void aborted(DataBinder parameters, Throwable throwable) throws Exception {
        if (_aborted != null) {
            js.put("binder", parameters);
            js.put("throwable", throwable);
            js.eval(_aborted);
        }
    }

    @Override
    public void complete(DataBinder parameters) throws Exception {
        if (_complete != null) {
            js.put("binder", parameters);
            js.eval(_complete);
        }
    }
}
