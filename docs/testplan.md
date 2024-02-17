## Test Plan

This document lays out a series of feature tests for the "Bert" project. Those tests which have passed are marked with green (![green](/images/ball_green.png)). Features that are currently in a broken state and are
actively being debugged are marked in red (![red](/images/ball_red.png)).  A yellow (![yellow](/images/ball_yellow.png)) marker indicates a feature that has yet to be tested in the current version.
Gray (![gray](/images/ball_gray.png)) indicates features that are not yet implemented. The collection of
green markers is a pretty fair indication of completion status of the project.

Unless otherwise stated, tests are run by giving typed commands to the stand-alone version of the robot.


***************************************************************
## Table of Contents <a id="table-of-contents"></a>
  * [Startup](#startup)
  * [Configuration](#configuration)
  * [Calibration](#calibration)
  * [Movement](#movement)
  * [Motion Planning](#planning)
  * [Tablet Communication](#tablet)
  * [Tablet Application](#bertspeak)
  * [Parameters](#parameters)
  * [Grammar](#grammar)

*********************************************************
### a - Startup and Test <a id="startup"></a>
* ![green](/images/ball_green.png) ``System Scripts``  - Launch the robot code on system boot. Run standalone
test applications
- [x] bert-server start/stop: Start/stop the "headless" version of the robot code on system boot.
- [x] bert-blueserver start/stop: Run the bluetooth daemon necessary for communication with the tablet.
* ![gray](/images/ball_gray.png) ``Test Applications``  - Test features independent of the robot
application.
- [x] bert-standalone: Run an interactive version of the robot on the odroid. It features a terminal
interface for interactive testing.
- [x] dxl_scan: Show ids of all connected DXM controllers.
- [x] dxl_read: Read parameters of a servo motor.
- [x] dxl_write: Set volatile values for a given motor.

### b - Configuration <a id="configuration"></a>

This section contains tests that validate the wiring and addressing of stepper motors,
the conversion of raw readings from the motors into proper engineering
units, and the listing of various parameters in the motor control tables. Finally, there
is a section listing maintenance commands.
* ![green](/images/ball_green.png) ``Motor addressing``  - Use *dxl_scan* to access each of the motor groups (*upper* and *lower*). Verify that the discovery operation shows the correct motor ids within each group.
* ![yellow](/images/ball_yellow.png) ``Motor configuration``  - Use *dxl_read* to access each individual motor. Verify that parameter settings match values in *bert.xml*.
* ![yellow](/images/ball_yellow.png) ``Individual Joint Properties`` - Use the terminal application to
read the current values of joint properties, one by one. A complete list of joint names and properties may be found
in the *Vocabulary* section of the user guide. In addition to properties configured in the configuration
file (like: id, motor type, orientation, minimum angle and maximum angle), properties include current
values read directly from the motor (like: position (degrees), speed (degrees/sec), load (N-m),
temperature (deg C), and voltage (volts). A typical query:
```
    what is the id of your left hip y
    what is the position of your left elbow
    what is your right ankle position
    what is the speed of your right knee
    what is the temperature of your right shoulder x
    what is the torque of your left hip x
```
* ![yellow](/images/ball_yellow.png) ``Name Parameters`` - This set of commands and the next
are designed for data communication with the tablet. Responses are formatted in JSON. Use the terminal application to list the names of properties available for each joint. There are static and
dynamic properties. Typical requests:
```
    what are your static motor parameters
    what are the dynamic properties of your joints
```
* ![yellow](/images/ball_yellow.png) ``List Parameter Values`` - Use the terminal application to list
values of a selected property for all joints. Verify conversions from raw readings
to engineering units. Available
 parameters include: position, speed, load, voltage and temperature. Values are read directly
from the motors, scaled and bundled into the response in JSON format. Values for static parameters
come directly from the XML configuration file, dynamic properties are read from the motors.
Typical requests:
```
    tell me your joint positions
    list the speeds of your motors
    display your motor ids
```
* ![yellow](/images/ball_yellow.png) ``Goal positions`` - set the position of each joint to its default. Visually verify the position of each joint. Use the standalone version of the robot to enter the command:
```
    attention
```
* ![gray](/images/ball_gray.png) ``Maintenance Commands`` - These are no-argument commands that
perform various system operations. In the list below, the check mark indicates completion.
  - [x] halt: stop the control processes on the robot. Leave the operating system running.
  - [ ] reset: clear any unprocessed results from the serial ports. This is an internal recovery
  operation in the event of I/O errors.
  - [ ] shutdown: power off the robot after a clean shutdown.

### c - Calibration <a id="calibration"></a>
[toc](#table-of-contents)<br/>
The purpose of this section is to validate stepper motor configuration parameters
and to verify the correct orientation and limit values.

* ![yellow](/images/ball_yellow.png) ``Hardware Limits`` - Use the terminal application to query limits
that are configured in each motor's EEPROM. (Units must be flashed individually to change these.)
Values include angle, speed and and torque limits. Results are logged.
Typical syntax:
```
    what are the limits on your right knee
```
* ![yellow](/images/ball_yellow.png) ``Goals`` - Use the terminal application to list
the current goals for a joint. Goal parameters
include angle, speed and and torque limits. Results are logged. Speeds are degrees/sec and
torques are newton-meters.
Typical syntax:
```
    what are the targets for your left shoulder x
```
* ![yellow](/images/ball_yellow.png) ``Positions`` - Use the terminal application to
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

### d - Movement <a id="movement"></a>
[toc](#table-of-contents)<br/>
This section includes the first set of tests for driving the position of the robot.
It also introduces use of the database to store "poses". The tests here simply
drive joints to a goal. There is not yet a concept of trajectory planning.

* ![yellow](/images/ball_yellow.png) ``Move Joint`` - Using the terminal application,
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
* ![yellow](/images/ball_yellow.png) ``Pronouns`` - Show the use of 'it' and 'other' as substitutions for
the last referenced joint or side. Sample command syntax:
```
    move it to 20
    set your other elbow to 90
    straighten it
```
* ![yellow](/images/ball_yellow.png) ``Change speed or torque`` - Using the terminal application,
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

* ![yellow](/images/ball_yellow.png) ``Enable torque`` - Dynamixel motors may be
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

* ![yellow](/images/ball_yellow.png) ``Save a Pose`` - Associate the current joint positions with
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

* ![yellow](/images/ball_yellow.png) ``Map Pose to Command`` - Associate a pose with a command to
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
* ![yellow](/images/ball_yellow.png) ``Pose`` - Command the robot to assume a previously stored
pose. The command is a word or phrase taken from a previous mapping. As of yet, this movement does not
account for positional conflicts.
```
    salute
```

* ![yellow](/images/ball_yellow.png) ``Define an Action`` - Actions are a series of commands (usually poses)
executed with intervening time intervals.

### e - Motion Planning <a id="planning"></a>
[toc](#table-of-contents)<br/>
Plan motions so as to bring end effectors to a certain position and avoid collisions
between different parts of the robot.

* ![yellow](/images/ball_yellow.png) ``Forward Kinematics`` - Query the robot to determine its understanding
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

### f - Tablet Communication <a id="tablet"></a>
[toc](#table-of-contents)<br/>
Test the integration of the android tablet with the robot, especially as it involves
spoken text.
* ![yellow](/images/ball_yellow.png) ``Speech`` - Validate that all commands and queries
used in the previous section can be executed via speech and that responses are
likewise formulated into audible sentences.

### g - Tablet Application <a id="bertspeak"></a>
[toc](#table-of-contents)<br/>

* ![yellow](/images/ball_yellow.png) ```Cover```

![Cover](/images/bertspeak_cover.png)
The cover page shows a reclining picture of
the robot. It also contains controls to
enable/disable speech and to adjust the
volume,

* ![yellow](/images/ball_yellow.png) ```Ignoring```
 It can be annoying when the robot
  attempts to interpret (and fails) background speech not directed  towards it. The
  commands below place the robot into a state where it ignores ambient speech until specifically directed to be attentive.
  ```
      ignore me
      pay attention
  ```
* ![gray](/images/ball_gray.png) ```FaceRec```
![Facial Recognition](/images/bertspeak_facerec.png)
This is the facial recognition page that
will eventually allow the application to
recognize whoever is handling the tablet.

* ![gray](/images/ball_gray.png) ```Animation```

![Animation](/images/bertspeak_animation.png)
This panel is planned to show the robot position
in real-time .

* ![yellow](/images/ball_yellow.png) ```Logging```
  ![Logging](/images/bertspeak_logging.png)
  Validate that notifications and internal
  application errors are logged to this panel.

* ![yellow](/images/ball_yellow.png) ```Settings```
![Settings](/images/bertspeak_settings.png)
There are a small number of configurable parameters
that are settable on this page.

* ![yellow](/images/ball_yellow.png) ```Transcript```

![Transcript](/images/bertspeak_transcript.png) - Validate that the tablet keeps a record of spoken commands and corresponding responses from the robot.


### h - Static Parameters <a id="parameters"></a>
[toc](#table-of-contents)<br/>
Test the ability to query performance metrics from the dispatcher. These do not involve
the stepper motors
* ![green](/images/ball_green.png) ``Metrics`` - use the terminal application to query
the dispatcher for: name, age, height, cadence, cycle time, duty cycle and cycles processed.
The results should be formatted into proper english sentences. Typical syntax:
```
  what is your name
  how tall are you
  what is your age
  what is the cycle count
```

### i - Grammar <a id="grammar"></a>
[toc](#table-of-contents)<br/>
This section includes tests of irregular or one-off speech patterns.
* ![yellow](/images/ball_yellow.png) ``Completed``  - these are statements outside the regular
syntax shown above that are processed in a reasonable manner.
```
    hi bert
    move in slow motion
    why do you wear mittens
```

* ![yellow](/images/ball_yellow.png) ``Desired``  - the list below consists of statements or queries
that are useful, but not currently recognized.<br/>
