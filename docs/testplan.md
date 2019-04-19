## Test Plan

This document lays out a series of feature tests for the "Bert" project. Those tests which have passed are marked with green (![green](/images/ball_green.png)). Features that are currently in a broken state are marked in red (![red](/images/ball_red.png)).  A yellow (![yellow](/images/ball_yellow.png)) marker indicates
a feature that is actively being debugged. Gray (![gray](/images/ball_gray.png)) indicates features that are not yet implemented. The green marker is a pretty fair indication of completion status of the project.


***************************************************************
## Table of Contents <a id="table-of-contents"></a>
  * [Connectivity](#connectivity)
  * [Calibration](#calibration)
  * [Movement](#movement)
  * [Tablet Interaction](#tablet)
  * [Performance](#performance)
  * [Grammar](#grammar)

*********************************************************
### a - Connectivity <a id="connectivity"></a>
[toc](#table-of-contents)
The purpose of this section is to validate wiring and addressing of the stepper motors.
It also validates the conversion of raw readings from the motors into proper engineering
units.
* ![green](/images/ball_green.png) Validate Connections  - use *herborist* to access each of the motor groups (*upper* and *lower*). Verify that the discovery operation shows the correct motor ids within each group.
* ![green](/images/ball_green.png) Joint IDs - verify that the pairing of name to ID
is correct for every joint.  Syntax of the query:
```
    what is the id of your left hip y
```
* ![green](/images/ball_green.png) List attributes - use the terminal application to list
values of a selected attribute for all joints. Verify conversions from raw readings
to engineering units. Available
 parameters include: position, speed, load, voltage and temperature. Values are read directly
from the motors, scaled and logged.
(At a later time the values will be displayed on the tablet.) Typical requests:
```
    tell me your joint positions
    list the speeds of your motors
```
* ![green](/images/ball_green.png) Joint Properties - use the terminal application to
read the current values of joint properties. A complete list of joint names and properties may be found
in the *Vocabulary* section of the user guide. In addition to properties configured in the configuration
file (like: id, motor type, orientation, minimum angle and maximum angle), properties include current
values read directly from the motor (like: position, speed, load, temperature,
and voltage). A typical query:
```
    what is the position of your left elbow
```
### b - Calibration <a id="calibration"></a>
The purpose of this section is to validate stepper motor configuration parameters
and to verify the correct orientation and limit values.

* ![green](/images/ball_green.png) Configuration File - use the terminal application to
dump motor parameters from the XML configuration file. Values are sent to a log file.
The parameter list includes: id, motor type, orientation and angle limits. The request is:
```
   describe your configuration
```
* ![green](/images/ball_green.png) Hardware Limits - use the terminal application to query limits
that are configured in each motor's EEPROM. (Units must be flashed individually to change these.)
Values include angle, speed and and torque limits. Results are logged.
Typical syntax:
```
    what are the limits on your right knee
```
* ![green](/images/ball_green.png) Goals - use the terminal application to list
the current goals for a joint. Goal parameters
include angle, speed and and torque limits. Results are logged.
Typical syntax:
```
    what are the targets for your left shoulder x
```
* ![yellow](/images/ball_yellow.png) Positions - use the terminal application to
revisit the detection of position. In particular, check that the orientation is
proper and limits and values makes sense. E.g. a straight knee should be at 180 deg; the neck
when facing straight ahead is 0 deg. Fix the configuration file limits to be within the actual
EEPROM limits. A typical query:
```
    what is the position of your left elbow
    what are the limits of your left elbow
```

### c - Movement <a id="movement"></a>
This section includes the first set of tests for driving the position of the robot.

### d - Tablet Interaction <a id="tablet"></a>
Test the integration of the android tablet with the robot, especially as it involves
spoken text.
* ![red](/images/ball_red.png) Speech - validate that all commands and queries
used in the previous section can be executed via speech and that responses are
likewise formulated into audible sentences.

### e - Performance <a id="performance"></a>
Test the ability to query performance metrics from the dispatcher.
* ![green](/images/ball_green.png) Metrics - use the terminal application to query
the dispatcher for: name, age, height, cadence, cycle time and duty cycle. The results
should be formatted into proper english sentences. Typical syntax:
```
  how tall are you
  what is your age
```

### f - Grammar <a id="grammar"></a>
This section includes tests of more complex speech patterns.
* ![green](/images/ball_green.png) Completed  - these are statements outside the regular
syntax shown above that are processed in a reasonable manner.
* ![yellow](/images/ball_yellow.png) Desired  - the list below consists of statements or queries
that are useful, but not currently recognized.
