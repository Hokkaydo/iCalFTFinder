package be.hokkaydo;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.util.Calendars;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


/**
 * Created by Hokkaydo 11-09-2021.
 */
public class Main {

    //region Schedules ids
    //Those ids are just used to human identify the concerned intervals at the output.
    //Write them down here

    //endregion

    // This Map MUST contain :
    //      As KEY : the schedule id (see up)
    //      As VALUE : a path to the iCalendar to process
    private final Map<String, String> schedules = Map.of(

    );

    public static void main(String[] args) throws IOException {
        new Main().findSharedFreeTime();
    }

    /**
     * Main method which initiate each others method call
     * */
    private void findSharedFreeTime() throws IOException {
        List<Interval.ScheduleFreeIntervals> free = new ArrayList<>();

        schedules.forEach((id, filescheduleId) -> {
            //For each registered scheduled, we try to find the free intervals of time and we add them to a global list
            try {
                free.add(retrieveFreeIntervals(filescheduleId, id));
            } catch (ParserException | IOException e) {
                e.printStackTrace();
            }
        });

        List<Interval.CrossingInterval> finalList = crossIntervals(0, new ArrayList<>(), free, new ArrayList<>());

        //We sort the crossed interval based on which is before the other
        finalList.sort((c1, c2) -> {
            if(c1.start.toInstant().isBefore(c2.start.toInstant())) return -1;
            if(c1.start.toInstant().isAfter(c2.start.toInstant())) return 1;
            return 0;
        });

        //We determine the scheduleId of the file in which the data will be written to
        StringBuilder filescheduleIdBuilder = new StringBuilder();
        schedules.forEach((id, filescheduleId) -> filescheduleIdBuilder.append(id).append("-"));
        String filescheduleId = filescheduleIdBuilder.substring(0, filescheduleIdBuilder.length() - 1) + "--schedule-cross.txt";

        printProcessedDataToFile(filescheduleId, finalList);
    }

    /**
     * We process the iCalendar of the given course to find free intervals
     * @param filescheduleId the scheduleId of the file where the iCalendar is stored
     * @param scheduleId the arbitrary given schedule it used to identify it
     * @return an {@link be.hokkaydo.Interval.ScheduleFreeIntervals} containing the scheduleId and its free intervals
     * */
    private Interval.ScheduleFreeIntervals retrieveFreeIntervals(String filescheduleId, String scheduleId)
            throws ParserException, IOException {
        List<Course> courses = new ArrayList<>();

        // We retrieve the iCalendar using the ICal4J API
        Calendar calendar = Calendars.load(Objects.requireNonNull(Main.class.getClassLoader().getResource(filescheduleId)).getFile());

        // We define an endTimeStamp which will be used later to determine if a course start when the other end or not.
        Timestamp endTimestamp = Timestamp.from(Instant.EPOCH);

        /*
          We loop over each course present in the schedule and we create a Course object which will be used later.
          It contains the course scheduleId as well as the timestamp of start and end of this course.
        */
        for (Component component : (List<Component>)calendar.getComponents()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMMddHHmmssX");
            Timestamp start = Timestamp.from(
                    Instant.from(formatter.parse(
                            component.getProperty("DTSTART").getValue().replaceFirst("T", ""))
                    )
            );
            Timestamp end = Timestamp.from(
                    Instant.from(formatter.parse(
                            component.getProperty("DTEND").getValue().replaceFirst("T", ""))
                    )
            );
            courses.add(new Course(component, new Interval(start, end)));
        }

        // We sort the courses according to the fact if they are before or after the other
        courses.sort((c1, c2) -> {
            if(c1.interval.start.toInstant().isBefore(c2.interval.start.toInstant())) return -1;
            if(c1.interval.start.toInstant().isAfter(c2.interval.start.toInstant())) return 1;
            return 0;
        });


        List<Interval> free = new ArrayList<>();
        for (Course course : courses) {
            /*
                If the endTimeStamp is equal 0, it means we just started to loop over the schedule, we can go next
                without forgetting to change it the first course end timestamp
                If the previous course ends when the current course starts we don't have any free interval and as well
                as for the 0 case, we can go next and change the end timestamp
                If the previous course ends after the current course starts, the current course is the most likely a
                parallel course.
            */
            if(endTimestamp.getTime() == 0 || course.interval.start.equals(endTimestamp) || course.interval.start.before(endTimestamp)) {
                endTimestamp = course.interval.end;
                continue;
            }

            // We know that interval is free because the previous condition didn't catch it. We can so add it to the free list
            free.add(new Interval(endTimestamp, course.interval.start));
            endTimestamp = course.interval.end;
        }

        // We return an object containing all free intervals as well as the involved schedule id
        return new Interval.ScheduleFreeIntervals(scheduleId, free);
    }


    /**
     * We cross each previously got interval with the ones from others schedules to determine which are at the same time
     * @param i an integer to stop the recursive call when needed (when it reaches the size of scheduleFreeIntervals
     * @param crossProcessingIntervals the intervals being processed by the recursive call. Should be empty at
     *                                 initialization, it gets filled by the recursive method
     * @param scheduleFreeIntervals a {@link List} of scheduled free intervals to cross together
     * @param crossedIntervals the already crossed intervals, saved through the recursive call to be returned at the end of the method execution
     * @return a {@link List} of each possible interval crossing for given intervals
     * */
    private List<Interval.CrossingInterval> crossIntervals(
            int i,
            List<Interval.CrossingInterval> crossProcessingIntervals,
            List<Interval.ScheduleFreeIntervals> scheduleFreeIntervals,
            List<Interval.CrossingInterval> crossedIntervals
    ) {
        if(scheduleFreeIntervals.size() == 0) throw new IllegalArgumentException("Error, no schedule free intervals given");

        //If we have gone through all schedule free intervals, we can end the recursive call here
        if(i == scheduleFreeIntervals.size()) {
            //We add last processed data to already previously processed data
            crossedIntervals.addAll(crossProcessingIntervals);
            return crossedIntervals;
        }
        //We get the next schedule free intervals to process (i is increased at each new call)
        Interval.ScheduleFreeIntervals first = scheduleFreeIntervals.get(i);

        /*
           If no data is being processed, it means we just started the recursive call, we have to give data to process
           We just assume that the first schedule contains free intervals that match until now
           (they will be crossed with others later)
        */
        if(crossProcessingIntervals.size() == 0) {
            List<Interval.CrossingInterval> newCrossProcessingIntervals = new ArrayList<>();
            for (Interval interval : first.intervals) {
                List<Interval.ScheduleInterval> scheduleIntervals = new ArrayList<>();
                scheduleIntervals.add(new Interval.ScheduleInterval(first.scheduleId, interval.start, interval.end));
                Interval.CrossingInterval crossingInterval = new Interval.CrossingInterval(
                        scheduleIntervals,
                        interval.start,
                        interval.end
                );
                newCrossProcessingIntervals.add(crossingInterval);
            }
            return crossIntervals(i+1, newCrossProcessingIntervals, scheduleFreeIntervals, crossedIntervals);
        }

        // We split the ScheduleFree objects in a List of CrossingInterval containing the same information but split
        for (Interval.ScheduleFreeIntervals scheduleFree : scheduleFreeIntervals) {
            for (Interval interval1 : scheduleFree.intervals) {
                List<Interval.ScheduleInterval> list = new ArrayList<>(
                        Collections.singletonList(
                                new Interval.ScheduleInterval(scheduleFree.scheduleId, interval1.start, interval1.end)
                        )
                );
                crossProcessingIntervals.add(new Interval.CrossingInterval(list, interval1.start, interval1.end));
            }
        }

        // This list will store the newly crossed intervals to cross them at the next recursive call
        List<Interval.CrossingInterval> newList = new ArrayList<>();

        // For each interval of actually processed schedule, we cross it with the already previously crossed intervals
        for (Interval interval : first.intervals) {
            for (Interval.CrossingInterval crossingInterval : crossProcessingIntervals) {

                /*
                   If the crossed interval has already been crossed with the processed schedule, we skip to next interval.
                   It permits to avoid doubled values
                */
                if(crossingInterval.scheduleIntervals
                        .stream()
                        .anyMatch(scheduleInterval -> scheduleInterval.scheduleId.equals(first.scheduleId))) continue;

                // Check if both intervals can be crossed
                if(interval.isCrossing(crossingInterval)){
                    /*
                       We add the concerned interval of the processed schedule to the list of intervals to allows
                       to define a crossing interval
                    */
                    crossingInterval.scheduleIntervals.add(new Interval.ScheduleInterval(
                            first.scheduleId,
                            interval.start,
                            interval.end)
                    );
                    /*
                       We cross the processed interval and the previously crossed interval together and
                       it gives us a simple Interval
                    */
                    Interval cross =  interval.getCrossingInterval(crossingInterval);
                    /*
                        We create then a new CrossingInterval containing the updated information about involved intervals
                        and start & end of the crossed interval
                        We could have modified the previously crossed interval but since the attributes have a final
                        modifier, they can't be modified.
                        However, it also allows us to keep track of all crossed intervals (see next line)
                    */
                    Interval.CrossingInterval newCrossingInterval = new Interval.CrossingInterval(
                            crossingInterval.scheduleIntervals,
                            cross.start,
                            cross.end
                    );
                    crossedIntervals.add(newCrossingInterval);

                    // We add the new interval to the newly crossed list
                    newList.add(newCrossingInterval);
                }
            }
        }
        return crossIntervals(i+1, newList, scheduleFreeIntervals, crossedIntervals);
    }

    /**
     * We print the previously got data in a file
     * @param filescheduleId the scheduleId of the file to write into
     * @param data the data to write
     * */
    private void printProcessedDataToFile(String filescheduleId, List<Interval.CrossingInterval> data) throws IOException {
        //This DateTimeFormatter will format following this pattern : Day/Month/Year de Hours:Minutes:Seconds
        DateTimeFormatter intervalStartFormatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH).appendLiteral('/')
                .appendValue(ChronoField.MONTH_OF_YEAR)
                .appendLiteral('/')
                .appendValue(ChronoField.YEAR)
                .appendLiteral(" de ")
                .appendValue(ChronoField.HOUR_OF_DAY)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR).appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE)
                .toFormatter()
                .withZone(ZoneId.of("UTC+2"))
                .withLocale(Locale.FRANCE);
        //This DateTimeFormatter will format following this pattern : Day/Month/Year à Hours:Minutes:Seconds
        DateTimeFormatter intervalEndFormatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH)
                .appendLiteral('/')
                .appendValue(ChronoField.MONTH_OF_YEAR)
                .appendLiteral('/')
                .appendValue(ChronoField.YEAR)
                .appendLiteral(" à ")
                .appendValue(ChronoField.HOUR_OF_DAY)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE)
                .toFormatter()
                .withZone(ZoneId.of("UTC+2"))
                .withLocale(Locale.FRANCE);

        File file =  new File(filescheduleId);
        if(!file.createNewFile()) throw new IllegalStateException("File " + filescheduleId + " could not be created");
        FileWriter fileWriter = new FileWriter(filescheduleId);

        /*
         Pattern :
          Cross:
          schedulescheduleId     time/stamp/from  hours:minutes:seconds              time/stamp/to    hours:minutes:seconds
          schedulescheduleId     time/stamp/from  hours:minutes:seconds              time/stamp/to    hours:minutes:seconds
          ...
               ->      crossing/timestamp/from    hours:minutes:seconds     crossing/timestamp/to     hours:minutes:seconds
         * */
        for (Interval.CrossingInterval crossingInterval : data) {
            fileWriter.write("Cross :     \n");
            for (Interval.ScheduleInterval scheduleInterval : crossingInterval.scheduleIntervals) {
                fileWriter.write(
                        scheduleInterval.scheduleId +
                                "     " +
                                intervalStartFormatter.format(scheduleInterval.start.toInstant()) +
                                "       " +
                                intervalEndFormatter.format(scheduleInterval.end.toInstant()) + "\n"
                );
            }
            fileWriter.write(
                    "     ->   " +
                            intervalStartFormatter.format(crossingInterval.start.toInstant()) +
                            "       " +
                            intervalEndFormatter.format(crossingInterval.end.toInstant()) + "\n"
            );
            fileWriter.write("\n\n");
        }

        fileWriter.flush();
        fileWriter.close();
    }

    public static class Course{
        public final Interval interval;
        public final Component component;
        public Course(Component component, Interval interval) {
            this.component = component;
            this.interval = interval;
        }
    }

}
