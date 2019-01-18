## Test Plan

This document lays out a series of feature tests for the "Bert" project. Those tests which have passed are coded in
<span style="color:green;">green</span>. The green markup is a pretty fair indication of the progress of the project.


***************************************************************
## Table of Contents <a id="table-of-contents"></a>
  * [Connectivity](#connectivity)
  * [Calibration](#calibration)

*********************************************************
### a - Connectivity <a id="connectivity"></a>
[toc](#table-of-contents)
The purpose of this section is to validate wiring and addressing of the stepper motors.
* `Validate Connections` - use *herborist* to access each of the motor groups - head, torso, right-leg, right-arm, left-leg and left-arm. Verify that the discovery operation shows the correct motor ids within each group.
* `Read Positions` - use the terminal application to request the current position of each joint. Move the joint manually and verify that the position changes. A typical command:
```
    what is the position of your left elbow
```

### b - Calibration <a id="calibration"></a>
