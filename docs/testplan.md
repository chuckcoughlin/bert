## Test Plan

This document lays out a series of feature tests for the "Bert" project. Those tests which have passed are marked with green (![green](/images/ball_green.png)). Features that are currently in a broken state are marked in red (![red](/images/ball_red.png)).  A yellow (![yellow](/images/ball_yellow.png)) marker indicates
a feature that is actively being debugged. Gray (![gray](/images/ball_gray.png)) indicates features that are not yet implemented. The green marker is a pretty fair indication of completion status of the project.


***************************************************************
## Table of Contents <a id="table-of-contents"></a>
  * [Connectivity](#connectivity)
  * [Calibration](#calibration)
  * [Movement](#movement)
  * [Motion Planning](#planning)
  * [Tablet Interaction](#tablet)
  * [Performance](#performance)
  * [Grammar](#grammar)

*********************************************************
### a - Connectivity <a id="connectivity"></a>
[toc](#table-of-contents)<br/>
This section contains tests that validate the wiring and addressing of stepper motors,
the conversion of raw readings from the motors into proper engineering
units, and the listing of various parameters in the motor control tables. Finally, there
is a section listing maintenance commands.
* ![green](/images/ball_green.png) ``Validate Connections``  - Use *herborist* to access each of the motor groups (*upper* and *lower*). Verify that the discovery operation shows the correct motor ids within each group.
* ![green](/images/ball_green.png) ``Joint IDs`` - verify that the pairing of name to ID
is correct for every joint.  Syntax of the query:
```
    what is the id of your left hip y
```
* ![green](/images/ball_green.png) ``List attributes`` - Use the terminal application to list
values of a selected attribute for all joints. Verify conversions from raw readings
to engineering units. Available
 parameters include: position, speed, load, voltage and temperature. Values are read directly
from the motors, scaled and logged.
(At a later time the values will be displayed on the tablet.) Typical requests:
```
    tell me your joint positions
    list the speeds of your motors
```
* ![green](/images/ball_green.png) ``Joint Properties`` - Use the terminal application to
read the current values of joint properties. A complete list of joint names and properties may be found
in the *Vocabulary* section of the user guide. In addition to properties configured in the configuration
file (like: id, motor type, orientation, minimum angle and maximum angle), properties include current
values read directly from the motor (like: position (degrees), speed (degrees/sec), load (N-m),
temperature (deg C), and voltage (volts). A typical query:
```
    what is the position of your left elbow
    what is your right ankle position
    what is the speed of your right knee
    what is the temperature of your right shoulder x
    what is the torque of your left hip x
```

* ![gray](/images/ball_gray.png) ``Maintenance Commands`` - These are no-argument commands that
perform various system operations. In the list below, the check mark indicates completion.
  - [x] halt: stop the control processes on the robot. Leave the operating system running.
  - [ ] reset: clear any unprocessed results from the serial ports. This is an internal recovery
  operation in the event of I/O errors.
  - [ ] shutdown: power off the robot after a clean shutdown.


### b - Calibration <a id="calibration"></a>
The purpose of this section is to validate stepper motor configuration parameters
and to verify the correct orientation and limit values.

* ![green](/images/ball_green.png) ``Configuration File`` - use the terminal application to
dump motor parameters from the XML configuration file. Values are sent to a log file.
The parameter list includes: id, motor type, orientation and angle limits. The request is:
```
   describe your configuration
```
* ![green](/images/ball_green.png) ``Hardware Limits`` - Use the terminal application to query limits
that are configured in each motor's EEPROM. (Units must be flashed individually to change these.)
Values include angle, speed and and torque limits. Results are logged.
Typical syntax:
```
    what are the limits on your right knee
```
* ![green](/images/ball_green.png) ``Goals`` - Use the terminal application to list
the current goals for a joint. Goal parameters
include angle, speed and and torque limits. Results are logged. Speeds are degrees/sec and
torques are newton-meters.
Typical syntax:
```
    what are the targets for your left shoulder x
```
* ![green](/images/ball_green.png) ``Positions`` - Use the terminal application to
revisit the detection of position. In particular, check that the orientation is
proper and limits and values makes sense. E.g. a straight knee should be at 180 deg; the neck
when facing straight ahead is 0 deg. Fix the configuration file limits to be within the actual
EEPROM limits. A typical query:
```
    what is the position of your left elbow
    what are the limits of your left elbow
```

* ![green](/images/ball_green.png) ``Sane Startup`` - When the robot is first powered on,
its limbs are in unknown positions. As part of the startup sequence, read the positions of
all joints and move those
that are outside configured limits to the closest "legal" value. When this initialization
is complete, issue a response stating that the robot is ready.
```
  bert is ready
```

### c - Movement <a id="movement"></a>
This section includes the first set of tests for driving the position of the robot.
It also introduces use of the database to store "poses". The tests here simply
drive joints to a goal. There is not yet a concept of trajectory planning.

* ![green](/images/ball_green.png) ``Move Joint`` - Using the terminal application,
move a joint to a specified position. Make sure that any attempts to drive it
out-of-range are clipped . Sample command syntax:
```
    move your left knee to 90 degree
    turn your right thigh x to 180
    set the left elbow position to 90 degrees
    set the position of your left ankle to 60
    set the left elbow to 90
    straighten your left knee
```
* ![green](/images/ball_green.png) ``Pronouns`` - Show the use of 'it' and 'other' as substitutions for
the last referenced joint or side. Sample command syntax:
```
    move it to 20
    set your other elbow to 90
    straighten it
```
* ![green](/images/ball_green.png) ``Change speed or torque`` - Using the terminal application,
change the speed or torque of a joint. Both speed and torque are expressed as a percentage
of the maximum as defined in the XML configuration file.
Sample command syntax:
```
    set the speed of your right elbow to 50
    set the torque of your other elbow to 50
    move normally
    move quickly
    move very slowly
```
The "move" commands set speeds for all joint movements at once.

* ![green](/images/ball_green.png) ``Enable torque`` - Dynamixel motors may be
configured to be freewheeling and compliant or stiff. The term for this feature
is "torque enable". Our names are "relax" and "freeze". These commands may be applied to individual
joints, limbs or the entire robot.
Sample command syntax:
```
    relax your right arm
    freeze your right elbow
    hold it
    relax
    freeze
    go rigid
    go limp
    straighten up
```
Note that the Dynamixel motors automatically enable torque whenever a position is set (otherwise there
would be no point). Additionally, whenever a joint is stiffened, the position is read and recorded as "current".

* ![gray](/images/ball_gray.png) ``Save a Pose`` - Associate the current joint positions with
a named pose. The pose is saved in the robot's internal database.
```
    your pose is saluting
    you are standing
    what is your pose
    save your pose
    save your pose as leaping
```
In the case where a name is not specified,
the robot will use the most recently referenced pose name.

* ![gray](/images/ball_gray.png) ``Map Pose to Command`` - Associate a pose with a command to
take that pose. Pose and command names are
arbitrary, but must be spelled in the same
way as the Android text-to-speech processor. Sample syntax:
```
  to salute means to take the pose saluting
  when you sit you are sitting
  when i say sit then take the pose sitting
  to wave at me means you are waving at me
  to cry means to be crying
```
* ![green](/images/ball_green.png) ``Pose`` - Command the robot to assume a previously stored
pose. The command is a word or phrase taken from a previous mapping. As of yet, this movement does not
account for positional conflicts.
```
    salute
```

* ![gray](/images/ball_gray.png) ``Define an Action`` - Actions are a series of commands (usually poses)
executed with intervening time intervals.

### d - Motion Planning <a id="planning"></a>
Plan motions so as to bring end effectors to a certain position and avoid collisions
between different parts of the robot.

* ![gray](/images/ball_gray.png) ``Forward Kinematics`` - Query the robot to determine its understanding
of the location of its joints or appendages. The result consists of distances (~m) in three dimensions
of target to the origin which is the center of the pelvis.
```
    where is your left hand
    where are your eyes
    where is your right ear
```
  The object of the question may be either an "appendage" (as used in the examples) or a joint. An appendage
is simply a protuberance somewhere on a limb. The 'URDF' file defines legal names.
In addition to validating that the syntax works, check numeric results for the following:
  - [x] ABS_Y: this is the first joint, at the top of the Pelvis. Its position should never change.
  - [ ] ABS_X: connected on top of ABS_Y, verify its position as ABS_Y is moved.
  - [ ] ... and so on. Follow the joints in order until reaching the left finger (an appendage).
  - [ ] ... likewise, follow the joints in order until reaching the right finger.
  - [ ] RIGHT_HIP_X: this is the first joint in a sub-chain. Its position should never change.
  - [ ] ... as before, follow this chain of joints to the right toe.
  - [ ] ... likewise, follow the left hip sub-chain to the left toe.
  - [ ] NOSE: make sure that the HEAD appendage calculations are correct.

### e - Tablet Interaction <a id="tablet"></a>
Test the integration of the android tablet with the robot, especially as it involves
spoken text.
* ![green](/images/ball_red.png) ``Speech`` - Validate that all commands and queries
used in the previous section can be executed via speech and that responses are
likewise formulated into audible sentences.

* ![green](/images/ball_green.png) ``Transcript`` - Validate that the tablet keeps a record of spoken
commands and corresponding responses from the robot.

* ![gray](/images/ball_gray.png) ``Logging`` - Validate that notifications (errors and results
  of commands that produce lists of things) are recorded on the tablet application.

* ![gray](/images/ball_gray.png) ``Voice Suppression`` - It can be annoying when the robot
  attempts to interpret (and fails) background speech not directed  towards it. The
  commands below place the robot into a state where it ignores ambient speech until specifically
  directed to be attentive.

  ```
      ignore me
      pay attention
  ```

### f - Performance <a id="performance"></a>
Test the ability to query performance metrics from the dispatcher.
* ![green](/images/ball_green.png) ``Metrics`` - use the terminal application to query
the dispatcher for: name, age, height, cadence, cycle time and duty cycle. The results
should be formatted into proper english sentences. Typical syntax:
```
  how tall are you
  what is your age
```

### g - Grammar <a id="grammar"></a>
This section includes tests of irregular or one-off speech patterns.
* ![green](/images/ball_green.png) ``Completed``  - these are statements outside the regular
syntax shown above that are processed in a reasonable manner.
```
    hi bert
    move in slow motion
    why do you wear mittens
```

* ![yellow](/images/ball_yellow.png) ``Desired``  - the list below consists of statements or queries
that are useful, but not currently recognized.<br/>
