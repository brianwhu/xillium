package org.xillium.core.util;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;

import org.xillium.base.beans.Beans;
import org.xillium.data.DataBinder;
import org.xillium.data.validation.Dictionary;
import org.xillium.core.Persistence;
import org.xillium.core.Service;


/**
 * Service milestones and milestone evaluations management.
 * <p/>
 * A milestone is a point in a service call, where disassociated, external objects can be called upon to evaluate the milestone
 * and thus alter and extend the original behavior of the service.
 * <p/>
 * To introduce milestones and allow milestone evaluations to a service, do the following.
 * <xmp>
 *  public class Service101 ... {
 *      // define an enum named "Milestone" to enumerate all milestones in this service
 *      public static enum Milestone {
 *          M1,
 *          M2,
 *          M3,
 *          M4
 *      }
 *
 *      // create a ServiceMilestone instance named "milestone"
 *      private final ServiceMilestone<Milestone> support = new ServiceMilestone<Milestone>(Milestone.class);
 *
 *      ...
 *
 *      public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
 *          ...
 *
 *          if (support.evaluate(Milestone.M1, binder, dict, persist)) == ServiceMilestone.Recommendation.COMPLETE) return binder;
 *
 *          ...
 *      }
 *  }
 * </xmp>
 * To define a service milestone evaluation, implement the ServiceMilestone.Evaluation interface.
 * <xmp>
 *  @registration(service="service/name", milestone="milestoneName")
 *  public class MilestoneEvaluationM1 implements ServiceMilestone.Evaluation {
 *      ...
 *  }
 * </xmp>
 * And declare an evaluation instance
 * <xmp>
 *  ...
 *  <bean class="my.company.package.MilestoneEvaluationM1">
 *  </bean>
 *  ...
 * </xmp>
 */
public class ServiceMilestone<M extends Enum<M>> {

    /**
     * Recommendations returned from a milestone evaluation
     */
    public static enum Recommendation {
        CONTINUE,
        COMPLETE
    }

    /**
     * A milestone evaluation
     */
    public static interface Evaluation {
        /**
         * Evaluates a milestone as identified by the milestone enumeration's type and the milestone's name.
         */
        public Recommendation evaluate(Class<? extends Enum> type, String name, DataBinder binder, Dictionary dict, Persistence persist);
    }

    /**
     * Installs a ServiceMilestone.Evaluation onto a service.
     */
    public static void install(Service service, String milestore, ServiceMilestone.Evaluation evaluation) throws Exception {
        Class<? extends Enum> type = (Class<? extends Enum>)Class.forName(service.getClass().getName() + "$Milestone");
        for (Field field: Beans.getKnownInstanceFields(service.getClass())) {
            if (ServiceMilestone.class.isAssignableFrom(field.getType())) {
                ((ServiceMilestone)field.get(service)).install(Enum.valueOf(type, milestore), evaluation);
                return;
            }
        }
    }

    /**
     * Uninstalls a ServiceMilestone.Evaluation from a service.
     */
    public static void uninstall(Service service, String milestore, ServiceMilestone.Evaluation evaluation) throws Exception {
        Class<? extends Enum> type = (Class<? extends Enum>)Class.forName(service.getClass().getName() + "$Milestone");
        for (Field field: Beans.getKnownInstanceFields(service.getClass())) {
            if (ServiceMilestone.class.isAssignableFrom(field.getType())) {
                ((ServiceMilestone)field.get(service)).uninstall(Enum.valueOf(type, milestore), evaluation);
                return;
            }
        }
    }

    /**
     * Constructs a ServiceMilestone.
     */
    public ServiceMilestone(Class<M> type) {
        _evaluations = new Evaluation[type.getEnumConstants().length];
    }

    /**
     * Installs a ServiceMilestone.Evaluation.
     */
    public void install(M m, Evaluation eval) {
        _evaluations[m.ordinal()] = _evaluations[m.ordinal()] == null ? eval : new CompoundMilestoneEvaluation(eval, _evaluations[m.ordinal()]);
    }

    /**
     * Uninstalls a ServiceMilestone.Evaluation.
     */
    public void uninstall(M m, Evaluation eval) {
        _evaluations[m.ordinal()] = CompoundMilestoneEvaluation.cleanse(_evaluations[m.ordinal()], eval);
    }

    /**
     * Executes evaluations at a milestone.
     */
    public Recommendation evaluate(M milestone, DataBinder binder, Dictionary dict, Persistence persist) {
        Evaluation evaluation = _evaluations[milestone.ordinal()];
        return evaluation != null ? evaluation.evaluate(milestone.getClass(), milestone.toString(), binder, dict, persist) : Recommendation.CONTINUE;
    }

    private final Evaluation[] _evaluations;

}
