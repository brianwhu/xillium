package org.xillium.gear.util;

import java.util.logging.*;
import org.xillium.base.beans.*;
import org.xillium.core.Persistence;
import org.springframework.transaction.annotation.Transactional;


/**
 * Progression is an implementation of Progressive.
 */
public class Progression implements Progressive {
    private static final Logger _logger = Logger.getLogger(Progression.class.getName());
    private final Persistence _persistence;
    private final String _qRecallState;
    private final String _qRecallParam;
    private final String _uCommitState;
    private final String _uMarkAttempt;


    /**
     * Constructs a Progression object.
     *
     * @param persistence - a Persistence object
     * @param qRecallState - an ObjectMappedQuery that returns "previous" and "step" column froms the "PROGRESSIVE_STATES" table as a State object
     * @param qRecallParam - a ParametricQuery that returns the "param" column as a string from the "PROGRESSIVE_STATES" table
     * @param uCommitState - a ParametricState that commits a completed state into the the "PROGRESSIVE_STATES" table
     * @param uMarkAttempt - an optional ParametricState that writes attempted state progression into the the "PROGRESSIVE_STATES" table in a
     *                       sub/autonomous transaction
     */
    public Progression(Persistence persistence, String qRecallState, String qRecallParam, String uCommitState, String uMarkAttempt) {
        _persistence = persistence;
        _qRecallState = qRecallState;
        _qRecallParam = qRecallParam;
        _uCommitState = uCommitState;
        _uMarkAttempt = uMarkAttempt;
    }

    @Override
    @Transactional
    public <E extends Enum<E>, T, F> T doStateful(State state, E current, F facility, Persistence.Task<T, F> task) {
        return doStateful(state, current, 0, facility, task);
    }

    @Override
    @Transactional
    public <E extends Enum<E>, T, F> T doStateful(State state, E current, int step, F facility, Persistence.Task<T, F> task) {
        //_logger.info(".param = " + state.param);
        try {
            try {
                State last = _persistence.getObject(_qRecallState, state);
                _logger.info("Module " + state.moduleId + " at " + last);
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
            state.state = current.toString() + '/' + step;
            state.param = null;
            //state.assessProgress(current);
            markAttempt(state);
            _logger.info("Module " + state.moduleId + " to " + current);

            T result = task.run(facility, _persistence);

            state.previous = current.toString();
            state.step = step;
            _persistence.executeUpdate(_uCommitState, state);
            _logger.info("Module " + state.moduleId + " .. " + current);
            return result;
        } catch (Exception x) {
            String message = Throwables.getRootCause(x).getMessage();
            state.param = message != null ? State.PARAM_PROBLEM + ' ' + Strings.substringBefore(message, '\n') : State.PARAM_PROBLEM;
            // try to mark the attempt including the param - this is only possible with something like autonomous transactions
            //markAttempt(state);
            throw (x instanceof RuntimeException) ? (RuntimeException)x : new RuntimeException(x.getMessage(), x);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public String report(State state) {
        try {
            return _persistence.executeSelect(_qRecallParam, state, Persistence.StringRetriever);
        } catch (Exception x) {
            _logger.log(Level.WARNING, "failure in reading param", x);
            throw new RuntimeException(x.getMessage(), x);
        }
    }

    @Override
    @Transactional
    public void markAttempt(State state) {
        try {
            _persistence.executeUpdate(_uMarkAttempt, state);
        } catch (Exception x) {
            _logger.log(Level.WARNING, "failure in logging attempt: " + state, x);
        }
    }
}

