# High-level Design and Software Architecture

Control software embedded in the "Bert" project takes its inspiration from the [Poppy Humanoid](https://www.poppy-project.org) project at
[Generation Robots](https://www.generationrobots.com/en/278-poppy-humanoid-robot). Our implementation deviates significantly, however. The core language is Kotlin instead of C++ and Python. The feature-set has been greatly simplified - and we have added natural-language speech processing with the integration of an Android tablet.

The *Poppy*  project is described in detail in the thesis ["Poppy: open-source, 3D printed and fully-modular
robotic platform for science, art and education"](https://hal.inria.fr/tel-01104641v1/document) by Matthieu Lapeyre. A full list of project authors may be found at: <https://github.com/poppy-project/poppy-humanoid/doc/authors.md>.

The bulk of this document addresses various design issues and approaches to their solutions (or lack thereof)

***
## Table of Contents <a id="table-of-contents"/>
  * [Target Hardware](#hardware)
    * [Odroid](#odroid)
    * [Android](#android)
    * [Raspberry Pi](#raspberry)
  * [Software Architecture](#architecture)
    * [ANTLR](#antlr)
    * [Configuration](#configuration)
    * [URFD](#geometry)
    * [Dynamixel Servos](#dynamixel)
    * [Open AI](#ai)
  * [Design Considerations](#design)
    * [Control](#control)
    * [Messaging](#messages)
    * [Forward Kinematics](#forward)
    * [Inverse Kinematics](#inverse)
    * [Poses](#poses)
  * [Appendices](#appendices)
    * [Rationale for Kotlin](#whykotlin)
    * [Failures](#failures)


***
## Target Hardware <a id="hardware"></a>
[toc](#table-of-contents)

#### Odroid  <a id="odroid"></a>
The robot's central processor is a single-board Linux computer, an Odroid N2+.

#### Android <a id="android"></a>

A Samsung Galaxy Tablet S8+, model SM-X800 is used as the speech processor. It converts spoken input into command strings used to control the robot. It also converts textual responses from tne robot into audible speech. The tablet is Android version 13, SDK version 33.

#### Raspberry Pi <a id="raspberry"></a>

A Raspberry Pi version 5 serves as a "fine motor controller". For smaller parts, currently eyes and
eyelids.

## Software Architecture <a id="architecture"></a>
[toc](#table-of-contents)

Here is a diagram that shows the major software components.

![Major Software Components](/images/software_components.png)
```                        Development - System Architecture ```

#### ANTLR  <a id="antlr"></a>
*ANTLR* is a parsing framework explained [here](https://www.antlr.org). It is used to convert
streams of tokens from spoken text into commands for the robot.

#### Configuration <a id="configuration"></a>
Robot attributes and connectivity are defined in an .xml file, *bert.xml*. A representative
example is shown below.
The file is read by each of the independent processes, giving them a common understanding
of site-specific parameters and attributes of the robot.

Substitution parameters, @...@, are replaced with site-specific values during the build.
```
<?xml version="1.0" encoding="UTF-8"?>

<!-- This file describes "bert", a poppy-like robot. The identical configuration
     is used for both the server and client-side processes.
 -->
 <?xml version="1.0" encoding="UTF-8"?>

 <!-- This file describes "bert", a robot derived from the Poppy project.
 	 This identical configuration is used for each of the independent processes.
  -->
 <robot>
 	<!-- This first section lists miscellaneous properties. @...@ values are
 	     replaced by the build scripts.
 	-->
 	<property name="name">bert</property>
 	<property name="release">@RELEASE@</property>
 	<property name="date">@DATE@</property>
 	<!--  Socket port used by the bluetooth RFCOMM server  -->
 	<property name="blueserver">11046</property>
 	<!--  Cadence in msecs refers to the record frequency  -->
 	<property name="cadence">1000</property>
 	<!--  Name of machine hosting the server process  -->
 	<property name="hostname">localhost</property>
 	<!--  Used by the terminal  -->
 	<property name="prompt">bert: </property>


  <!-- For the "Command" controller, defines the bert  TCP socket. The device UUID
	     must match the hardcoded value in tablet code, the well-known bluetooth UUID
	     for RFCOMM. -->
	<controller name="command" type="COMMAND"
		socket="bert" hostname="localhost"  port="11046" uuid="1101-0000-2000-8080-00815FAB34FF">
	</controller>
	<controller name="terminal" type="TERMINAL" prompt="bert:"/>

 	<!-- These controllers manage groups of joints. Requests are sent the entire group at once across
 	     a serial connection. The names must exist in the enumeration bert.share.motor.Joint.
 	     There is an upper body group and a lower body group.
     -->
 	<controller name="lower" type="SERIAL">
 		<port  name="lower" device="@PORT_LOWER@" />
 		<joint name="LEFT_ANKLE_Y" type="MX28" id="15" offset="-91" min="60" max="135" orientation="direct" />
 		<joint name="LEFT_HIP_X"   type="MX28" id="11" offset="-8" min="155" max="205" orientation="indirect" />
 		<joint name="LEFT_HIP_Y"   type="MX64" id="13" offset="-18" min="75" max="225" orientation="direct" />
 		<joint name="LEFT_HIP_Z"   type="MX28" id="12" offset="-200" min="-20" max="45" orientation="indirect" />
 		<joint name="LEFT_KNEE_Y"      type="MX28" id="14" offset="-4" min="50" max="180" orientation="indirect" />
 		<joint name="RIGHT_ANKLE_Y"    type="MX28" id="25" offset="-108" min="60" max="135" orientation="indirect" />
 		<joint name="RIGHT_HIP_X"      type="MX28" id="21" offset="16" min="155" max="205" orientation="direct" />
 		<joint name="RIGHT_HIP_Y"      type="MX64" id="23" offset="-18" min="75" max="225" orientation="indirect" />
 		<joint name="RIGHT_HIP_Z"      type="MX28" id="22" offset="-160" min="-20" max="45" orientation="direct" />
 		<joint name="RIGHT_KNEE_Y"     type="MX28" id="24" offset="15" min="50" max="180" orientation="direct" />
 	</controller>
 	<controller name="upper" type="SERIAL">
 		<port  name="upper" device="@PORT_UPPER@" />
 		<joint name="LEFT_ARM_Z"   type="MX28" id="43" offset="0" min="-105" max="45" orientation="indirect" />
 		<joint name="LEFT_ELBOW_Y" type="MX28" id="44" offset="17" min="45" max="180" orientation="direct" />
 		<joint name="LEFT_SHOULDER_X"  type="MX28" id="42" offset="-70" min="90" max="180" orientation="direct" />
 		<joint name="LEFT_SHOULDER_Y"  type="MX28" id="41" offset="-65" min="-120" max="155" orientation="direct" />
 		<joint name="RIGHT_ARM_Z"      type="MX28" id="53" offset="0" min="-105" max="45" orientation="direct" />
 		<joint name="RIGHT_ELBOW_Y"    type="MX28" id="54" offset="4" min="45" max="180" orientation="indirect" />
 		<joint name="RIGHT_SHOULDER_X" type="MX28" id="52" offset="-105" min="90" max="180" orientation="indirect" />
 		<joint name="RIGHT_SHOULDER_Y" type="MX28" id="51" offset="-100" min="-155" max="120" orientation="indirect" />
 		<joint name="HEAD_Y"       type="AX12" id="37" offset="-120" min="-5" max="20" orientation="direct" />
 		<joint name="HEAD_Z"       type="AX12" id="36" offset="-150" min="-30" max="30" orientation="indirect" />
 		<joint name="ABS_X"        type="MX64" id="32" offset="0" min="150" max="210" orientation="direct" />
 		<joint name="ABS_Y"        type="MX64" id="31" offset="16" min="150" max="210" orientation="direct" />
 		<joint name="ABS_Z"        type="MX28" id="33" offset="-160" min="-60" max="60" orientation="direct" />
 		<joint name="BUST_X"       type="MX28" id="35" offset="5" min="150" max="210" orientation="direct" />
 		<joint name="BUST_Y"       type="MX28" id="34" offset="24" min="150" max="210" orientation="direct" />
 	</controller>
 </robot>
```

#### URDF <a id="geometry"></a>
The geometry of the robot is used for trajectory planning, balance and other purposes. It is inspired by the
Unified Robot Description Format](http://wiki.ros.org/urdf/XML) (URDF) file.  A sample, specific to _Poppy_, may be found [here](https://github.com/poppy-project/poppy-humanoid/blob/master/hardware/URDF/robots/Poppy_Humanoid.URDF). We have modified the format freely for our own purposes.

We have added the IMU location and taken other liberties with the standard (e.g. angles in degrees to match
the Dynamixel output). Joint orientations are defined via a unit vector showing the alignment. The axes
form a right-handed rool, pitch, yaw coordinate system as follows:

| Axis | <center>Description</center> |<center>Positive</center>
| :--: | :---------------------- | :-------------: |
| x | Front-to-back | Front |
| y | Side-to-side | Robot's Right |
| z | Vertical | Up |

Note that in order to be symmetric with respect to the vertical centerline, some
joints are defined in an inverse direction. Links are
names of the "bones" between joints. As series of joints
and links form a "limb".
```
<?xml version="1.0" encoding="utf-8"?>
<!--
	Right-handed coordinate system: x positive to front, y positive to right, z positive up
	- axis is unit vector around which joint rotates clockwise
   	- lengths ~ mm, angles ~ degrees -->
<robot name="bert">
	<!-- The Internal Measurement Unit (IMU) is the world-frame origin. Its position is always (0,0,0).
	     This origin is located in the pelvis midway between the hip sockets and back to align with the
	     left/right hip y axis. The orientation of the IMU is with respect to the robot reference frame.
	     It may be altered by setting IMU quaternion roll,pitch,yaw directly and then updating.
	-->
	<imu rpy="0. 0. 0."/>
	<!-- A link is roughly equivalent to a bone. A link connects a source pin (joint)
	     to a joint or end effector. The parent link is a joint (a REVOLUTE link pin).

	    Links form a tree from the IMU terminating in the various end effectors.
		Joint names must match the names in bert.xml.

		Link (end-pin) coordinates are relative to the source pin ~ mm. The y-axis coincides with the joint axis
		with the origin at the mid-point, positive to the left. The link z axis is always perpendicular to the joint
		axis corresponding to the y-axis "home" position, positive up. "home" is where the robot stands straight up.
		The x-axis juts forward perpendicular to the z-axis (positive forward).

		The "rpy" settings (roll, pitch, yaw) define the orientation of the motor or end-effector with respect to
		the source pin. The order of rotations is: roll, pitch, yaw. The "rpy" settings do not affect the location
		of the end-pin, only its orientation. Use right-hand rule to determine direction.
		     roll  - rotation about the x axis
		     pitch - rotation about the y-axis
		     yaw   - rotation about the z-axis

		Joints are named for the robot axis about which they rotate when viewed in the "home" position.

	  -->
	<link side="FRONT">        <!-- root -->
		<joint name="RIGHT_HIP_X"
			   xyz="0.0 22.5417390633467 0.0"  rpy="0. 0. 90." home="180.0"/>
		<joint name="LEFT_HIP_X"
			   xyz="0.0 -22.5417390633466 0.0" rpy="0. 0. 90." home="180.0"/>
		<joint name="ABS_Y"
			   xyz="12.0 0.0 62.0"             rpy="0. 0. 0." home="180.0"/>
		<source joint="IMU" />
	</link>
	<link side="RIGHT">
		<joint name="RIGHT_HIP_Z"          home="0.0"
			   xyz="43.9986 5.0 0.0" rpy="0. 0. 90."/>
		<source joint="RIGHT_HIP_X" />
	</link>
	<link side="RIGHT">
		<joint name="RIGHT_HIP_Y"
			   xyz="0.0 0.0 24.0" rpy="0. 0. 0." home="180.0"/>
		<source joint="RIGHT_HIP_Z" />
	</link>
	<link side="RIGHT">
		<joint name="RIGHT_KNEE_Y"
			   xyz="0.0 0.0 182.0"  rpy="0. 0. 0." home="180.0"/>
		<source joint="RIGHT_HIP_Y" />
	</link>
	<link side="RIGHT">
		<joint name="RIGHT_ANKLE_Y"
			   xyz="0.0 0.0 -180.0"  rpy="0. 180. 0." home="90.0"/>
		<source joint="RIGHT_KNEE_Y"/>
	</link>
	<link side="RIGHT">
		<appendage name="RIGHT_TOE"
				   xyz="95. -15.0 -35.5" rpy="180. 90. 180."/>
		<appendage name="RIGHT_HEEL"
				   xyz="-43.0 12.0 -35.5" rpy="180. 90. 180."/>
		<source joint="RIGHT_ANKLE_Y"/>
	</link>
	<link side="LEFT">
		<joint name="LEFT_HIP_Z"
			   xyz="-43.9986 5.00 0.0" rpy="0. 0. 90." home="180.0"/>
		<source joint="LEFT_HIP_X"/>
	</link>
	<link side="LEFT">
		<joint name="LEFT_HIP_Y"
			   xyz="0.0 0.0 24.0"       rpy="0. 0. 0." home="180.0"/>
		<source joint="LEFT_HIP_Z"/>
	</link>
	<link side="LEFT">
		<joint name="LEFT_KNEE_Y"
			   xyz="0.0 0.0 182.0" rpy="0. 0. 0." home="180.0"/>
		<source joint="LEFT_HIP_Y"  />
	</link>
	<link side="LEFT">
		<joint name="LEFT_ANKLE_Y"
			   xyz="0.0 0.0 -180.0" rpy="0. 180. 0." home="90.0"/>
		<source joint="LEFT_KNEE_Y" />
	</link>
	<link side="LEFT">
		<appendage name="LEFT_TOE"
				   xyz="95. 15.0 -35.5"  rpy="180. 90. 180."/>
		<appendage name="LEFT_HEEL"
				   xyz="-43. -12.0 -35.5" rpy="180. 90. 180."/>
		<source joint="LEFT_ANKLE_Y" />
	</link>
	<link side="FRONT">
		<joint name="ABS_X"
			   xyz="0.0 0.0 8.0" rpy="0.0 180.0 90.0" home="180.0"/>
		<source joint="ABS_Y"/>
	</link>
	<link side="FRONT">
		<joint name="ABS_Z"
			   xyz="0.0 12.0 51.6374742" rpy="90. 90.0 0.0" home="0.0"/>
		<source joint="ABS_X" />
	</link>
	<link side="FRONT">
		<joint name="CHEST_Y"
			   xyz="-2.8 79.85 0.0"  rpy="-90. 90. 90." home="180.0"/>
		<source joint="ABS_Z" />
	</link>
	<link side="FRONT">
		<joint name="CHEST_X"
			   xyz="0.0 8.0 0.0" rpy="90. 180. -90." home="180.0"/>
		<source joint="CHEST_Y"/>
	</link>
	<link side="FRONT">
		<joint name="NECK_Z"
			   xyz="-5.0 0.0 84.0"    rpy="90. -90. 0." home="180.0"/>
		<joint name="LEFT_SHOULDER_Y"
			   xyz="-4.0 -77.1 50.0"  rpy="0. 0. 90." home="0.0"/>
		<joint name="RIGHT_SHOULDER_Y"
			   xyz="-4.0 77.1 50.0"  rpy="0. 0. 90." home="0.0"/>
		<source joint="CHEST_X" />
	</link>
	<link side="FRONT">
		<joint name="NECK_Y"        home="0.0"
			   xyz="-0.0 20.0 20.0"  rpy="-90. 0. -90."/>
		<source joint="NECK_Z"/>
	</link>
	<link side="FRONT">
		<appendage name="LEFT_EYE"
				   xyz="0.0 32.0 55.0" rpy="0. 0. 0."/>
		<appendage name="RIGHT_EYE"
				   xyz="0.0 -32.0 55.0"  rpy="0. 0. 0."/>
		<appendage name="LEFT_EAR"
				   xyz="-30.0 62.0 55.0" rpy="0. 0. 0."/>
		<appendage name="RIGHT_EAR"
				   xyz="-30.0 -62.0 55.0"/>
		<appendage name="NOSE"
				   xyz="15.0 0.0 40.0" rpy="0. 0. 0."/>
		<source joint="NECK_Y"  />
	</link>
	<link side="LEFT">
		<joint name="LEFT_SHOULDER_X"
			   xyz="-28.4 0.0 0.0" rpy="0. 90. 90." home="180.0"/>
		<source joint="LEFT_SHOULDER_Y" />
	</link>
	<link side="LEFT">
		<joint name="LEFT_SHOULDER_Z"
			   xyz="0.0 18.5 -83.0"  rpy="0. 90. 90." home="180.0"/>
		<source joint="LEFT_SHOULDER_X" />
	</link>
	<link side="LEFT">
		<joint name="LEFT_ELBOW_Y"
			   xyz="81.175 0.0 10.0"  rpy="0. 90. 180." home="180.0"/>
		<source joint="LEFT_SHOULDER_Z" />
	</link>
	<link side="LEFT">
		<appendage name="LEFT_FINGER"
				   xyz="4.99 -6.915 -121.0" rpy="0. 90. 0.0"/>
		<source joint="LEFT_ELBOW_Y" />
	</link>
	<link side="RIGHT">
		<joint name="RIGHT_SHOULDER_X"
			   xyz="28.4 0.0 0.0" rpy="0. 90. 90." home="0.0"/>
		<source joint="RIGHT_SHOULDER_Y"/>
	</link>
	<link side="RIGHT" >
		<joint name="RIGHT_SHOULDER_Z"
			   xyz="0.0 18.5 83.0" rpy="0. 90. 90." home="180.0"/>
		<source joint="RIGHT_SHOULDER_X"/>
	</link>
	<link side="RIGHT">
		<joint name="RIGHT_ELBOW_Y"
			   xyz="81.175 0.0 10.0" rpy="0. 90. 180." home="180.0"/>
		<source joint="RIGHT_SHOULDER_Z"/>
	</link>
	<link side="RIGHT" >
		<appendage name="RIGHT_FINGER"
				   xyz="4.99 6.915 -121.0" rpy="-90. 90. -90.0"/>
		<source joint="RIGHT_ELBOW_Y" />
	</link>
</robot>
```
Some dimensions were taken from the [Poppy URDF](https://github.com/poppy-project/poppy-humanoid/blob/master/hardware/URDF/robots/Poppy_Humanoid.URDF), others were measured directly.

Analysis of this file, results in a description of the robot as a tree or
"chain" of links. The tree has a single origin in the center of the pelvis. There are
multiple terminations or "end effector" (e.g. finger or toe). The links represent limbs of the body (e.g "thigh").
A quaternion transform matrix is calculated for each link, describing its orientation and size with respect to
the parent joint.

Calculation of end-effector location is accomplished via Quaternion multiplication up the chain.

#### Dynamixel Servos  <a id="dynamixel"></a>
The control motors are Dynamixel MX-64, MX-28 and AT-12A models from [Robotis](http://en.robotis.com).
The servos feature their own PID control. A single write of a target position, speed and torque is all that is
required for control. There is no need for a constant refresh action.
A further efficiency is provided by the *SYNC WRITE* directive. This allows control of multiple motors
with a single command, as long as the affected motors are daisy-chained on the same serial port. A
*SYNC WRITE* command does not return status.

We use version 1.0 of the protocol as the motors were delivered with that version.

Idiosyncrasies:<br/>
  * There appears to be a maximum write frequency, especially for the AT-12A motors. We guarantee at least 50ms between consecutive writes to the same serial port. The symptom is dropped responses.
  * There is no guarantee that responses from the serial port correlate 1:1 with the requests. The
   response buffer can hold partial replies or several replies combined.

#### Open AI  <a id="ai"></a>
 The interface with [Chat GPT](https://openai.com/chatgpt/overview)  is incidental to the operation
 of the robot. It is used simply to add completion to queries or commands that would be otherwise not understood
 by the robot. In order to use the AI interface you must create an account and obtain a key from from `Open AI`
 and place it in the environment on the robot (e.g. in ~/.bashrc) as CHATGPT_KEY.

## Design Considerations <a id="design"/>

#### Control <a id="control"/>
[toc](#table-of-contents)

The main robot process is a Kotlin application running on the Odroid processor.
Major components, _terminal_,_command_, and _dispatcher_ are Kotlin co-routines
communicating over Channels. The messages passed back and forth between these processes are instances of class _MessageBottle_.
Communication with the tablet process is via text messages across a Wi-Fi
socket.

There are numerous situations in the robot where a single user command results in multiple motor commands internally. For example, whan the robot has been relaxed,
all positional information is lost. So when re-enabling torque, the joint angle information must be re-read to keep it up-to-date.
Another example is *pose* commands that are split into separate
directives for each limb.

Moreover, it is important tnat the ``MotorController`` not be given a command until the serial callback of the previous has been received.
A new request must not
submitted until the ``MotorConrtoller`` finishes with the previous request.
The sequencing function as well as command expansion is handled by an ``InternalController`` which queues requests and inserts required supporting requests as necessary.


#### Messaging<a id="messages"></a>
The tablet executes a Wi-fi client based on Java classes that are a standard part of the Android SDK. It connects to the robot process via sockets.

Messages that are intended to be spoken or logged are prepended
with a 4-character header followed by ASCII text.
Messages have a 1K-byte
size limit and are stripped of any trailing whitespace except for a terminating new-line.

More complicated messages intended for inter-process communication have
a similar header followed by a string indicating type and then JSON
text.

Header values are described in the table below:

| Header	| <center>Description</center>|
| -------- | :------------------------------------------------------------------ |
|MSG:|	Message from the tablet to the robot. Plain english request or query.	|
|ANS:|	Reply from the robot. This text is meant to be spoken and appear on the transcription log. |
|JSN:|	Communicate via JSON objects. These messages direct robot-tablet communication, not meant for enunciation.	|
|LOG:|	System message from the robot meant to appear on the tablet's log panel. |
|TBL:|	Define a table to show on the tablet's table panel. Pipe-delimited fields contain: title, column headings. |


Several messages may be buffered in the same socket transmission to or from the tablet. However, the
sender appends a new-line to each message to enable both the tablet and robot
Kotlin code to use a custom `readline()` routine to parse one message at a time.

#### Forward Kinematics <a id="forward"></a>
Forward kinematics refers to the calculation of the 3D position of joints and end effectors based
on the connections of skeletal components and joint angles. The calculations are derived from
<ins>Introduction to Robotics</ins>  by S. K. Saha and
facilitated by the blog post [How to Calculate a Robot's Forward Kinematics in 5 Easy Steps](https://blog.robotiq.com/how-to-calculate-a-robots-forward-kinematics-in-5-easy-steps) by Alex Owen-Hill.

Refering to the URDF.xml file (listed above) *link* elements:

| Parameter	| <center>Description</center> |
| -------- | :------------------------------------------------------------------ |
|d| distance from the link's source pin to the REVOLUTE center in the z direction. |
|r| length of the common normal between source and link center z axes|
|alpha| the angle along the common normal between previous and current z axis |
|theta| the angle of the limb x axis with respect to the source pin|
|xyz| coordinates from source to REVOLUTE center |

Lengths are ~mm, angles in degrees. The z direction is always the axis of rotation of the motor.

#### Inverse Kinematics <a id="inverse"></a>
Optimal trajectory planning, such as the Poppy example [here]
(https://github.com/Phylliade/ikpy/tree/master/src/ikpy) by Pierre Manceron is known to be complex and slow. We go to great lengths to avoid it.

#### Poses <a id="poses"></a>
A "pose" is a position of the robot as a whole, comprising settings for each of its joints. Poses may be
generated offline or as a result of recording joint values when the robot has been manually positioned for some
purpose.  For each joint, the pose records torque, speed and position. Torque and speed are in percent
of maximum and position is in degrees. Torque is a measure of how pliant the joint is. Speed refers to
how fast the motor will move to the desired position. All values are integers. Null values
imply that the current setting will not be changed. Not all parameters are required.

| Name	| Parameter	| ABS_X	| ABS_Y |	ABS_Z ...
| -------- | --------- | ------- | ------ | ------
|relaxing|	torque	|0	|0
|standing|	position|	0	|90
|standing|	speed	|25|	null
|standing|	torque	|100|	75

``Partial View of a Sample "Pose" Table``

In the sample database table shown above, only 3 of the 25 actual joints are shown. The "relaxed" pose contains
only the single line to set torque to a zero setting. This effectively renders the robot limp.
The "standing" setting defines all three parameters. Note that the ABS_Y speed is null. This
means that for ABS-Y the speed at which it travels to 90 degrees is the same as the last
time the joint was used. On power-up speeds are 100% and torques are 0%. Unless specified in
the pose, torques are automatically set to 100% whenever a joint is moved.

When moving to a set pose, it is usually sufficient to simply use that pose as a goal and move
to it directly using a single command to the servos. For the most part, collision conflicts are
avoided by limits on the angular motion of the joints.  For situations not covered in this way, we have adopted a simple set of heuristic tests that are applied prior to each movement. In general,
these heuristics insert intermediate poses to avoid conflicts en route or truncate motion when the goal
is itself a conflict. We avoid a full trajectory optimization.

*** Raspberry Pi ***</br>
An additional processor, a Raspberry Pi, is placed in the
cranium on top of the main Odroid processor. The RPi
is a "fine motor controller" used for control of smaller
appendages, e.g. eyes.

## Appendices <a id="appendices"></a>
[toc](#table-of-contents)
#### Why Kotlin?<a id="whykotlin"></a>
In the "Poppy" [thesis](https://hal.inria.fr/tel-01104641v1/document) (section 7.4.1 and following), the author considers use of the Robot Operating System (ROS) for the core software and concludes that it is overly complex and inefficient. This coincides with my own experience with [sarah-bella](https://github.com/chuckcoughlin/sarah-bella). Moreover, I discovered that, at least with Android, ROS messaging was not reliable. Messages were dropped under high load, a situation not acceptable for control applications.

This same author noted that the "Pypot" software developed in Python for Poppy had severe performance limitations that placed strict limits on its design.

While I can't simply try all the designs, [YARP](http://www.yarp.it/index.html) used by the "iCub" project seemed closest to my perceived needs. It is written in C++ and is loaded with features that I'll never use. Given my failure to create a cross-compilation environment, I've decided to let a simplified *YARP* serve as an inspiration for a Kotlin-based solution. For simplicity, the ancillary code will be Kotlin also.

The previous version was written entirely in Java. However I decided to
write the current iteration of the project in Kotlin for the following reasons:
  * The Android platform identifies Kotlin as a core language
  * The threading model seems much more straightforward
  * It is possible to automatically convert Java to Kotlin
  * It was time for me to learn something new

Why did I select Java for the original project when the iCub project chose Python?
  * Familiarity - over 2 decades of working with Java
  * Refactoring - the refactoring capabilities of IntelliJ are unparalleled, in my opinion, and make code re-structuring quick, easy and accurate
  * Code-Debug-Cycle - problems are more likely to be discovered by the compiler rather than run-time as with Python
  * Debugging - via IntelliJ we can set breakpoints and inspect run-state not only locally, but on the target robot
  * Performance - Java executes an order of magnitude faster than Python
  * Threading - the Java threading model is more straightforward (IMHO)
  * Cross-compiling - difficulties creating Odroid executables precluded my use of C++.

### Failures <a id="failures"></a>
[toc](#table-of-contents)

This section documents ideas that were tried and abandoned. The implication here is not
so much that these ideas won't work, but rather that I couldn't get them to work or found
more expedient alternatives.

*** Gradle *** <br/>
``Gradle`` is a modern tool for building software of several flavors.  To install ``Gradle`` use ``homebrew``:
```
     brew install gradle
     brew install gradle-completion
```
I totally failed to create a C++ build script for cross-compilation (Mac to Odroid). Moreover after nearly a week of trying, I gave up on the Java building also. I was unable to create an environment to conveniently share re-usable code among separate top level targets. I'm sure it can be done, I just lost interest. *ant* does everything I need.

*** Cross-compilation *** <br/>
On the build machine, I downloaded [GNU Embedded Toolchain for ARM]( https://developer.arm.com/open-source/gnu-toolchain/gnu-rm/downloads), then followed the installation steps outlined [here](https://gnu-mcu-eclipse.github.io/toolchain/arm/install/#macos-1), Using a *makefile* and these tools, I compiled C++ code for the Odroid X64. It had a bad format and didn't execute. I gave up on C++ in favor of Java for the core control loop.

*** Cmake *** <br/>
The [TinyB](https://github.com/intel-iot-devkit/tinyb ) library for Bluetooth integration came with build files for *cmake*.
I was unable to get a successful compilation of the JNI code on the Odroid. I followed the directions [here](http://fam-haugk.de/starting-with-bluetooth-le-on-the-raspberry-pi) and [here](http://www.martinnaughton.com/2017/07/install-intel-tinyb-java-bluetooth.html) with
modifications for the Odroid and Java11. With these installs which literally took
hours and used up over 10% of available disk space,  the _cmake_ build still never completed successfully.
```
sudo apt-get install libgtk2.0-dev
sudo apt-get install pkg-config
sudo apt-get --purge autoremove cmake
sudo apt-get install build-essential
cd ~
wget http://www.cmake.org/files/v3.14/cmake-3.14.0.tar.gz
tar -xzf cmake-3.14.0.tar.gz
cd cmake-3.14.0
./configure
make
sudo make install
hash -r

export JAVA_AWT_LIBRARY=$JAVA_HOME/lib/libawt.so
export JAVA_JVM_LIBRARY=$JAVA_HOME/lib/server/libjvm.so
export JAVA_INCLUDE_PATH=$JAVA_HOME/include
export JAVA_INCLUDE_PATH2=$JAVA_HOME/include/linux
export JAVA_AWT_INCLUDE_PATH=$JAVA_HOME/include
```
Since the purpose of all this was to generate a Makefile of about 50 lines, I
did it the old-fashioned way and edited it by hand, to use `make` directly.

*** Herborist *** </br>

Herborist is a tool for configuring Dynamixel servos. The [toolchain](https://github.com/chuckcoughlin/bert/tree/master/docs/toolchain.md) document describes an Ubuntu installation using *pip*, but, for me, the install of the PyQt5 package failed and, subsequently, the *herborist* install. As a workaround, I copied the code (pure python) and ran it directly.
```
   cd
   cd src/herborist
   python3 setup.py build
   sudo chmod 777 /usr/local/lib/python3.10/dist-packages
   sudo chmod 666 /usr/local/lib/python3.10/dist-packages/easy-install.pth
   sudo chmod 777 build.bdist.linux-aarch64
   sudo chmod 777 /usr/local/bin
   python3 setup.py install
```
This failed to run, also. My workaround is to use a command-line tool in the *PyPot* distribution for servo
configuration.

*** JBlueZ ***<br/>
JBlueZ](http://jbluez.sourceforge.net/)
by Edward Kay is a minimalist
implementation of a Bluetooth interface. Its intent is to use JNI and link directly
to _libbluetooth.so_. However, it requires J2ME which is not the Java on the Odroid.
It also relies on _DBus_ which is yet-another-interface to learn and debug. It
also comes with a GPL license.

The version1 code had this working, but broke with the current Bluetooth version (BlueZ 5.48). Instead of
debugging yet again, we elected to drop Bluetooth communication in favor simple socket-socket communication
over WiFi. The realization that a WiFi network could be established anywhere with a cell phone "local hotspot"
removed the need for bluetooth between robot and tablet.

Programmatic access to Bluetooth requires a interface to the Odroid's
bluetooth library `libbluetooth.so` .
An excellent introduction is a book by Albert Huang
of MIT published [here](http://people.csail.mit.edu/albert/bluez-intro/).
 Starting with Albert's examples and relying
heavily on lessons learned trying to implement the various packages
mentioned in my "Failures" section (See [Software Architecture](http://github.com/chuckcoughlin/bert/tree/master/docs/architecture.md)), I developed a custom, minimalist
daemon using RFCOMM. It communicates with the Kotlin application over sockets and
the tablet via Bluetooth.
Its sole purpose is to transfer strings between the tablet and the Odroid robot application.
The tablet uses standard Android Bluetooth classes.

On the development machine, in the _IntelliJ_ Configuration project, the ``install_odroid_source`` script
copies C source files onto the Odroid in preparation for building _blueserverd_, the
daemon, and _blueserver_, an interactive test application. (This script may have to be modified for
the correct robot home directory on the Odroid).

Then to build on the Odroid, from the directory containing the source projects -
```
  cd blueserver
  make -e
  make install
  cd ${BERT_HOME}/bin
  sudo ./install_blueserver_init_scripts
```

Configure Bluetooth using the robot's pull-down menu. Configure the adapter so that it is always visible (discoverable) and give it a "friendly"
name of "bert". Pairing is based on the friendly name and can be initiated
either from the robot or the tablet. The ```bluetoothctl``` tool is available for command-line configuration.

Add the following lines to _/etc/dbus-1/system.d/bluetooth.conf_ under ``<policy context="default">``:

```
  <allow send_interface="org.bluez.GattService1"/>
  <allow send_interface="org.bluez.GattCharacteristic1"/>
  <allow send_interface="org.bluez.GattDescriptor1"/>
```

Note that _blueserver.h_ has the bluetooth address of the tablet hard-coded. On the tablet
the device should be configured as ``bert``. On the Odroid the adapter is also known as ``bert``.

The init script launches the _blueserverd_ daemon. Connection difficulties may arise if too many bluetooth-enabled
devices are in range leading to incorrect pairings. If so, the Odroid system may report
"DbusFailedError: host is down".

*** TinyB *** </br>
[TinyB](https://github.com/intel-iot-devkit/tinyb ) is a library and Java classes for Bluetooth LE communication.
 The original distribution builds with `cmake`, but I was never
able to get that working. I just created a custom `Makefile` to build the code.
and integrate it with the main robot application.

Unfortunately when loading the JNI library ...
```
      UnsatisfiedLinkError:  g_cclosure_marshall_generic
        in tinyb.BluetoothManager.getNativeAPIVersion
          BluetoothManager.getBluetoothManager
```
The underlying issue is that the missing method was introduced in libc6 2.30.
The Odroid has 2.27. I get this error on all native methods. I assume it is a
result of using C++ in the interface library.

*** Bluecove ***</br>
*BlueCove* is yet another Bluetooth library available from [here](https://code.google.com/archive/p/bluecove/wikis/Documentation.wiki). A well written example for a simple connection is written by [Luu Gia Thuy](
https://github.com/luugiathuy/Remote-Bluetooth-Android/blob/master/RemoteBluetoothServer/src/com/luugiathuy/apps/remotebluetooth)

Unfortunately the project jar files contained too many dependencies to modularize into Java 10 jars.
I decided to build the jar from scratch, adding classes as necessary. I converted logging to java.util.logger (removing _log4j_ dependencies). I removed the Java dependency on J2ME.

The standard jar files included the "stacks", native library code for different target architectures,
though not the one we needed, for ARM. Consequently I had to build from source using code
that brought on a GPL dependency.

This just got too complicated.

*** GATT *** <br/>
Since we don't need a full-featured Bluetooth interface, the idea was to simplify things
considerably by making use of GATT (Generic Atributes)
Bluetooth profiles. The only characteristics we require are two to read and write
simple strings. Following instructions [here](https://stackoverflow.com/questions/25427768/bluez-how-to-set-up-a-gatt-server-from-the-command-line), we attempted to create a GATT server on the Odroid using
 _bluetoothctl_. The reported syntax for setting service characteristics in _bluetoothctl_
 didn't work and I was never successful in creating a registration.
