## Test Plan

This document lays out a series of feature tests for the "Bert" project. Those tests which have passed are marked with green (![green](/images/ball_green.png)). Failures of features that have previously been reported as complete are marked in red (![red](/images/ball_red.png)). The green marker is a pretty fair indication of completion status of the project. A yellow (![yellow](/images/ball_yellow.png)) marker indicates
a feature that is actively being debugged.


***************************************************************
## Table of Contents <a id="table-of-contents"></a>
  * [Connectivity](#connectivity)
  * [Calibration](#calibration)
  * [Performance](#performance)
  * [Grammar](#grammar)

*********************************************************
### a - Connectivity <a id="connectivity"></a>
[toc](#table-of-contents)
The purpose of this section is to validate wiring and addressing of the stepper motors.
* ![green](/images/ball_green.png) Validate Connections  - use *herborist* to access each of the motor groups (*upper* and *lower*). Verify that the discovery operation shows the correct motor ids within each group.
* ![green](/images/ball_yellow.png) Read the robot configuration - use the terminal application to list configured motor
parameters, including: id, motor type. This does not involve reading the motors directly. A typical request:
```
    what is your configuration
```
* ![yellow](/images/ball_yellow.png) Read the status of all motors/joints - use the terminal application to read of
motor parameters, including: id, position, velocity. A typical request:
```
    describe your motors
```
* ![yellow](/images/ball_yellow.png) Read Positions - use the terminal application to request the current position of each joint. Move the joint manually and verify that the reported position changes. A typical query:
```
    what is the position of your left elbow
```
* ![yellow](/images/ball_yellow.png) Ask Positions - use spoken requests through the
Android tablet to ascertain the current position of each joint (the same queries as in the previous test). Move the joint manually and verify that the answer changes.

### b - Calibration <a id="calibration"></a>
The purpose of this section is to validate the stepper motor configuration.
A complete list of joint names and properties may be found in the user guide.
* `Joint Properties` - use the terminal application to query and validate
position, offset, orientation, and angle extremes at each joint.
A typical query:
```
    what is the id of left hip y
```
* `Correct Configuration` - use the terminal application to set configurable
parameters at each joint. Typical syntax:
```
    set the offset of left hip y to 88 degrees
    set the minimum angle of the right shoulder to -45 degrees
```

### c - Performance <a id="performance"></a>
Test the ability to query performance metrics from the dispatcher.
* <span style="color:green;">Query Metrics</span> - use the terminal application to query
the dispatcher for: name, age, height, cadence, cycle time and duty cycle. The results
should be formatted into proper english sentences.

### d - Grammar <a id="grammar"></a>
