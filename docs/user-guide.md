# User Guide

This document is a "how-to" user guide describing techniques required to operate specific features of the Bert robot.
Refer to [implementation](http://github.com/chuckcoughlin/bert/tree/master/docs/implementation.md) for detailed descriptions of feature designs and the code behind them.

***************************************************************
## Table of Contents <a id="table-of-contents"></a>
  * [Startup](#startup)
  * [Commands](#commands)
  * [Vocabulary](#vocabulary)


*********************************************************
### a - Startup <a id="startup"></a>
Once the robot is powered on, a boot sequence commences. On completion, Bert will announce a randomized "Ready" statement.

*********************************************************
### a - Commands <a id="commands"></a>
The primary method of command and control is via spoken English commands. If a command, statement or question is not recognized,
Bert will respond with an appropriate request for clarification.
*********************************************************
### a - Vocabulary <a id="vocabulary"></a>
"Bert" has a fixed vocabulary when it comes to names of parameters in
various categories.
  * Axes:
      x, y, z
  * Core Property: cadence, cycle time, duty cycle
  * Joint Property: id, offset, maximum angle, minimum angle, , motor type,
     orientation, speed, torque
  * Side: left, right
