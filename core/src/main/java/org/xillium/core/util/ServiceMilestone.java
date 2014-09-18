package org.xillium.core.util;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.xillium.base.beans.Beans;
import org.xillium.data.DataBinder;
import org.xillium.data.validation.Dictionary;
import org.xillium.core.Persistence;
import org.xillium.core.Service;


/**
 * Service milestones and milestone evaluations management.
 * <p/>
 * A <i>milestone</i> is a point in a service call, where <i>milestone evaluation</i> objects can be called upon to evaluate the current
 * state of the service call and thus alter and extend the behavior of the service.
 * <p/>
 * To define milestones and allow milestone evaluations in a service, do the following inside the service class.
 * <ol>
 * <li>Define an enum type named "Milestone" to enumerate all milestones in this service
 * <xmp>
 *      public static enum Milestone {
 *          M1,
 *          M2,
 *          M3,
 *          M4
 *      }
 * </xmp></li>
 * <li> Define a <i>single</i> <code>ServiceMilestone&lt;Milestone&gt;</code> instance, which may be given any name.
 * <xmp>
 *      private final ServiceMilestone<Milestone> support = new ServiceMilestone<Milestone>(Milestone.class);
 * </xmp></li>
 * <li>Wherever you want to introduce milestone evaluation inside the service's <code>run()</code> method, call <code>support.evaluate</code>.
 * The return value from a milestone evaluation recommends whether the current service should continue or return immediately, but the caller
 * has the discretion to either honor or ignore the recommendations.
 * <xmp>
 *      ...
 *      if (support.evaluate(Milestone.M1, binder, dict, persist)) == ServiceMilestone.Recommendation.COMPLETE) return binder;
 *      ...
 * </xmp></li>
 * </ol>
 * To define a service milestone evaluation, implement the ServiceMilestone.Evaluation interface.
 * <xmp>
 *  public class MilestoneEvaluationM1 implements ServiceMilestone.Evaluation {
 *      ...
 *  }
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
        public <M extends Enum<M>> Recommendation evaluate(Class<M> type, String name, DataBinder binder, Dictionary dict, Persistence persist);
    }

    /**
     * Installs a ServiceMilestone.Evaluation onto a service.
     */
    @SuppressWarnings("unchecked")
    public static <M extends Enum<M>> void install(Service service, String milestore, ServiceMilestone.Evaluation evaluation) throws Exception {
        Class<M> type = getMilestoneClass(service.getClass());
        for (Field field: Beans.getKnownInstanceFields(service.getClass())) {
            if (ServiceMilestone.class.isAssignableFrom(field.getType())) {
                ((ServiceMilestone<M>)field.get(service)).install(Enum.valueOf(type, milestore), evaluation);
                return;
            }
        }
    }

    /**
     * Uninstalls a ServiceMilestone.Evaluation from a service.
     */
    @SuppressWarnings("unchecked")
    public static <M extends Enum<M>> void uninstall(Service service, String milestore, ServiceMilestone.Evaluation evaluation) throws Exception {
        Class<M> type = getMilestoneClass(service.getClass());
        for (Field field: Beans.getKnownInstanceFields(service.getClass())) {
            if (ServiceMilestone.class.isAssignableFrom(field.getType())) {
                ((ServiceMilestone<M>)field.get(service)).uninstall(Enum.valueOf(type, milestore), evaluation);
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
    @SuppressWarnings("unchecked")
    public Recommendation evaluate(M milestone, DataBinder binder, Dictionary dict, Persistence persist) {
        Evaluation evaluation = _evaluations[milestone.ordinal()];
        return evaluation != null ? evaluation.evaluate((Class<M>)milestone.getClass(), milestone.toString(), binder, dict, persist) : Recommendation.CONTINUE;
    }

    private final Evaluation[] _evaluations;

    @SuppressWarnings("unchecked")
    private static <M extends Enum<M>> Class<M> getMilestoneClass(Class<?> declaring) throws Exception {
        while (declaring != Object.class) {
            for (Class<?> declared: declaring.getDeclaredClasses()) {
                if (Modifier.isPublic(declared.getModifiers()) && declared.getSimpleName().equals("Milestone") && Enum.class.isAssignableFrom(declared)) {
                    return (Class<M>)declared;
                }
            }
            declaring = declaring.getSuperclass();
        }
        throw new ClassNotFoundException("Milestone");
    }

}
