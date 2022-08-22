# iCallFTFinder
This program simply allows you to cross multiple schedules together to find the shared free time between them.
It is written in Java and runs on the command line.

## Usage 
To use this program, you must first have Java installed on your computer. You can download it [here](https://www.java.com/en/download/).

You can then clone this repository and build a shadowJar using Gradle. You can do this by running the following command in the root directory of the project:
```
gradle shadowJar
```
This will create a jar file in the `build/libs` directory. You can then run the program by running the following command:
```
java -jar build/libs/iCallFTFinder-1.0-SNAPSHOT-all.jar
```
You can also run the program by running the following command:

The program will prompt you for schedules you want to cross. You can then enter the schedules in the following format when running it from the command line:
```
java -jar build/libs/iCallFTFinder-1.0-SNAPSHOT-all.jar "scheduleId1" "schedulePath1" "scheduleId2" "schedulePath2" ...
```
The schedules files must be in .ics format (iCalendar), otherwise the program won't be able to read them.
The program will then output the shared free time between the schedules in a file called "scheduleId1-scheduleId2--schedule-cross.txt" in the execution directory.

## Thanks
Special thanks to myself for writing this program and to you to be very likely the first person to read this useless README file

Cheers! :beers:
