package org.xillium.core.util;

import java.util.Map;
import javax.script.*;

import org.xillium.base.beans.Throwables;


/**
 * A scriptable object that can be assmbled in a Spring application context.
 */
public abstract class Scriptable {
    private static final String JAVASCRIPT_LINE_INFO = " *\\([^()]+\\)$";

    protected final ScriptEngine js = new ScriptEngineManager().getEngineByName("JavaScript");

    protected String getScriptingExceptionMessage(Throwable t) {
        String s = Throwables.getRootCause(t).getMessage();
        return s != null ? s.replaceAll(JAVASCRIPT_LINE_INFO, "") : "***UnknownError";
    }

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

}
