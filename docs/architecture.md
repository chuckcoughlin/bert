# Software Architecture

Control software embedded in the "Bert" project  takes its inspiration from two sources: [Poppy](https://www.poppy-project.org) by
[GenerationRobots](https://www.generationrobots.com/en/278-poppy-humanoid-robot) and the [iCub Project](http://www.icub.org/bazaar.php) from the [Italian Institute of Technology](https://www.iit.it). Our implementation deviates significantly, however. The core language is Java instead of C++ and Python; the feature-set has been greatly simplified; we have added natural-language speech processing with the integration of an Android tablet.

*Poppy* version 1.0.2 source is at: https://github.com/poppy-project/poppy-humanoid. The project is described in detail in the thesis ["Poppy: open-source, 3D printed and fully-modular
robotic platform for science, art and education"](https://hal.inria.fr/tel-01104641v1/document) by Matthieu Lapeyre. A full list of project authors may be found at: https://github.com/poppy-project/poppy-humanoid/doc/authors.md.

The *iCub* main project repository is at: https://github.com/robotology/icub-main. At its core, *iCub* uses Yet Another Robot Platform [(YARP)](http://www.yarp.it/).

***
## Table of Contents <a id="table-of-contents"></a>

  * [Software Architecture](#architecture)
    * [ANTLR](#antlr)
    * [Configuration](#configuration)
    * [Interprocess Communication](#sockets)
  * [Appendices](#appendices)
    * [Rationale for Java](#whyjava)
    * [Failures](#failured)
***





***
## Software Architecture <a id="architecture"/>

Here is a diagram that shows the major software components.
![Major Software Components](/images/software_components.png)
````                        Development - System Architecture ````

#### ANTLR  <a id="antlr"/>
*ANTLR* is a parsing framework.

#### Configuration <a id="configuration"/>
The entire robot configuration is described by an .xml file as shown below.
The file is read by both client and server-side processes. This allows the
independent processes to have a common understanding of the parameters.
```
<?xml version="1.0" encoding="UTF-8"?>

<!-- This file describes "bert", a poppy-like robot. The identical configuration
     is used for both the server and client-side processes.
 -->
<robot>
	<!-- This first section lists miscellaneous properties. @...@ values are
	     replaced by the build scripts.
	-->
	<property name="name">bert</property>
	<property name="release">@RELEASE@</property>
	<property name="date">@DATE@</property>
	<!--  Cadence in msecs refers to the record frequency  -->
	<property name="cadence">1000</property>
	<!--  Used by the terminal  -->
	<property name="prompt">bert: </property>

	<!-- The following section defines client-side applications that are also known to the server.
		 Each application communicates in both directions over its own socket.
	-->
	<controller name="terminal" type="TERMINAL">
		<socket  port="9044"/>
	</controller>
	<controller name="command" type="COMMAND">
		<socket  port="9045"/>
	</controller>

	<!-- These controllers manage groups of joints. Requests are sent the entire group at once across
	     a serial connection. The names must exist in the enumeration bert.share.motor.Joint.
    -->
	<controller name="torso" type="SERIAL">
		<port  name="torso" device="@PORT_TORSO@" />
		<joint name="ABS_X"        type="MX64" id="32" offset="0" min="-45" max="45" orientation="indirect" />
		<joint name="ABS_Y"        type="MX64" id="31" offset="0" min="-50" max="12" orientation="indirect" />
		<joint name="ABS_Z"        type="MX28" id="33" offset="0" min="-90" max="90" orientation="direct" />
		<joint name="BUST_X"       type="MX28" id="35" offset="0" min="-40" max="40" orientation="indirect" />
		<joint name="BUST_Y"       type="MX28" id="34" offset="0" min="-67" max="27" orientation="indirect" />
	</controller>
	<controller name="left_leg" type="SERIAL">
		<port  name="left_leg" device="@PORT_LEFT_LEG@" />
		<joint name="LEFT_ANKLE_Y" type="MX28" id="15" offset="0" min="-45" max="45" orientation="direct" />
		<joint name="LEFT_HIP_X"   type="MX28" id="11" offset="0" min="-30" max="28.5" orientation="direct" />
		<joint name="LEFT_HIP_Y"   type="MX64" id="13" offset="0" min="-104" max="84" orientation="direct" />
		<joint name="LEFT_HIP_Z"   type="MX28" id="12" offset="0" min="-25" max="90" orientation="indirect" />
		<joint name="LEFT_KNEE_Y"      type="MX28" id="14" offset="-90" min="-3.5" max="134" orientation="direct" />
	</controller>
	<controller name="right_leg" type="SERIAL">
		<port  name="right_leg" device="@PORT_RIGHT_LEG@" />
		<joint name="RIGHT_ANKLE_Y"    type="MX28" id="25" offset="0" min="-45" max="45" orientation="indirect" />
		<joint name="RIGHT_HIP_X"      type="MX28" id="21" offset="0" min="-28.5" max="30" orientation="direct" />
		<joint name="RIGHT_HIP_Y"      type="MX64" id="23" offset="0" min="-85" max="105" orientation="indirect" />
		<joint name="RIGHT_HIP_Z"      type="MX28" id="22" offset="0" min="-90" max="25" orientation="indirect" />
		<joint name="RIGHT_KNEE_Y"     type="MX28" id="24" offset="0" min="-134" max="3.5" orientation="indirect" />
	</controller>
	<controller name="left_arm" type="SERIAL">
		<port  name="left_arm" device="@PORT_LEFT_ARM@" />
		<joint name="LEFT_ARM_Z"   type="MX28" id="43" offset="0" min="-105" max="105" orientation="indirect" />
		<joint name="LEFT_ELBOW_Y" type="MX28" id="44" offset="0" min="-148" max="1" orientation="direct" />
		<joint name="LEFT_SHOULDER_X"  type="MX28" id="42" offset="90" min="-105" max="110" orientation="indirect" />
		<joint name="LEFT_SHOULDER_Y"  type="MX28" id="41" offset="0" min="-120" max="155" orientation="direct" />
	</controller>
	<controller name="right_arm" type="SERIAL">
		<port  name="right_arm" device="@PORT_RIGHT_ARM@" />
		<joint name="RIGHT_ARM_Z"      type="MX28" id="53" offset="0" min="-105" max="105" orientation="indirect" />
		<joint name="RIGHT_ELBOW_Y"    type="MX28" id="54" offset="0" min="-1" max="148" orientation="indirect" />
		<joint name="RIGHT_SHOULDER_X" type="MX28" id="52" offset="90" min="-110" max="105" orientation="indirect" />
		<joint name="RIGHT_SHOULDER_Y" type="MX28" id="51" offset="90" min="-155" max="120" orientation="indirect" />
	</controller>
	<controller name="head" type="SERIAL">
		<port  name="head" device="@PORT_HEAD@" />
		<joint name="HEAD_Y"       type="AX12" id="37" offset="20" min="-45" max="6" orientation="indirect" />
		<joint name="HEAD_Z"       type="AX12" id="36" offset="0" min="-90" max="90" orientation="direct" />
	</controller>
</robot>
```

#### Interprocess Communication <a id="sockets"/>
The major components are all independent linux processes. They communicate via sockets. Port numbers are defined in the configuration file.


## Appendices <a id="appendices"/>
#### Why Java?<a id="whyjava"/>
In the "Poppy" [thesis](https://hal.inria.fr/tel-01104641v1/document) (section 7.4.1 and following), the author considers use of the Robot Operating System (ROS) for the core software and concludes that it is overly complex and inefficient. This coincides with my own experience with [sarah-bella](https://github.com/chuckcoughlin/sarah-bella). Moreover, I discovered that, at least with Android, ROS messaging was not reliable. Messages were dropped under high load, a situation not acceptable for control applications.

This same author noted that the "Pypot" software developed in Python for Poppy had severe performance limitations that placed strict limits on its design.

While I can't simply try all the designs, [YARP](http://www.yarp.it/index.html) used by the "iCub" project seemed closest to my perceived needs. It is written in C++ and is loaded with features that I'll never use. Given my failure to create a cross-compilation environment, I've decided to let a simplified *YARP* serve as an inspiration for a Java-based solution. For simplicity, the ancillary code will be Java also.

Why did I select Java for this code when the iCub project chose Python?
  * Familiarity - over 2 decades of working with Java
  * Refactoring - the refactoring capabilities of Eclipse are unparalleled, in my opinion, and make code re-structuring quick, easy and accurate
  * Code-Debug-Cycle - problems are more likely to be discovered by the compiler rather than run-time
  * Debugging - via Eclipse we can set breakpoints and inspect run-state not only locally, but on the target robot
  * Performance - Java executes an order of magnitude faster than Python
  * Threading - the Java threading model is more straightforward (IMHO)
  * Cross-compiling - difficulties creating Odroid executables precluded my use of C++.

  #### Failures <a id="failures"/>
  This section documents some ideas that were tried and abandoned.

  *** Gradle *** <br/>
  ``Gradle`` is a modern tool for building software of several flavors.  To install ``Gradle`` use ``homebrew``:
  ```
     brew install gradle
     brew install gradle-completion
  ```
  I totally failed to create a C++ build script for cross-compilation (Mac to Odroid). Moreover after nearly a week of trying, I gave up on the Java building also. I was unable to create an environment to conveniently share re-usable code among separate top level targets. I'm sure it can be done, I just lost interest. *ant* does everything I need.

  *** Cross-compilation *** <br/>
  On the build machine, I downloaded [GNU Embedded Toolchain for ARM]( https://developer.arm.com/open-source/gnu-toolchain/gnu-rm/downloads), then followed the installation steps outlined [here](https://gnu-mcu-eclipse.github.io/toolchain/arm/install/#macos-1), Using a *makefile* and these tools, I compiled C++ code for the Odroid X64. It had a bad format and didn't execute. I gave up on C++ in favor of Java for the core control loop.