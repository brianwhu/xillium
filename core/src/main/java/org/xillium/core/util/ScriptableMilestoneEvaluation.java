package org.xillium.core.util;

import java.util.Map;
import javax.script.*;

import org.xillium.base.beans.Throwables;
import org.xillium.data.DataBinder;
import org.xillium.data.validation.Reifier;
import org.xillium.core.Persistence;
import org.xillium.core.ServiceException;


/**
 * A ServiceMilestone.Evaluation that can be implemented in JavaScript.
 */
public class ScriptableMilestoneEvaluation extends Scriptable implements ServiceMilestone.Evaluation {
    private String _script;

    /**
     * Constructs a ScriptableMilestoneEvaluation.
     */
    public ScriptableMilestoneEvaluation() {
    }

    /**
     * Constructs a ScriptableMilestoneEvaluation with then given script.
     */
    public ScriptableMilestoneEvaluation(String script) {
        _script = script;
    }

    /**
     * Provides a script for the "filtrate" method.
     */
    public void setScript(String script) {
        _script = script;
    }

    /**
     * Calls the evaluations in order, returning immediately if one returns ServiceMilestone.Recommendation.COMPLETE.
     */
    @Override
    public <M extends Enum<M>> ServiceMilestone.Recommendation evaluate(Class<M> type, String name, DataBinder binder, Reifier dict, Persistence persist) {
        if (_script != null) {
            try {
                js.put("type", type);
                js.put("name", name);
                js.put("binder", binder);
                js.put("dictionary", dict);
                js.put("persistence", persist);
                Object value = js.eval(_script);
        return value != null ? Enum.valueOf(ServiceMilestone.Recommendation.class, value.toString().toUpperCase()) : ServiceMilestone.Recommendation.CONTINUE;
            } catch (Throwable t) {
                binder.put("_script_", _script);
                throw new ServiceException(getScriptingExceptionMessage(t));
            }
        } else {
            return ServiceMilestone.Recommendation.CONTINUE;
        }
    }

    @Override
    public String toString() {
        return "ScriptableMilestoneEvaluation\n\t" + _script;
    }

}
