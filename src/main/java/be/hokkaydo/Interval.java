package be.hokkaydo;

import java.sql.Timestamp;
import java.util.List;

/**
 * Simple interval class. Store a {@link Timestamp} for the start and a {@link Timestamp} for the end of this {@link Interval}
 * */
public class Interval {

    public final Timestamp start, end;

    public Interval(Timestamp start, Timestamp end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Check if the current interval crossed the given one
     * @param interval the interval to check crossing with
     * @return true if both intervals cross together, false otherwise
     * */
    public boolean isCrossing(Interval interval) {
        return !end.before(interval.start) && !end.equals(interval.start) && !interval.end.before(start) && !interval.end.equals(start);
    }

    /**
     * Get the crossing interval between the given and the current one
     * @param interval the interval to cross with
     * @return a new {@link Interval} crossing current and given intervals
     * @throws IllegalArgumentException if no crossing is detected between the two intervals
     * */
    public Interval getCrossingInterval(Interval interval) {
        if(!isCrossing(interval)) throw new IllegalArgumentException("No crossing detected");
        Timestamp crossingStart = start.before(interval.start) ? interval.start : start;
        Timestamp crossingEnd = end.before(interval.end) ? end : interval.end;
        return new Interval(crossingStart, crossingEnd);
    }

    /**
     * Simple extending of {@link Interval} class to additionnaly store the schedule id (defined in {@link Main} class)
     * */
    public static class ScheduleInterval extends Interval{
        public final String scheduleId;
        public ScheduleInterval(String scheduleId, Timestamp start, Timestamp end) {
            super(start, end);
            this.scheduleId = scheduleId;
        }
    }

    /**
     * This class stores free intervals for a particular schedule (identified by the scheduleId)
     * */
    public static class ScheduleFreeIntervals {
        public final String scheduleId;
        public final List<Interval> intervals;
        public ScheduleFreeIntervals(String scheduleId, List<Interval> intervals){
            this.scheduleId = scheduleId;
            this.intervals = intervals;
        }
    }
    /**
     * Simple extending of {@link Interval} class to additionnaly store the {@link ScheduleFreeIntervals} involved in this crossed interval
     * */
    public static class CrossingInterval extends Interval {
        public final List<ScheduleInterval> scheduleIntervals;
        public CrossingInterval(List<ScheduleInterval> scheduleIntervals, Timestamp start, Timestamp end) {
            super(start, end);
            this.scheduleIntervals = scheduleIntervals;
        }
    }
}
