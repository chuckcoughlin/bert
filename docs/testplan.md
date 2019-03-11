## Test Plan

This document lays out a series of feature tests for the "Bert" project. Those tests which have passed are coded in (![green](/images/ball_green.png)). Failures of features that have previously been reported as complete are marked in red (![red](/images/ball_red.png)). The green markup is a pretty fair indication of completion status of the project.


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
* ![Validate Connections](/images/ball_green.png) - use *herborist* to access each of the motor groups (*upper* and *lower*). Verify that the discovery operation shows the correct motor ids within each group.
* `Read Positions` - use the terminal application to request the current position of each joint. Move the joint manually and verify that the position changes. A typical query:
```
    what is the position of your left elbow
```

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
