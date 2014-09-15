package org.xillium.core.util;

import org.xillium.base.util.Pair;
import org.xillium.data.DataBinder;
import org.xillium.data.validation.Dictionary;
import org.xillium.core.Persistence;


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
    public ServiceMilestone.Recommendation evaluate(Class<? extends Enum> t, String milestone, DataBinder binder, Dictionary dict, Persistence persist) {
        if (first.evaluate(t, milestone, binder, dict, persist) == ServiceMilestone.Recommendation.CONTINUE) {
            return second.evaluate(t, milestone, binder, dict, persist);
        } else {
            return ServiceMilestone.Recommendation.COMPLETE;
        }
    }

}
