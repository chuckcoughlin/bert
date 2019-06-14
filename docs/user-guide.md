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
```
  describe your configuration
  halt
  list the speeds of your motors
  move your left knee to 90 degrees
  set the head horizontal position to 0
  "sit"
```

##### Queries
```
    How old are you
    what are the limits on your right elbow (EEPROM hard limits)
    what are the targets for your neck horizontal (position, speed, torque)
    What is your height
    What is the id of your left shoulder vertical
    What is the vertical position of your neck
    what is your left ankle temperature
```
##### Settings
Infrequently it may be necessary to inform the robot of some fact.
```
  "sit" means to become "sitting"
  your pose is "saluting"
  when I say "sit" take the pose "sitting"
```

*********************************************************
### c - Vocabulary <a id="vocabulary"></a>
"Bert" has a fixed vocabulary when it comes to names of parameters in
various categories.
  * Axes: x, y, z, horizontal, vertical
  * Body parts (joints): abs, ankle, arm, bust, chest, elbow, head, hip,
  knee, neck, shoulder
  * Commands: exit, halt, shutdown, stop
  * Core property: age, cadence, cycle time, duty cycle, height, name
  * Joint Properties: id, maximum angle, minimum angle, motor type, offset,
     orientation, position, speed, temperature, torque, voltage
  * Sides: left, right

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
Additionally, under Settings->General Management, choose the preferred text-speech engine.
(I chose Samsung over Google). Install voice data (I chose a British male).

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
    Server           192.168.1.20
    Port             11046
    Paired Device    bert_humanoid
  ```
