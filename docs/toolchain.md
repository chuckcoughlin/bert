# Toolchain

This document describes the setup and tools used to develop "Bert" and summarizes the construction process, both hardware and software.

"Bert" is a based on the [Poppy](https://www.poppy-project.org) platform from
[GenerationRobots](https://www.generationrobots.com/en/278-poppy-humanoid-robot).  *Poppy* is supplied in open-source format, both hardware and software. Version 1.0.2 source is at: https://github.com/poppy-project/poppy-humanoid. The repository contains full assembly instructions.

![poppy](/images/poppy.png)
````                        Poppy -  Generation Robots````



***
## Table of Contents <a id="table-of-contents"></a>
  * [Hardware](#hardware)
    * [Skeleton](#skeleton)
    * [Motors](#motors)
  * [System Setup](#system)
    * [Odroid](#odroid)
  * [Software Development](#software)
    * [Eclipse](#eclipse)
    * [Android Studio](#android)
    * [Other Tools](#other)
  * [Software Architecture](#architecture)
  * [Failures](#failured)
***
## Hardware <a id="hardware"></a>

### Skeleton <a id="skeleton"></a>
[toc](#table-of-contents)

The skeletal print-files provided by GenerationRobots are in .STL format. This form is printable directly, but not conducive to modification.

![Bert Skeleton](/images/skeletal_assembly.png)
```                  Newly Assembled Skeleton     ```

Excellent assembly instructions may be found [here](https://github.com/poppy-project/poppy-humanoid/blob/master/hardware/doc/en/assemblyGuide.md). The
videos include step-by-step procedures for each motor group.
The following sections contain describe modifications we've made to the original instructions, if any.

#### Legs <a id="skeleton-legs"></a>
US child-size 8 shoes seem to fit. I used a 3D print coating, [XTC-3D](https://www.smooth-on.com/products/xtc-3d/) to effect a smooth, shiny finish on the feet.
#### Arms <a id="skeleton-arms"></a>
The hands are coated in [UreCoat](https://www.smooth-on.com/product-line/urecoat/) which gives a rather creepy-feeling soft latex covering. A flesh-toned [colorant](https://www.smooth-on.com/category/color-and-fillers/) was mixed into the coating before application.
#### Torso <a id="skeleton-torso"></a>
We used 2.5 x 12mm screws to fasten the 2 SMPS2Dynamixel boards to the torso. The video shows sticking them on with hot-glue.

We applied 12V power to each SMPS2Dynamixel separately from our power supply. There is no connection between SMPS2Dynamixel boards on the RS485 ports as shown in the assembly video. The RS485 port from the upper SMPs2Dynamixel board is used to power the DC/DC Stepdown converter. This, in turn, powers the Odroid board.

#### Head <a id="skeleton-head"></a>
Created an umbilical bundle from rear-end of head to torso. It includes:
 * DC/DC 5V step-down converter for power to Odroid
 * USB extension bundle for USB2Serial
 * HDMI 8" extension to save wear and tear on Odroid HDMI port

Here are two views of the bundle from the back:
 ![Umbilical bundle1](/images/umbilical1.png)
 ```                  Umbilical Bundle I     ```

 ![Umbilical bundle2](/images/umbilical2.jpg)
 ```                  Umbilical Bundle II     ```
#### Power Supply <a id="power-supply"></a>
5V power for the Odroid processor is supplied by an UBEC DC/DC step-down converter.

![5V Power Supply](/images/5volt_step_down.jpg)
```                  5V DC Power     ```

The main power pack, shown below, was constructed from a decorative tin that my wife had saved. Inside it contains 2 AC/12V DC converters. The connectors plug directly into the back of the robot.

![Power Supply](/images/power_supply.jpg)
```                  12V DC Power     ```
### Joints <a id="joints"></a>
[toc](#table-of-contents)

Once [pypot](#pypot) is installed on the Odroid then the *herborist* application is available to configure and address the Dynamixel motors.
```
python /usr/local/lib/python2.7/dist-packages/pypot/tools/herborist/herborist.py
```

Initially, the devices can be found with the following parameters:
```
   Port:       /dev/tty/ACM0
   Protocol:   MX
   USB Device: USB2AX
   Baud rate:  57600
   ID       :  1
```

The Dynamixel configuration setup is shown below:

![poppy](/images/dynamixel_configuration.png)
````                        Dynamixel Configuration Setup````

Refer to the worksheet in the *git* repository at ``cad/DynamixelConfiguration.ods`` to find id and angle limits for each motor. In each case, set the baud rate to 1000000 and return delay time to 0.

*Note: When the motors were fresh from the manufacturer ``herborist`` continually froze when flashing the EEPROM. I was forced to use Robotis [Dynamixel Wizard](http://www.robotis.us/roboplus1) on a Windows machine to make the initial ID and baudrate settings.  Once a motor was configured initially, ``herborist`` seems to work just fine.*

## System Setup <a id="system"/>
### Odroid <a id="odroid"></a>
[toc](#table-of-contents)

*** Initial Configuration ***<br/>
The following sections describe setup of the main processor on the robot, an Odroid-XU4 running Ubuntu 16.04 Linux. The instructions below assume an initial board setup as described  [here](https://magazine.odroid.com/wp-content/uploads/odroid-xu4-user-manual.pdf).

A USB extension board is required as slots are used for:
 * Keyboard/Mouse
 * WiFi
 * Bluetooth
 * USB2AX (2)

The filesystem appeared to be properly configured on initial startup. There was no need to extend the root partition.

Create a user. Add it to the **sudo**, **dialout** and **uucp** groups.
Add the following to ``~/.bashrc`` and equivalent to ``~/Library/LaunchAgents/environment.plist``:
```
   export BERT_HOME=/usr/local/robot
```

Set the timezone:
```
  sudo dpkg-reconfigure tzdata
```

Once a WiFi connection has been made, configure a static IP address. This allows us to connect to the robot even if it comes up "headless". Under the Preferences menu, Network Connections, edit the WiFi connection that is live. On the IPV4 Settings tab, change the Method: to "Manual". Add a static address, e.g. 192.168.1.20; 255.255.255.0; 192.168.1.1. Restart the machine. When running again,
make sure you can ping the new address from a different machine.

In the same dialog, set Domain Name Servers, comma-separated. These two will work, but there are others:
```
  8.8.8.8
  8.8.4.4
```

Set the hostname to "bert":
```
  sudo hostnamectl set-hostname bert
```

Make sure these lines exist in `/etc/ssh/sshd_config`.
```
PubkeyAuthentication yes
AuthorizedKeysFile .ssh/authorized_keys
```

Next, on each remote, generate SSH keys (if not already done).
```
  ssh-keygen  (use default location, no password)
```
Also on each remote system, add to `/etc/hosts`:
```
  192.168.1.20 bert
```
Then, also on each remote system (appropriately replacing the username),
```
  cd ~/.ssh
  ssh-copy-id -i id_rsa.pub chuckc@bert
```

Install some missing tools and update the system. We have found that the *apt* commands repeatedly throw our wi-fi router off-line, so these commands were all executed using a direct ethernet connection.
```
  sudo apt install rsync
  sudo apt install vsftpd
  sudo apt install firefox
  sudo apt install sqlite3
  sudo apt install vsftp
  sudo apt install libjssc-java
  sudo apt-get update
  sudo apt-get upgrade -y
  sudo apt-get autoremove -y
  sudo apt-get autoclean -y
```
The reason for ``firefox`` is that we were not able to properly configure the proxy server in ``chromium``, the default browser.

To shutdown,
```
  sudo shutdown -h now
```
Wait until the blue LED has gone out, then unplug.

*** Java ***<br/>
Download the latest Java 11 Development (JDK) version using
```
  sudo apt install openjdk-11-jdk-headless
```
Installing the JDK allows us to compile Java on the Odroid, if necessary.

Once the build has been executed on the Development system (and deployed), edit ``/etc/environment``, adding the following directories to the **PATH** variable (before  */usr/bin*):
```
   /usr/local/robot/bin
```

Make a scaled-down Java Runtime Environment that includes only the packages used by the robot applications. This results in a significant savings in memory.
```
   jlink --module-path /usr/local/robot/mods --add-modules java.base java.xml --output /usr/local/robot/java
```
All scripts that launch the robot applications refer to this *jre*. This command must be refreshed each time applications are updated.

*** PyPot <a id="pypot"></a>***<br/>
*PyPot* provides demonstration code and the **herborist** tool that is used to configure Dynamixel stepper motors. Documentation may be found [here](https://poppy-project.github.io/pypot/index.html).
```
  sudo apt-get install python-pip
  sudo apt-get install python-numpy
  sudo apt-get install python-scipy
  sudo apt-get install python-matplotlib
  sudo apt-get install python-qt4
  sudo pip install pypot
```

***
## Software Development <a id="software"/>
The development host is an iMac running OSX Mohave (10.14). The code repository resides on this machine. Code is cross-compiled and downloaded onto the robot target over a WiFi connection. Here is a diagram that summarizes the flow of the build and major tools used:
  ![Build Plan](/images/development_layoutsvg)
  ````                        Development Tools ````

Code is compiled and downloaded onto the robot target over a WiFi connection using *rsync*.

The iMac requires the same Java version as the Odroid (Java 11). It is downloadable from [here](http://www.oracle.com/technetwork/java/javase/downloads). Make sure to download the JDK and install the “Development tools” into the default location (e.g. /usr/local/bin). Extend the system path to include this area.

### Eclipse <a id="eclipse"></a>
[toc](#table-of-contents)

*** Installation *** <br/>
_eclipse_ is an open-source Integrated Development Environment (IDE) for Java, C++ and Python. The available eclipse versions are listed at: http://www.eclipse.org/downloads/packages. At the time of this writing, the latest version is “2018-09”. Download the package “Eclipse IDE for Java Developers” and follow installation instructions.
Start eclipse and point it to our initial workspace, ```workspace``` in the `git` repository. Add a ```/.metadata/``` entry in _.gitignore_ to avoid saving the workspace configuration in the repository.

Under ```Preferences->Java->Installed JREs``` make sure that the only available JRE is Java 11.

*** Projects *** <br/>
From the _eclipse_ <u>File->Import</u> menu,"General","Existing Projects into Workspace", import the following projects.
  - Core: C++ source for the application on the robot which runs the main event loop.
  - Build: _ant_ scripts for compiling and installing the project build products onto the robot. There are separate scripts for the CommandLoop, Robot and Configuration projects.
  - Joint: Java code for control of the various servo motors on the robot. These are executed by the _core_ application.
  - Lib: Third party open-source libraries. In general, this area does not include source.
  - Poppy: The _Poppy_ python source code from _GenerationRobots_. This code is for reference only and is not installed on the robot. It consists largely of sample applications.
  - PyPot: _Poppy_ code for controlling the Dynamixel motors. This is primarily for viewing. We do use the *herborist* tool for configuring the Dynamixel stepper motors.
  - YARP: C++ source code from the _iCub_ project. This code is for provided for ease of browsing and is not compiled.

When complete the project workspace should look like:
![Eclipse Setup](/images/eclipse_setup.png)
```                  Eclipse Projects     ```

Each java module has its own _eclipse_ project.

*** Build Scripts *** <br/>
Every project has a *build.xml* *ant* script that test-compiles code within the project. The _Build_ project contains additional *ant* scripts that build the main executables and copy them to the robot. These scripts are designed to be executed directly from ``eclipse``.
NOTE: If the ant scripts fail to run, terminating immediately, the remedy is to edit their configuration to "run in the same JRE as the workspace".

In order to make environment variables accessible within *ant*, edit the run configuration for the script and add in the argument section something similar to:
```
  -Dbert.home="${env_var:BERT_HOME}"
```

This makes *BERT_HOME* accessible as *${bert.home)* inside the script.

In addition to the *ant* scripts, there are a few shell scripts. To execute from ``eclipse``, in the Project browser, right-click on the script and select ``Run As->Run Shell Script``.

*** Archive *** <br/>
The *Archive* project is a collection of open-source library modules.
* https://www.slf4j.org/download.htm slf4j-api-1.8.0.beta2.jar logback-classic-1.3.0-alpha4.jar logback-core-1.3.0-alpha4.jar
* http://repo1.maven.org/maven2/com/fasterxml/jackson/core jackson-core-2.9.7.jar jackson-databind-2.9.7.jar java-annotations-2.9.7.jar
* http://central.maven.org/maven2/com/ibm/icu/icu4j/63.1 com.ibm.icu4f-63.1.jar
* https://bitbucket.org/xerial/sqlite-jdbc/downloads sqlite-jdbc-3.23.1.jar
* https://code.google.com/archive/p/java-simple-serial-connector/downloads jssc.jar, plus C++ source for shared library

*** Modularized Jar Files ***<br/>
Most of the open source jar files listed above had not been updated for Java11 module compatibility. However all have been manually updated prior to storage in the archive.

Thanks to [Michael Easter](https://github.com/codetojoy/easter_eggs_for_java_9/blob/master/egg_34_stack_overflow_47727869/run.sh) for the following example that shows how to modularize the ``Jackson`` jar files.
The root directory of the *Archive* project is the starting point. The 3 original non-modularized *Jackson* jar files have been downloaded into ``jars``. The modularized results will be stored into ``mods``.
```
   ARCHIVE=`pwd`
   jdeps --generate-module-info work jars/jackson-core-2.9.7.jar
   cp jars/jackson-core-2.9.7.jar mods/jackson-core.jar
   rm -rf classes
   mkdir classes
   cd classes
   jar -xf ${ARCHIVE}/jars/jackson-core-2.9.7.jar
   cd ${ARCHIVE}/work/com.fasterxml.jackson.core
   javac -p jackson.core -d ${ARCHIVE}/classes module-info.java
   jar -uf ${ARCHIVE}/mods/jackson-core.jar -C ${ARCHIVE}/classes module-info.class

   cd $ARCHIVE
   jdeps --generate-module-info work jars/jackson-annotations-2.9.7.jar
   cp jars/jackson-annotations-2.9.7.jar mods/jackson-annotations.jar
   rm -rf classes
   mkdir classes
   cd classes
   jar -xf ${ARCHIVE}/jars/jackson-annotations-2.9.7.jar
   cd ${ARCHIVE}/work/com.fasterxml.jackson.annotation
   javac -p jackson.annotations -d ${ARCHIVE}/classes module-info.java
   jar -uf ${ARCHIVE}/mods/jackson-annotations.jar -C ${ARCHIVE}/classes module-info.class

   cd $ARCHIVE
   jdeps --module-path ${ARCHIVE}/mods --add-modules com.fasterxml.jackson.annotation,com.fasterxml.jackson.core --generate-module-info work jars/jackson-databind-2.9.7.jar
   cp jars/jackson-databind-2.9.7.jar mods/jackson-databind.jar
   rm -rf classes
   mkdir classes
   cd classes
   jar -xf ${ARCHIVE}/jars/jackson-databind-2.9.7.jar
   cd ${ARCHIVE}/work/com.fasterxml.jackson.databin
   javac --module-path ${ARCHIVE}/mods --add-modules com.fasterxml.jackson.annotation,com.fasterxml.jackson.core -d ${ARCHIVE}/classes module-info.java
   jar -uf ${ARCHIVE}/mods/jackson-databind.jar -C ${ARCHIVE}/classes module-info.class
   ${ARCHIVE}
   rm -rf work classes
```
*** ANTLR *** <br/>
*ANTLR* is a parsing framework used for understanding natural language. From the Eclipse marketplace install a plugin for *antlr4*. Use the one by *Edgar Espina*.

*** C++ *** <br/>
To add C++ support, under <u>Help->Install New Software</u>, in the _work with_ selector, enter http://download.eclipse.org/tools/cdt/releases/9.5. Then select "C++ Tools". Restart _eclipse_.

We use C++ support to browse the original *iCub* and *YARP* code. There is no C++ in the final product.

*** Python *** <br/>
PyDev is an eclipse plugin for development of Python code. Under the _eclipse_ <u>Help->Install New Software</u> menu, add a new update source:
```
Name: PyDev
Location: http://pydev.org/updates
```
We use PyDev to browse the original *Poppy* and *iCub* code.

### Android Studio <a id="android"></a>
[toc](#table-of-contents)

*** General *** <br/>
The tablet application, ***BertSpeak*** is the only Human Machine Interface (HMI) in normal operation.  Most importantly, it receives and analyzes voice commands, forming the only control interface. Additionally it maintains the voice transcript and displays results from the robot's internal health monitor. The tablet is a Samsung Galaxy S3, 10" Android 8.0.0 (SDK version 26).

The control application is a standard Android application built using Android Studio 3.3. (The studio may be downloaded from http://developer.android.com.) The studio requires a minor configuration of the host build system. Make the Android home environment variable available by adding to ~/.bashrc:
    ```ANDROID_HOME=~/Library/Androd/sdk```

*** Voice Commands ***<br/>
Voice commands are implemented purely via the Android tablet using the builtin speech-to-text features of Android. On the tablet, the ***BertOp*** app must be given microphone permissions.

For production of speech from robot output,  text-to-speech can be configured in the settings under "Accessibility". Parameters include languages supported and characteristics of the speaker such as volume.

*** Network Configuration ***<br/>
Communication between the tablet and main robot processor is over Bluetooth. On the tablet's settings "Connections" page, make sure that bluetooth is enabled. Under "More Connection Settings", enable "Nearby device scanning". Network or device pairing selections are made from menus accessed from the main screen.

Pairing with the robot must be completed before the ***BertOp*** application is started. Pairing may be initiated either from the robot or the tablet.

Note that the emulator does not support Bluetooth and is, therefore, not available as a test tool.

*** Persistent Storage ***<br/>
Configuration parameters, vocabulary and other data that are meant to be stored long-term reside in a SQLite database accessible through the tablet application.

*** Transfer to Tablet ***<br/>
In order for the application to be transferred to the tablet from the build system, the tablet must be connected via USB. Use the same USB that is used to charge the device.

The tablet must also be set in "developer" mode. This is accomplished under Settings->About Tablet. Tap on "Build number" 7 times. (Yes, really). Under Settings->Developer options, enable USB debugging. Once the cable is connected a dialog should popup asking you to allow file transfer. (If this does not appear, you may have to fiddle with Developer options->USB Configuration).

On the build system, configure Android Studio (Tools->Run>Edit Configurations) to target the build output directly to a USB device. After a successful build, merely select the "run" button to transfer the **apk** executable to the tablet.

***
### Other Tools <a id="other"/>
We make use of the following freely-available applications:
  * [OpenSCAD](http://www.openscad.org) - Construct geometric parts in script. Export in .stl format.
  * [MeshLab](http://www.meshlab.net) - Use this tool to optimize and convert between 3D formats.
  * [Sculptris](http://pixologic.com/sculptris) - Sculpt objects free-form. This is useful for molding irregular, rounded or textured parts. Export in .obj format.
This drawing and others is constructed using **InkScape** from https://inkscape.org/en/release/0.92.2. 3D CAD drawings are constructed using OpenSCAD from http://www.openscad.org/downloads.html.
Electrical circuits are constructed using [iCircuit](https://itunes.apple.com/us/app/icircuit/id454347770?ls=1&mt=12).
