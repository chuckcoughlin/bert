# High-level Design and Software Architecture

Control software embedded in the "Bert" project  takes its inspiration from two sources: [Poppy](https://www.poppy-project.org) by
[GenerationRobots](https://www.generationrobots.com/en/278-poppy-humanoid-robot) and the [iCub Project](http://www.icub.org/bazaar.php) from the [Italian Institute of Technology](https://www.iit.it). Our implementation deviates significantly, however. The core language is Java instead of C++ and Python. The feature-set has been greatly simplified - and we have added natural-language speech processing with the integration of an Android tablet.

*Poppy* version 1.0.2 source is at: https://github.com/poppy-project/poppy-humanoid. The project is described in detail in the thesis ["Poppy: open-source, 3D printed and fully-modular
robotic platform for science, art and education"](https://hal.inria.fr/tel-01104641v1/document) by Matthieu Lapeyre. A full list of project authors may be found at: https://github.com/poppy-project/poppy-humanoid/doc/authors.md.

The *iCub* main project repository is at: https://github.com/robotology/icub-main. At its core, *iCub* uses Yet Another Robot Platform [(YARP)](http://www.yarp.it/).

The bulk of this document addresses various design issues and approaches to their solutions.

***
## Table of Contents <a id="table-of-contents"></a>

  * [Software Architecture](#architecture)
    * [ANTLR](#antlr)
    * [Configuration](#configuration)
    * [Dynamixel Servos](#dynamixel)
    * [Interprocess Communication](#sockets)
  * [Design Considerations](#design)
    * [Message Structure](#messages)
    * [Poses](#poses)
    * [Trajectory Planning](#trajectory)
  * [Appendices](#appendices)
    * [Rationale for Java](#whyjava)
    * [Failures](#failures)
***


***
## Software Architecture <a id="architecture"/>

Here is a diagram that shows the major software components.
![Major Software Components](/images/software_components.png)
````                        Development - System Architecture ````

#### ANTLR  <a id="antlr"/>
*ANTLR* is a parsing framework explained [here](https://www.antlr.org/). It is used to convert
streams of tokens from spoken text into commands for the robot.

#### Configuration <a id="configuration"/>
The robot configuration is described by an .xml file, *bert.xml*. A representative
example is shown below.
The file is read by each of the independent processes, giving them a common understanding
of site-specific parameters and attributes of the robot.

This file (and not the URDF) is the source of standard joint names and angular limits.
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


 	<!-- The following section defines client-side processes that are also known to the server.
 		 Each client process communicates in both directions over its own socket.
 		 Device UUID for Bluetooth - must match hardcoded value in tablet code. This is
		 the well-known UUID for RFCOMM.
 	-->
 	<controller name="terminal" type="TERMINAL">
 		<socket  name="terminal" port="11044"/>
 	</controller>
 	<controller name="command" type="COMMAND">
 		<socket  name="command" port="11045"/>
 		<socket  type="bluetooth"  uuid="33001101-0000-2000-8080-00815FAB34FF"/>
 	</controller>

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

#### Dynamixel Servos  <a id="dynamixel"/>
The control motors are Dynamixel MX-64, MX-28 and AT-12A models from [Robotis](http://en.robotis.com).
The servos feature their own PID control. A single write of a target position, speed and torque is all that is
required for control. There is no need for a constant refresh action.

A further efficiency is provided by the *SYNC WRITE* directive. This allows control of multiple motors
with a single command, as long as the motors are daisy-chained on the same serial port.

We use version 1.0 of the protocol as the motors were delivered with that version.

#### Geometry <a id="geometry"/>
The geometry of the robot is used for trajectory planning, balance and other purposes. It is described in a
Unified Robot Description Format](http://wiki.ros.org/urdf/XML) (URDF) file. A series of tutorials concerning its construction may be found [here](http://wiki.ros.org/urdf/Tutorials). A sample is shown below:
```
  URFD
```

Upon analysis of this file, the robot is described as a collection
of "chains" of links. The chains have a common origin at the pelvis and terminate at an
end effector (e.g. hand or foot).


#### Interprocess Communication <a id="sockets"/>
The major components, _terminal_,_command_, and _dispatcher_ are independent linux processes and communicate via sockets. Port numbers are defined in the configuration file. There is an additional daemon process, _blueserver_, that serves as an interface between the _command_ process and the Bluetooth Serial Port service.

The tablet is a Bluetooth client based on Java classes that are a standard part of the Android SDK.

## Design Considerations <a id="design"/>
[toc](#table-of-contents)
#### Message Structure <a id="messages"/>
*** Internal ***</br>
The messages passed back and forth to the _dispatcher_ process are instances of class _MessageBottle_.
In preparation for transmission across the socket connections, the messages are serialized into JSON
strings and, of course, reconstituted on the other side.

*** Tablet ***</br>
Messages transmitted to and from the tablet are strings with a simple 4-character header and a 1K-byte
size limit. Header values are described in the table below:

| Header	| <center>Description</center>|
| -------- | :------------------------------------------------------------------ |
|MSG:|	Message from the tablet to the robot. Plain english request or query.	|
|ANS:|	Reply from the robot. This text is meant to be spoken and appear on the transcription log. |
|LOG:|	System message from the robot meant to appear on the tablet's log panel. |
|TBL:|	Define a table to show on the tablet's table panel. Pipe-delimited fields contain: title, column headings. |
|ROW:|	Append a row to the most-recently defined table. Pipe-delimited fields contain cell values.	|
<center>``Tablet Message Structure``</center>

#### Poses <a id="poses"/>
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
<center>``Partial View of a Sample "Pose" Table``</center>

In the sample database table shown above, only 3 of the 25 actual joints are shown. The "relaxed" pose contains
only the single line to set torque to a zero setting. This effectively renders the robot limp.
The "standing" setting defines all three parameters. Note that the ABS_Y speed is null. This
means that for ABS-Y the speed at which it travels to 90 degrees is the same as the last
time the joint was used. On power-up speeds are 100% and torques are 0%. Unless specified in
the pose, torques are automatically set to 100% whenever a joint is moved.

#### Trajectory Planning <a id="trajectory"/>
Optimal trajectory planning, such as the Poppy example [here](https://github.com/Phylliade/ikpy/tree/master/src/ikpy) by Pierre Manceron is known to be complex and slow. We go to great lengths to avoid it.

When moving to a set pose, it is usually sufficient to simply use that pose as a goal and move
to it directly using a single command to the servos. For the most part, collision conflicts are
avoided by limits on the angular motion of the joints.  For situations not covered in this way, we have adopted a simple set of heuristic tests that are applied prior to each movement. In general,
these heuristics insert intermediate poses to avoid conflicts en route or truncate motion when the goal position is in itself a conflict. We avoid a full trajectory optimization. The list of checks is as follows:
A full trajectory optimization is not necessary.



## Appendices <a id="appendices"/>
[toc](#table-of-contents)
#### Why Java?<a id="whyjava"/>
In the "Poppy" [thesis](https://hal.inria.fr/tel-01104641v1/document) (section 7.4.1 and following), the author considers use of the Robot Operating System (ROS) for the core software and concludes that it is overly complex and inefficient. This coincides with my own experience with [sarah-bella](https://github.com/chuckcoughlin/sarah-bella). Moreover, I discovered that, at least with Android, ROS messaging was not reliable. Messages were dropped under high load, a situation not acceptable for control applications.

This same author noted that the "Pypot" software developed in Python for Poppy had severe performance limitations that placed strict limits on its design.

While I can't simply try all the designs, [YARP](http://www.yarp.it/index.html) used by the "iCub" project seemed closest to my perceived needs. It is written in C++ and is loaded with features that I'll never use. Given my failure to create a cross-compilation environment, I've decided to let a simplified *YARP* serve as an inspiration for a Java-based solution. For simplicity, the ancillary code will be Java also.

Why did I select Java for this code when the iCub project chose Python?
  * Familiarity - over 2 decades of working with Java
  * Refactoring - the refactoring capabilities of Eclipse are unparalleled, in my opinion, and make code re-structuring quick, easy and accurate
  * Code-Debug-Cycle - problems are more likely to be discovered by the compiler rather than run-time as with Python
  * Debugging - via Eclipse we can set breakpoints and inspect run-state not only locally, but on the target robot
  * Performance - Java executes an order of magnitude faster than Python
  * Threading - the Java threading model is more straightforward (IMHO)
  * Cross-compiling - difficulties creating Odroid executables precluded my use of C++.

### Failures <a id="failures"/>
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

*** JBlueZ ***<br/>
JBlueZ](http://jbluez.sourceforge.net/)
by Edward Kay is a minimalist
implementation of a Bluetooth interface. Its intent is to use JNI and link directly
to _libbluetooth.so_. However, it requires J2ME which is not the Java on the Odroid.
It also relies on _DBus_ which is yet-another-interface to learn and debug. It
also comes with a GPL license.

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
