# User Guide

This document is a "how-to" user guide describing techniques required to operate specific features of the Bert robot.
Refer to [implementation](http://github.com/chuckcoughlin/bert/tree/master/docs/implementation.md) for detailed descriptions of feature designs and the code behind them.

***************************************************************
## Table of Contents <a id="table-of-contents"></a>
  * [Startup](#startup)
  * [Speech](#speech)
  * [Example Statements](#example)
  * [Vocabulary](#vocabulary)


*********************************************************
### a - Startup <a id="startup"></a>
Once the robot is powered on, a boot sequence commences. On completion, Bert will announce a randomized "Ready" statement.

*********************************************************
### b - Speech <a id="speech"></a>
The primary method of command and control is via spoken English commands. If a command, statement or question is not recognized,
Bert will respond with an appropriate request for clarification.
*********************************************************
### c - Example Statements <a id="example"></a>
The lists below show typical statements that Bert will recognize.
The full range of understanding comes from combining these patterns with
options from the vocabulary list in the next section.
##### Commands

##### Queries
```
    How old are you?
    What is your cycle time?
    What is the id of your left shoulder z?
    What is the z position of your neck?
```
##### Settings
Infrequently it may be necessary to inform the robot of some fact.
There is no visible response to these statements.

*********************************************************
### d - Vocabulary <a id="vocabulary"></a>
"Bert" has a fixed vocabulary when it comes to names of parameters in
various categories.
  * Axes: x, y, z
  * Body part: abs, ankle, arm, bust, elbow, head, hip, knee, neck, shoulder
  * Core property: age, cadence, cycle time, duty cycle, height, name
  * Joint Property: id, maximum angle, minimum angle, motor type, offset,
     orientation, position, speed, torque
  * Side: left, right
