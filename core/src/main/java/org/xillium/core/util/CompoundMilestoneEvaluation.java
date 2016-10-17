package org.xillium.core.util;

import org.xillium.base.util.Pair;
import org.xillium.data.DataBinder;
import org.xillium.data.persistence.Persistence;
import org.xillium.data.validation.Reifier;


/**
 * A compound service milestone evaluation that binds together a pair of evaluations.
 */
public class CompoundMilestoneEvaluation extends Pair<ServiceMilestone.Evaluation, ServiceMilestone.Evaluation> implements ServiceMilestone.Evaluation {

    /**
     * Constructs a CompoundMilestoneEvaluation.
     */
    public CompoundMilestoneEvaluation(ServiceMilestone.Evaluation a, ServiceMilestone.Evaluation b) {
        super(a, b);
    }

    /**
     * Calls the evaluations in order, returning immediately if one returns ServiceMilestone.Recommendation.COMPLETE.
     */
    @Override
    public <M extends Enum<M>> ServiceMilestone.Recommendation evaluate(Class<M> type, String name, DataBinder binder, Reifier dict, Persistence persist) {
        if (first.evaluate(type, name, binder, dict, persist) == ServiceMilestone.Recommendation.CONTINUE) {
            return second.evaluate(type, name, binder, dict, persist);
        } else {
            return ServiceMilestone.Recommendation.COMPLETE;
        }
    }

}
