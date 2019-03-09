# User Guide

This document is a "how-to" user guide describing techniques required to operate specific features of the Bert robot.
Refer to [implementation](http://github.com/chuckcoughlin/bert/tree/master/docs/implementation.md) for detailed descriptions of feature designs and the code behind them.

***************************************************************
## Table of Contents <a id="table-of-contents"></a>
  * [Speech](#speech)
    * [Startup](#startup)
    * [Example Statements](#example)
    * [Vocabulary](#vocabulary)
  * [Android Application](#android)

*********************************************************
### Speech <a id="speech"></a>
  The primary method of command and control is via spoken English commands. If a command, statement or question is not recognized,
  Bert will respond with an appropriate request for clarification.
*********************************************************
#### a - Startup <a id="startup"></a>
Once the robot is powered on, a boot sequence commences. On completion, Bert will announce a randomized "Ready" statement.
*********************************************************
#### b - Example Statements <a id="example"></a>
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
### c - Vocabulary <a id="vocabulary"></a>
"Bert" has a fixed vocabulary when it comes to names of parameters in
various categories.
  * Axes: x, y, z
  * Body part: abs, ankle, arm, bust, elbow, head, hip, knee, neck, shoulder
  * Core property: age, cadence, cycle time, duty cycle, height, name
  * Joint Property: id, maximum angle, minimum angle, motor type, offset,
     orientation, position, speed, torque
  * Side: left, right

*********************************************************
  ### Android Application <a id="android"></a>
  [toc](#table-of-contents)

The Android tablet is a vital part of the robot control system. It provides
voice services (both interpretive (listening) and spoken). The tablet must be
within ~10' for Bluetooth communication. It may be placed conveniently in the
backpack worn by the robot.

The control application is called ***BertSpeak***, and, while it must be running,
there is no necessary interactive control other than speech. The screens shown
below provide some minimal troubleshooting capabilities.

Make sure that the application is configured in the Settings with Permission for Microphone and Storage.

*********************************************************
  ![BertSpeak](/images/bertspeak_cover.png)
 ```                        BertSpeak - Cover Page ```</br>
  The **cover** panel shows the current state of connection with the main robot
  processor.   
  *********************************************************
   ![BertSpeak](/images/bertspeak_settings.png)
 ```                        BertSpeak - Settings Page ```</br>
    The **settings** page should have the following values:

  ```
    Paired Device    bert_humanoid 
  ```
