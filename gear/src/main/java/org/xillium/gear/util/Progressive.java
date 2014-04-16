package org.xillium.gear.util;

//import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.core.Persistent;
import org.xillium.core.Persistence;
//import org.xillium.core.management.*;
//import org.xillium.core.management.ManagedComponent;
import org.xillium.core.util.*;
import org.springframework.transaction.annotation.Transactional;
//import com.yizheng.yep.*;
//import com.yizheng.yep.util.DatabaseUpdate;
//import com.yizheng.yep.util.LRUCache;
//import com.yizheng.yep.util.PairingOutcome;
//import com.yizheng.yep.settings.PlatformSettings;


/**
 * A facility that supports stepwise, progressive processes.
 *
 *  <xmp>
 *  public enum AccountingStep {
 *      STEP0,
 *      STEP1,
 *      STEP2
 *  }
 *
 *  private final Progressive _progressive; // injected by Spring
 *
 *  Progressive.State state = new Progressive.State&lt;AccountingStep&gt;("accounting");
 *  ...
 *
 *  new VitalTask&lt;SomeManagedComponentClass&gt;(managedComponent, _progressive) {
 *      protected void execute() throws Exception {
 *          _progressive.doStateful(state, AccountingStep.STEP0, null, new Persistent.Task&lt;Void, Void&gt;() {
 *              public Void run(Void facility, Persistence persistence) throws Exception {
 *                  ...
 *                  return null;
 *              }
 *          });
 *
 *          _progressive.doStateful(state, AccountingStep.STEP1, null, new Persistent.Task&lt;Void, Void&gt;() {
 *              public Void run(Void facility, Persistence persistence) throws Exception {
 *                  ...
 *                  return null;
 *              }
 *          });
 *
 *          _progressive.doStateful(state, AccountingStep.STEP2, null, new Persistent.Task&lt;Void, Void&gt;() {
 *              public Void run(Void facility, Persistence persistence) throws Exception {
 *                  ...
 *                  return null;
 *              }
 *          });
 *      }
 *  }.runAsInterruptible();
 *
 *  </xmp>
 *
 * An implementation of this interface will be wired through Spring and transaction enabled.
 */
public class Progressive {
    private static final Logger _logger = Logger.getLogger(Progressive.class.getName());
    private final Persistence _persistence;
    private final String _qRecallState;
    private final String _qRecallParam;
    private final String _uCommitState;
    private final String _uRecordAttempt;

    public static class State<T extends Enum<T>> implements DataObject, TrialStrategy {
        private static final long PAUSE_BETWEEN_RETRIES = 5000L;

        private final Persistence _persistence;
        private final String _qRecallParam;

        public final String moduleId;
        public T state;
        public String previous; // to allow unrecognizable value in the database
        public int step;
        public String param;

        public int basis;
        public int progress;

        public State(Progressive progressive, String module) {
            _persistence = progressive._persistence;
            _qRecallParam = progressive._qRecallParam;
            moduleId = module;
        }

        public State() {
            _persistence = null;
            _qRecallParam = null;
            moduleId = null;
        }

        public void markProgressBasis(T current) {
            if (current != null) basis = current.ordinal();
        }

        public void assessProgress(T current) {
            progress = (int)Math.round((current.ordinal() + 1 - basis) * 100.0 / (current.getDeclaringClass().getEnumConstants().length - basis));
        }

        @Override
        public final void observe(int age) throws InterruptedException {
            while (_persistence.doReadOnly(null, new Persistent.Task<String, Void>() {
                public String run(Void facility, Persistence persistence) throws Exception {
                    return persistence.executeSelect(_qRecallParam, State.this, Persistence.StringRetriever);
                }
            }) != null) {
                Thread.sleep(PAUSE_BETWEEN_RETRIES);
            }
        }

        @Override
        public final void backoff(int age) {
/*
            _persistence.doReadWrite(new Persistent.Task<Void, Void>() {
                public Void run(Void facility, Persistence persistence) throws Exception {
                    persistence.executeUpdate(_uRecordParam, this);
                    return null;
                }
            });
*/
        }
    }


    /**
     * @param Persistence - a Persistence object
     * @param qRecallState - an ObjectMappedQuery that returns a row from the "PersistentState" table
     * @param qRecallState - an ParametricQuery that returns the "param" column from the "PersistentState" table
     * @param uCommitState - an ParametricState that commits state change into the the "PersistentState" table
     * @param uRecordAttempt - an ParametricState that commits state change into the the "PersistentState" table
     */
    public Progressive(Persistence persistence, String qRecallState, String qRecallParam, String uCommitState, String uRecordAttempt) {
        _persistence = persistence;
        _qRecallState = qRecallState;
        _qRecallParam = qRecallParam;
        _uCommitState = uCommitState;
        _uRecordAttempt = uRecordAttempt;
    }

    @Transactional
    public <E extends Enum<E>, T, F> T doStateful(State<E> state, E current, F facility, Persistent.Task<T, F> task) {
        return doStateful(state, current, 0, facility, task);
    }

    @Transactional
    public <E extends Enum<E>, T, F> T doStateful(State<E> state, E current, int step, F facility, Persistent.Task<T, F> task) {
        _logger.info(".param = " + state.param);
        try {
            try {
                State<E> last = _persistence.getObject(_qRecallState, state);
                _logger.fine("Module " + state.moduleId + " last state = " + last);
                if (last != null && last.previous != null) {
                    @SuppressWarnings("unchecked") int previous = Enum.valueOf(current.getClass(), last.previous).ordinal();
                    if (previous > current.ordinal() || (previous == current.ordinal() && last.step >= step)) {
                        _logger.info("Module " + state.moduleId + " fast-forwarding beyond state " + current + '/' + step);
                        return null;
                    }
                }
            } catch (Exception x) {
                throw new RuntimeException(x.getMessage(), x);
            }
            state.param = null;
            state.assessProgress(current);
            logAttempt(state, current, step);
            _logger.info("Module " + state.moduleId + " to " + current);
            T result = task.run(facility, _persistence);
            //try {
                state.state = current;
                state.step = step;
                _persistence.executeUpdate(_uCommitState, state);
                _logger.info("Module " + state.moduleId + " .. " + current);
            //} catch (Exception x) {
                //throw new RuntimeException(x.getMessage(), x);
            //}
            return result;
        } catch (Exception x) {
            String message = Throwables.getRootCause(x).getMessage();
            state.param = message != null ? "+ " + Strings.substringBefore(message, '\n') : "+";
            logAttempt(state, current, step);
            throw (x instanceof RuntimeException) ? (RuntimeException)x : new RuntimeException(x.getMessage(), x);
        }
    }

    // This method depends on AUTONOMOUS_TRANSACTION
    @Transactional
    public <E extends Enum<E>> void logAttempt(State<E> state, E current) {
        logAttempt(state, current, 0);
    }

    // This method depends on AUTONOMOUS_TRANSACTION
    @Transactional
    public <E extends Enum<E>> void logAttempt(State<E> state, E current, int step) {
        E lstate = state.state;
        int lstep = state.step;
        try {
            state.state = current;
            state.step = step;
            _persistence.executeUpdate(_uRecordAttempt, state);
        } catch (Exception x) {
            _logger.log(Level.WARNING, "failure in logging stateful attempt for module " + state.moduleId, x);
        } finally {
            state.state = lstate;
            state.step = lstep;
        }
    }
}

