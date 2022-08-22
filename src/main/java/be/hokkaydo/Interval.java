package be.hokkaydo;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Stores a {@link Timestamp} for the start and a {@link Timestamp} for the end of this {@link Interval}
 * */
public class Interval {

    public final Timestamp start, end;

    public Interval(Timestamp start, Timestamp end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Checks if the current interval cross another one
     * @param interval the interval to check crossing with
     * @return true if both intervals are crossing together, false otherwise
     * */
    public boolean isCrossing(Interval interval) {
        return !end.before(interval.start) && !end.equals(interval.start) && !interval.end.before(start) && !interval.end.equals(start);
    }

    /**
     * Retrieves the crossing interval between the current one and another
     * @param interval the interval to cross with
     * @return a new {@link Interval} crossing current and given intervals
     * @throws IllegalArgumentException if no crossing is detected between the two intervals
     * */
    public Interval getCrossingInterval(Interval interval) {
        if (!isCrossing(interval)) throw new IllegalArgumentException("No crossing detected");
        Timestamp crossingStart = start.before(interval.start) ? interval.start : start;
        Timestamp crossingEnd = end.before(interval.end) ? end : interval.end;
        return new Interval(crossingStart, crossingEnd);
    }

    @Override
    public String toString() {
        return "Interval{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Interval interval = (Interval) o;

        if (!start.equals(interval.start)) return false;
        return end.equals(interval.end);
    }

    @Override
    public int hashCode() {
        int result = start.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    /**
     * Simple extending of {@link Interval} class to additionally store the schedule id
     */
    public static class ScheduleInterval extends Interval {
        public final String scheduleId;

        public ScheduleInterval(String scheduleId, Timestamp start, Timestamp end) {
            super(start, end);
            this.scheduleId = scheduleId;
        }

        @Override
        public String toString() {
            return "ScheduleInterval{" +
                    "start=" + start +
                    ", end=" + end +
                    ", scheduleId='" + scheduleId + '\'' +
                    '}';
        }
    }

    /**
     * This class stores free intervals for a particular schedule (identified by the scheduleId)
     * */
    public static class ScheduleFreeIntervals {
        public final String scheduleId;
        public final List<Interval> intervals;

        public ScheduleFreeIntervals(String scheduleId, List<Interval> intervals) {
            this.scheduleId = scheduleId;
            this.intervals = intervals;
        }

        @Override
        public String toString() {
            return "ScheduleFreeIntervals{" +
                    "scheduleId='" + scheduleId + '\'' +
                    ", intervals=" + intervals +
                    '}';
        }
    }

    /**
     * Simple extending of {@link Interval} class to additionally store the {@link ScheduleFreeIntervals} involved in this crossed interval
     */
    public static class CrossingInterval extends Interval {
        public final List<ScheduleInterval> scheduleIntervals;

        public CrossingInterval(List<ScheduleInterval> scheduleIntervals, Timestamp start, Timestamp end) {
            super(start, end);
            this.scheduleIntervals = scheduleIntervals;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            CrossingInterval that = (CrossingInterval) o;

            List<String> thatScheduledIntervalId = that.scheduleIntervals.stream().map(scheduleInterval -> scheduleInterval.scheduleId).collect(Collectors.toList());
            for (ScheduleInterval scheduleInterval : scheduleIntervals) {
                if (!thatScheduledIntervalId.contains(scheduleInterval.scheduleId)) return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + scheduleIntervals.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "CrossingInterval{" +
                    "start=" + start +
                    ", end=" + end +
                    ", scheduleIntervals=" + scheduleIntervals +
                    '}';
        }
    }
}
