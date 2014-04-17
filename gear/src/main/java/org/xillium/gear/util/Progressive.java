package org.xillium.gear.util;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.core.Persistent;
import org.xillium.core.Persistence;
import org.xillium.core.util.*;
import org.springframework.transaction.annotation.Transactional;


/**
 * <p>Progressive is facility that supports stepwise, progressive processes. To use it, follow these steps.</p>
 * <ol>
 *  <li><p>Create a database table with the following columns:</p>
 *      <xmp>
 *      MODULE_ID VARCHAR2 (32 CHAR) NOT NULL
 *      STATE     VARCHAR2 (40 CHAR)
 *      PREVIOUS  VARCHAR2 (40 CHAR)
 *      PARAM     VARCHAR2 (1024 CHAR)
 *      STEP      NUMBER (5)
 *      </xmp>
 *  </li>
 *  <li><p>Define parametric statements that read and write state information in the database, and pass these statement
 *      names to the constructor of Progressive. Read the constructor description for more details. The following gives
        example statements on Oracle database.</p>
        <xmp>
    <persist:object-mapped-query class="org.xillium.gear.util.Progressive.State">
    <?assemble name="RecallState"?>
        <![CDATA[
        SELECT PREVIOUS, STEP FROM MARKET_STATE WHERE MODULE_ID = :moduleId:VARCHAR
        ]]>
    </persist:object-mapped-query>

    <persist:parametric-query>
    <?assemble name="RecallParam"?>
        <![CDATA[
        SELECT PARAM FROM MARKET_STATE WHERE MODULE_ID = :moduleId:VARCHAR
        ]]>
    </persist:parametric-query>

    <persist:parametric-statement>
    <?assemble name="CommitState"?>
        <![CDATA[
        MERGE INTO MARKET_STATE USING DUAL ON (MODULE_ID = :moduleId:VARCHAR)
        WHEN MATCHED THEN
            UPDATE SET STATE = NULL, PREVIOUS = :state:VARCHAR, STEP = :step:INTEGER, PARAM = SUBSTR(:param:VARCHAR, 1, 1024)
        WHEN NOT MATCHED THEN
            INSERT (MODULE_ID, PREVIOUS, STEP, PARAM) VALUES (:moduleId:VARCHAR, :state:VARCHAR, :step:INTEGER, SUBSTR(:param:VARCHAR, 1, 1024))
        ]]>
    </persist:parametric-statement>

    <persist:parametric-statement>
    <?assemble name="RecordAttempt"?>
        <![CDATA[
        DECLARE
            PRAGMA AUTONOMOUS_TRANSACTION;
        BEGIN
            MERGE INTO MARKET_STATE USING DUAL ON (MODULE_ID = :moduleId:VARCHAR)
            WHEN MATCHED THEN
                UPDATE SET STATE = :state:VARCHAR||'/'||:step:INTEGER, PARAM = SUBSTR(:param:VARCHAR, 1, 1024)
            WHEN NOT MATCHED THEN
                INSERT (MODULE_ID, STATE, PARAM) VALUES (:moduleId:VARCHAR, :state:VARCHAR||'/'||:step:INTEGER, SUBSTR(:param:VARCHAR, 1, 1024));
            COMMIT;
        END;
        ]]>
    </persist:parametric-statement>
        </xmp>
    </li>
 *  <li><p>Define a "progressive" bean in a Spring context:</p>
 *      <xmp>
 *  <bean id="progressive" class="org.xillium.gear.util.Progressive">
 *      <constructor-arg index="0"><ref bean="persistence"/></constructor-arg>
 *      <constructor-arg index="1"><value>module1/RecallState</value></constructor-arg>
 *      <constructor-arg index="2"><value>module1/RecallParam</value></constructor-arg>
 *      <constructor-arg index="3"><value>module1/CommitState</value></constructor-arg>
 *      <constructor-arg index="4"><value>module1/RecordAttempt</value></constructor-arg>
 *  </bean>
 *      </xmp>
 *  </li>
 * </ol>
 * Java code example:
 * <xmp>
 *  // the steps to go through, defined as values in an enum type.
 *  public enum AccountingStep {
 *      STEP1,
 *      STEP2,
 *      STEP3
 *  }
 *
 *  // a Progressive property, injected by Spring
 *  private final Progressive _progressive;
 *
 *  // a Progressive.State object, used to keep track of the current process state
 *  Progressive.State state = new Progressive.State<AccountingStep>("accounting");
 *  ...
 *
 *  // the process logic inside a VitalTask, which depends on an instance of ManagedComponent.
 *  new VitalTask<SomeManagedComponentClass>(managedComponent, _progressive) {
 *      protected void execute() throws Exception {
 *
 *          // do work in STEP1
 *          _progressive.doStateful(state, AccountingStep.STEP1, null, new Persistent.Task<Void, Void>() {
 *              public Void run(Void facility, Persistence persistence) throws Exception {
 *                  ...
 *                  return null;
 *              }
 *          });
 *
 *          // do work in STEP2
 *          _progressive.doStateful(state, AccountingStep.STEP2, null, new Persistent.Task<Void, Void>() {
 *              public Void run(Void facility, Persistence persistence) throws Exception {
 *                  ...
 *                  return null;
 *              }
 *          });
 *
 *          // do work in STEP3
 *          _progressive.doStateful(state, AccountingStep.STEP3, null, new Persistent.Task<Void, Void>() {
 *              public Void run(Void facility, Persistence persistence) throws Exception {
 *                  ...
 *                  return null;
 *              }
 *          });
 *      }
 *  }.runAsInterruptible();
 * </xmp>
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

        /**
         * Constructs a State object that is associated with a Progressive object and a unique ID that identifies the process.
         */
        public State(Progressive progressive, String id) {
            _persistence = progressive._persistence;
            _qRecallParam = progressive._qRecallParam;
            moduleId = id;
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
     * Constructs a Progressive object.
     *
     * @param persistence - a Persistence object
     * @param qRecallState - an ObjectMappedQuery that returns a row from the "PersistentState" table
     * @param qRecallParam - an ParametricQuery that returns the "param" column as a string from the "PersistentState" table
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

    /**
     * Performs a task that is associated with a state. The task is only executed if the state has not been reached previously.
     *
     * @param state - a State object that keeps track of the progress of the process
     * @param current - the state associated with the task
     * @param facility - an optional object that might provides environmental support to the task. This parameter is passed directly
     *        to the task.
     * @param task - the Persistent.Task to execute
     */
    @Transactional
    public <E extends Enum<E>, T, F> T doStateful(State<E> state, E current, F facility, Persistent.Task<T, F> task) {
        return doStateful(state, current, 0, facility, task);
    }

    /**
     * Performs a task that is associated with a state and a step within the state. The task is only executed if the state has not been reached previously,
     * or the step has not been reached previously.
     *
     * @param state - a State object that keeps track of the progress of the process
     * @param current - the state associated with the task
     * @param step - a step within the state
     * @param facility - an optional object that might provides environmental support to the task. This parameter is passed directly
     *        to the task.
     * @param task - the Persistent.Task to execute
     */
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

