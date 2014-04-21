package org.xillium.gear.util;

import org.xillium.data.DataObject;
import org.xillium.core.Persistence;


/**
 * <p>Progressive is facility that supports stepwise, progressive processes. To use it, follow these steps.</p>
 * <ol>
 *  <li><p>Create a "PROGRESSIVE_STATES" database table with the following columns, of which MODULE_ID column is the primary key:</p>
 *      <xmp>
 *      MODULE_ID VARCHAR2 (32 CHAR) NOT NULL
 *      STATE     VARCHAR2 (40 CHAR)
 *      PREVIOUS  VARCHAR2 (40 CHAR)
 *      PARAM     VARCHAR2 (1024 CHAR)
 *      STEP      NUMBER (5)
 *      </xmp>
 *  </li>
 *  <li><p>Define parametric statements that read and write state information in the database, and pass these statement
 *      names to the constructor of a Progressive implementation. Read the constructor description for more details. The following gives
 *      an example implementation on Oracle database.</p>
 *      <xmp>
 *  <persist:object-mapped-query class="org.xillium.gear.util.Progressive$State">
 *  <?assemble name="RecallState"?>
 *      <![CDATA[
 *      SELECT PREVIOUS, STEP FROM PROGRESSIVE_STATES WHERE MODULE_ID = :moduleId:VARCHAR
 *      ]]>
 *  </persist:object-mapped-query>
 *
 *  <persist:parametric-query>
 *  <?assemble name="RecallParam"?>
 *      <![CDATA[
 *      SELECT PARAM FROM PROGRESSIVE_STATES WHERE MODULE_ID = :moduleId:VARCHAR
 *      ]]>
 *  </persist:parametric-query>
 *
 *  <persist:parametric-statement>
 *  <?assemble name="CommitState"?>
 *      <![CDATA[
 *      MERGE INTO PROGRESSIVE_STATES USING DUAL ON (MODULE_ID = :moduleId:VARCHAR)
 *      WHEN MATCHED THEN
 *          UPDATE SET STATE = NULL, PREVIOUS = :state:VARCHAR, STEP = :step:INTEGER, PARAM = SUBSTR(:param:VARCHAR, 1, 1024)
 *      WHEN NOT MATCHED THEN
 *          INSERT (MODULE_ID, PREVIOUS, STEP, PARAM) VALUES (:moduleId:VARCHAR, :state:VARCHAR, :step:INTEGER, SUBSTR(:param:VARCHAR, 1, 1024))
 *      ]]>
 *  </persist:parametric-statement>
 *
 *  <persist:parametric-statement>
 *  <?assemble name="MarkAttempt"?>
 *      <![CDATA[
 *      DECLARE
 *          PRAGMA AUTONOMOUS_TRANSACTION;
 *      BEGIN
 *          MERGE INTO PROGRESSIVE_STATES USING DUAL ON (MODULE_ID = :moduleId:VARCHAR)
 *          WHEN MATCHED THEN
 *              UPDATE SET STATE = :state:VARCHAR||'/'||:step:INTEGER, PARAM = SUBSTR(:param:VARCHAR, 1, 1024)
 *          WHEN NOT MATCHED THEN
 *              INSERT (MODULE_ID, STATE, PARAM) VALUES (:moduleId:VARCHAR, :state:VARCHAR||'/'||:step:INTEGER, SUBSTR(:param:VARCHAR, 1, 1024));
 *          COMMIT;
 *      END;
 *      ]]>
 *  </persist:parametric-statement>
 *      </xmp>
 *  </li>
 *  <li><p>Define a "progressive" bean in a Spring context, using the implementation class of Progressive, Progression:</p>
 *      <xmp>
 *  <bean id="progressive" class="org.xillium.gear.util.Progression">
 *      <constructor-arg index="0"><ref bean="persistence"/></constructor-arg>
 *      <constructor-arg index="1"><value>module1/RecallState</value></constructor-arg>
 *      <constructor-arg index="2"><value>module1/RecallParam</value></constructor-arg>
 *      <constructor-arg index="3"><value>module1/CommitState</value></constructor-arg>
 *      <constructor-arg index="4"><value>module1/MarkAttempt</value></constructor-arg>
 *  </bean>
 *      </xmp>
 *  </li>
 * </ol>
 * Java code example:
 * <xmp>
 *  // the steps to go through, defined as values in an enum type.
 *  public enum OperationState {
 *      STATE1,
 *      STATE2,
 *      STATE3
 *  }
 *
 *  // a Progressive property, injected by Spring
 *  private final Progressive _progressive;
 *
 *  // a Progressive.State object, used to keep track of the current process state
 *  Progressive.State<OperationState> state = new Progressive.State<OperationState>(_progressive, "accounting");
 *  ...
 *
 *  // the process logic inside a VitalTask, which depends on an instance of Reporting for exception reporting.
 *  new VitalTask<SomeClassImplementingReporting>(someClassImplementingReporting, state) {
 *      protected void execute() throws Exception {
 *
 *          // the first argument passed to VitalTask.<init>, someClassImplementingReporting, is available
 *          // here as the return value of getReporting(), a method of VitalTask.
 *
 *          // do work in STATE1
 *          _progressive.doStateful(state, OperationState.STATE1, null, new Persistence.Task<Void, Void>() {
 *              public Void run(Void facility, Persistence persistence) throws Exception {
 *                  ...
 *                  return null;
 *              }
 *          });
 *
 *          // do work in STATE2
 *          _progressive.doStateful(state, OperationState.STATE2, null, new Persistence.Task<Void, Void>() {
 *              public Void run(Void facility, Persistence persistence) throws Exception {
 *                  ...
 *                  return null;
 *              }
 *          });
 *
 *          // do work in STATE3
 *          _progressive.doStateful(state, OperationState.STATE3, null, new Persistence.Task<Void, Void>() {
 *              public Void run(Void facility, Persistence persistence) throws Exception {
 *                  ...
 *                  return null;
 *              }
 *          });
 *      }
 *  }.runAsInterruptible();
 * </xmp>
 */
public interface Progressive {
    /**
     * A State object is used to keep track of progress in a stepwise process.
     */
    public static class State<E extends Enum<E>> implements DataObject, TrialStrategy {
        private static final long PAUSE_BETWEEN_OBSERVATIONS = 5000L;

        private final Progressive _progressive;

        public final String moduleId;
        public String state;
        public String previous;
        public int step;
        public String param;
        public transient E current;

        //public int basis;
        //public int progress;

        /**
         * Constructs a State object that is associated with a Progressive object and a unique ID that identifies the process.
         */
        public State(Progressive progressive, String id) {
            _progressive = progressive;
            moduleId = id;
        }

        public State() {
            _progressive = null;
            moduleId = null;
        }

/*
        public void markProgressBasis(E current) {
            if (current != null) basis = current.ordinal();
        }

        public void assessProgress(E current) {
            progress = (int)Math.round((current.ordinal() + 1 - basis) * 100.0 / (current.getDeclaringClass().getEnumConstants().length - basis));
        }
*/

        @Override
        public final void observe(int age) throws InterruptedException {
            // wait until "param" is empty
            String param;
            while (true) {
                try {
                    if ((param = _progressive.report(this)) == null || param.trim().length() == 0) break;
                    Thread.sleep(PAUSE_BETWEEN_OBSERVATIONS);
                } catch (InterruptedException x) {
                    throw x;
                } catch (Exception x) {
                    Thread.sleep(PAUSE_BETWEEN_OBSERVATIONS);
                }
            }
        }

        @Override
        public final void backoff(int age) {
            _progressive.markAttempt(this, current, step);
        }
    }

    /**
     * Performs a task that is associated with a state. The task is only executed if the state has not been reached previously.
     *
     * @param state - a State object that keeps track of the progress of the process
     * @param current - the state associated with the task
     * @param facility - an optional object that provides environmental support to the task. This parameter is passed directly to the task.
     * @param task - the Persistence.Task to execute
     */
    public <E extends Enum<E>, T, F> T doStateful(State<E> state, E current, F facility, Persistence.Task<T, F> task);

    /**
     * Performs a task that is associated with a state and a step within the state. The task is only executed if the state has not been
     * reached previously, or the step within the state has not been reached previously.
     *
     * @param state - a State object that keeps track of the progress of the process
     * @param current - the state associated with the task
     * @param step - a step within the state
     * @param facility - an optional object that provides environmental support to the task. This parameter is passed directly to the task.
     * @param task - the Persistence.Task to execute
     */
    public <E extends Enum<E>, T, F> T doStateful(State<E> state, E current, int step, F facility, Persistence.Task<T, F> task);

    /**
     * Reports any error from a previous state progression attempt, stored in the "param" column.
     */
    public <E extends Enum<E>> String report(State<E> state);

    /**
     * Logs an attempt.
     */
    public <E extends Enum<E>> void markAttempt(State<E> state, E current);

    /**
     * Logs an attempt.
     */
    public <E extends Enum<E>> void markAttempt(State<E> state, E current, int step);
}

