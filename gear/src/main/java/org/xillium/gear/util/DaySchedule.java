package org.xillium.gear.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.management.*;

import org.xillium.base.util.Objects;
import org.xillium.base.beans.Throwables;
import org.xillium.core.management.*;


/**
 * <p>A DaySchedule is a utility that monitors a given 24-hour schedule of activities, which could start at a moment other than the true midnight.
 * The schedule is reloaded every 24 hours at about 1 second after the "logical midnight".</p>
 *
 * <p>The "logical midnight" can be defined at any given minute within the 24 hours. For example, a day can start at 4:05 am, which
 * corresponds to</p>
 * <xmp>
 *  midnight_hour = 4
 *  midnight_minute = 5
 *  calendar_offset = 0
 * </xmp>
 * <p>If a logical day starts at 8:30 pm the day before, then</p>
 * <xmp>
 *  midnight_hour = 20
 *  midnight_minute = 30
 *  calendar_offset = 1
 * </xmp>
 * <p>Limitations: no activity can be scheduled at about 1 second after "midnight". Also, activities usually start about 1 milliseconds later than
 * scheduled.</p>
 *
 * <p>To use DaySchedule, create a new instance and then call the start method to let it run as a background thread.</p>
 * <xmp>
 *      DaySchedule schedule = new DaySchedule(...);
 *      schedule.start();
 * </xmp>
 * <p>To stop a running DaySchedule, call its shutdown method. This should be done only during web app termination.</p>
 * <xmp>
 *      schedule.shutdown();
 * </xmp>
 */
public class DaySchedule<T extends Enum<T>> extends Thread implements Manageable {
    private static final Logger _logger = Logger.getLogger(DaySchedule.class.getName());
    private static final int MILLISECONDS_PER_MINUTE = 60000;
    private static final int MILLISECONDS_PER_DAY = 24*60*MILLISECONDS_PER_MINUTE;

	/**
	 * A scheduled activity.
	 */
	public static class Activity<T extends Enum<T>> implements Comparable<Activity<T>>, Runnable {
        public static final String DAY_STARTING = "+";
        public static final String DAY_FINISHED = "-";

        /**
         * scheduled time, expressed as milliseconds since the 'logical' midnight
         */
		public final int clock;
		public final T event;
        public final String tag;

        /**
         * Constructs an Activity.
         *
         * To allow 2 side by side activities, all non-ending (non-IDLE) events are to start 1 millisecond later than scheduled.
         *
         * @param schedule - a DaySchedule object
         * @param minutes - number of <i>minutes</i> since the true midnight
         * @param e - the scheduled event
         * @param t - a tag for the event
         */
        public Activity(DaySchedule<T> schedule, int minutes, T e, String t) {
            _schedule = schedule;
            clock = schedule.trueToLogical(minutes*MILLISECONDS_PER_MINUTE) + (e.ordinal() == 0 ? 0 : 1);
            event = e;
            tag = t;
        }

        /*!
         * Constructs a special (DayStarting/DayFinished) Activity.
         *
         * @param schedule - a DaySchedule object
         * @param minutes - number of <i>minutes</i> since the logical midnight
         * @param t - one of the special values DAY_STARTING and DAY_FINISHED
         */
        private Activity(DaySchedule<T> schedule, int minutes, String t) {
            _schedule = schedule;
            clock = minutes*MILLISECONDS_PER_MINUTE;
            event = null;
            tag = t;
        }

        /**
         * Natural order honors identity by (clock, tag)
         */
        @Override
        public int compareTo(Activity<T> activity) {
            int order = this.clock - activity.clock;
            return order != 0 ? order : this.tag.compareTo(activity.tag);
        }

        /**
         * Natural order honors identity by (clock, tag)
         */
        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Activity)) return false;
            return compareTo((Activity<T>)o) == 0;
        }

        @Override
        public int hashCode() {
            return 37*clock + tag.hashCode();
        }

        @Override
        public void run() {
            _active = true;
            try {
                if (event == null) {
                    if (tag.charAt(0) == DAY_STARTING.charAt(0)) {
                        _schedule._user.dayStarting(_schedule._date);
                    } else {
                        _schedule._user.dayFinished(_schedule._date);
                    }
                } else {
                    _schedule._user.performActivity(event, tag, _schedule._date);
                }
            } catch (InterruptedException x) {
                _logger.info("Termination request detected at " + Throwables.getRootCause(x).getStackTrace()[0]);
            } catch (Exception x) {
                _logger.log(Level.WARNING, "Failure in scheduled event", x);
            } finally {
                _active = false;
            }
        }

        @Override
        public String toString() {
            return String.valueOf(event) + '[' + tag + "] @ " + clock;
        }

        private static boolean _active;
        private final DaySchedule<T> _schedule;
	}

	/**
	 * A day schedule user, who is responsible for providing a schedule every 24-hours, and performs scheduled activities.
	 */
	public static interface User<T extends Enum<T>> {
		/**
		 * (Re)loads scheduled activities to be monitored by this DaySchedule.
         *
         * @param date - current logical date
         * @return list of scheduled activities, which is never null but could be empty. The time of events is on the true clock.
		 */
		public List<Activity<T>> loadScheduledActivities(Calendar date);

        /**
         * Signals that a new day is starting.
         */
        public void dayStarting(Calendar date) throws InterruptedException;

        /**
         * Signals that the day has finished.
         */
        public void dayFinished(Calendar date) throws InterruptedException;

		/**
		 * Performs a scheduled activity.
		 */
		public void performActivity(T event, String tag, Calendar date) throws InterruptedException;
	}

    /**
     * Constructs a DaySchedule with a logical midnight.
     *
     * @param user - DaySchedule user
     * @param midnightHour - the hour of the logical midnight
     * @param midnightMinute - the minute of the logical midnight
     * @param calendarOffset - offset in days from true calendar date at logical midnight
     * @param closeBefore - number of minutes before midnight when the dayFinished special event is fired
     * @param reopenAfter - number of minutes after midnight when the dayStarting special event is fired
     */
    public DaySchedule(User<T> user, int midnightHour, int midnightMinute, int calendarOffset, int closeBefore, int reopenAfter) {
        super("DaySchedule-"+user.hashCode());
        _user = user;
        _midnight = (midnightHour*60 + midnightMinute)*MILLISECONDS_PER_MINUTE;
        _offset = calendarOffset;
        _closeBefore = closeBefore;
        _reopenAfter = reopenAfter;
        _worker = Executors.newSingleThreadExecutor();
    }

    /**
     * Adjusts the current logical time with a transition amount in milliseconds.
     */
    public DaySchedule<T> setTimeTransition(long transition) {
        _transition = transition;
        return this;
    }

    /**
     * Returns the current logical time.
     */
    public long getLogicalTime() {
        return _calendar.getTimeInMillis() + logicalClockMillis();
    }

    public int getCloseBeforeInMillis() {
        return logicalToTrue(_close);
    }

    public int getReopenAfterInMillis() {
        return logicalToTrue(_start);
    }

    public int getMillisToDayStart() {
        return _start - trueClockMillis(false);
    }

    public int getMillisToDayClose() {
        return _close - trueClockMillis(false);
    }

    /**
     * Reports component status.
     */
    @Override
    public Status getStatus() {
        return Status.HEALTHY;
    }

    /**
     * Reports component liveliness.
     */
    @Override
    public boolean isActive() {
        return Activity._active;
    }

    /**
     * Assigns an ObjectName to this manageable.
     *
     * @return the same ObjectName passed to this method.
     */
    @Override
    public ObjectName assignObjectName(ObjectName name) {
        if (_name != null) throw new IllegalStateException("ObjectNameAlreadyAssigned");
        return _name = name;
    }

    public ObjectName getObjectName() {
        return _name;
    }

    @Override
    public String getProperty(String name) throws AttributeNotFoundException {
        return String.valueOf(Objects.getProperty(this, name));
    }

    @Override
    public void setProperty(String name, String value) throws AttributeNotFoundException, BadAttributeValueExpException {
        Objects.setProperty(this, name, value);
    }

    @Override
    public void run() {
        try {
            _logger.info("started");

            // Step 1. read clock and calibrate date
            int clock = trueToLogical(trueClockMillis(true));

            // Step 2. Load scheduled activities
            Activity<T>[] activities = reload();
            if (activities.length > 0) {
                _start = activities[0].clock;
                _close = activities[activities.length-1].clock;
            } else {
                _start = _close = 0;
            }
            int pointer = 0;

            // Step 3. Fast-forward to the next activity from now
            while (pointer < activities.length && activities[pointer].clock < clock) {
                ++pointer;
            }

            // Step 4. Catch up, if the next activity is not "dayStarting".
            if (pointer > 0 && pointer < activities.length) {
                // Fire "dayStarting" even if the next activity is "dayFinished".
                _worker.execute(activities[0]);
                if (pointer > 1) { // an activity is supposed to be going on ... for each different tag
                    _logger.info("DaySchedule (re)started amid ...");
                    Set<String> tags = new HashSet<String>();
                    for (int p = pointer - 1; p > 0; --p) {
                        if (!tags.contains(activities[p].tag)) {
                            _logger.info("\t" + activities[p]);
                            _worker.execute(activities[p]);
                            tags.add(activities[p].tag);
                        }
                    }
                }
            }

            while (!isInterrupted()) {
                if (pointer == activities.length) {
                    _logger.info("The day is over - sleep till midnight + 1 full second - just to make sure we are into the next day");
                    sleepTill(MILLISECONDS_PER_DAY + 1000);
                    _logger.info("Waking up into a new day");
                    // calibrate the date after waking up
                    trueClockMillis(true);
                    // reload scheduled activities
                    activities = reload();
                    pointer = 0;
                    // if there's no activities, start from top of the loop
                    if (activities.length == 0) continue;
                }

                _logger.info("Sleeping till ...  [" + pointer + "] = " + activities[pointer]);
                sleepTill(activities[pointer].clock);
                _logger.info("... and wake up on [" + pointer + "] = " + activities[pointer]);
                _worker.execute(activities[pointer]);
                // next activity
                ++pointer;
            }
        } catch (InterruptedException x) {
            _logger.log(Level.WARNING, "Interrupted", x);
        } catch (RuntimeException x) {
            _logger.log(Level.WARNING, x.getMessage(), x);
            throw x;
        } finally {
            _worker.shutdownNow();
            try { _worker.awaitTermination(1, TimeUnit.HOURS); } catch (InterruptedException x) {}
            _logger.info("terminated");
        }
    }

    /**
     * Shuts down the DaySchedule thread.
     */
    public void shutdown() {
        interrupt();
        try { join(); } catch (Exception x) { _logger.log(Level.WARNING, "Failed to see DaySchedule thread joining", x); }
    }

    @SuppressWarnings("unchecked")
    private final Activity<T>[] reload() {
        SortedSet<Activity<T>> set = new TreeSet<Activity<T>>(_user.loadScheduledActivities(_date));

        // insert DayStarting and DayFinished activities if the schedule is not empty
        if (set.size() > 0) {
            int gap = set.first().clock/MILLISECONDS_PER_MINUTE;
            if (gap > _reopenAfter) {
                set.add(new Activity<T>(this, _reopenAfter, Activity.DAY_STARTING));
            } else {
                set.add(new Activity<T>(this, gap - 1, Activity.DAY_STARTING));
            }
            gap = (MILLISECONDS_PER_DAY - set.last().clock)/MILLISECONDS_PER_MINUTE;
            if (gap > _closeBefore) {
                set.add(new Activity<T>(this, MILLISECONDS_PER_DAY/MILLISECONDS_PER_MINUTE - _closeBefore, Activity.DAY_FINISHED));
            } else {
                set.add(new Activity<T>(this, MILLISECONDS_PER_DAY/MILLISECONDS_PER_MINUTE - (gap - 1), Activity.DAY_FINISHED));
            }
        }

        if (_logger.isLoggable(Level.INFO)) {
            StringBuilder sb = new StringBuilder("new DaySchedule {\n");
            for (Activity<T> a: set) {
                sb.append(String.format("\t[%02d:%02d] (%02d:%02d) %16s %s",
                    ((a.clock + _midnight) % MILLISECONDS_PER_DAY)/(60*MILLISECONDS_PER_MINUTE),
                    ((a.clock + _midnight) % MILLISECONDS_PER_DAY)/MILLISECONDS_PER_MINUTE % 60,
                    a.clock/(60*MILLISECONDS_PER_MINUTE),
                    a.clock/MILLISECONDS_PER_MINUTE % 60,
                    a.event,
                    a.tag
                )).append('\n');
            }
            sb.append("}");
            _logger.info(sb.toString());
        }

        return set.toArray((Activity<T>[])Array.newInstance(Activity.class, set.size()));
    }

    /*!
     * Returns logical clock: milliseconds since logical midnight
     */
    private final int logicalClockMillis() {
        return trueToLogical(trueClockMillis(false));
    }

    /*!
     * Returns true clock: milliseconds since true midnight, optionally calibrating date
     *
     * @param calibrate - calibrates current date if set to true
     */
    private final int trueClockMillis(boolean calibrate) {
        long now = System.currentTimeMillis() + _transition;
        _calendar.setTimeInMillis(now);
        _calendar.set(Calendar.HOUR_OF_DAY, 0);
        _calendar.set(Calendar.MINUTE, 0);
        _calendar.set(Calendar.SECOND, 0);
        _calendar.set(Calendar.MILLISECOND, 0);
        int clock = (int)(now - _calendar.getTimeInMillis());
        if (calibrate) {
            if (clock < _midnight) {
                // between true midnight and just before the logical midnight - the second half of a logical day
                _calendar.set(Calendar.DAY_OF_MONTH, _calendar.get(Calendar.DAY_OF_MONTH) + _offset - 1);
            } else {
                // between logical midnight and just before the true mignight - the first half of a logical day
                _calendar.set(Calendar.DAY_OF_MONTH, _calendar.get(Calendar.DAY_OF_MONTH) + _offset);
            }
            _date.setTimeInMillis(_calendar.getTimeInMillis());
        }
        return clock;
    }

    /*!
     * Converts true clock (milliseconds since true midnight) to logical clock (milliseconds since logical midnight)
     */
    private final int trueToLogical(int clock) {
        return (MILLISECONDS_PER_DAY + clock - _midnight) % MILLISECONDS_PER_DAY;
    }

    /*!
     * Converts logical clock (milliseconds since logical midnight) to true clock (milliseconds since true midnight)
     */
    private final int logicalToTrue(int clock) {
        return (clock + _midnight) % MILLISECONDS_PER_DAY;
    }

    /*!
     * Sleeps till a future time.
     */
    private final void sleepTill(long future) throws InterruptedException {
        long milliseconds = future - logicalClockMillis();
        if (milliseconds > 0) {
            Thread.sleep(milliseconds);
        }
    }

	private long _transition;    // milliseconds

	        final User<T> _user;
	private final int _midnight;    // milliseconds
	private final int _offset;      // days
	private final int _closeBefore; // minutes
	private final int _reopenAfter; // minutes
            final Calendar _date = Calendar.getInstance();
	private final Calendar _calendar = Calendar.getInstance();
    private final ExecutorService _worker;
    private int _start, _close;    // start & close time, for reporting purpose
    private ObjectName _name;
}
