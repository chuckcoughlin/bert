# Architecture and Toolchain

"Bert" is an amalgam of the [Poppy](https://www.poppy-project.org) platform from
[GenerationRobots](https://www.generationrobots.com/en/278-poppy-humanoid-robot) and the [iCub Project](http://www.icub.org/bazaar.php) from the [Italian Institute of Technology](https://www.iit.it).

Bert's physical characteristics are derived from *Poppy* which is supplied in open-source format, both hardware and software. Version 1.0.2 source is at: https://github.com/poppy-project/poppy-humanoid. The repository contains full assembly instructions. The project is described in detail in the thesis ["Poppy: open-source, 3D printed and fully-modular
robotic platform for science, art and education"](https://hal.inria.fr/tel-01104641v1/document) by Matthieu Lapeyre. A full list of project authors may be found at: https://github.com/poppy-project/poppy-humanoid/doc/authors.md.

The control software is patterned on <i>iCub</i> (main project repository: https://github.com/robotology/icub-main) which is at its core uses Yet Another Robot Platform [(YARP)](http://www.yarp.it/). We have greatly simplified the robot control converting it from C++ to Java, folded in the <i>Poppy</i> code, implemented extensions in Java (as opposed to the original Python), and added an Android tablet for speech processing.

![poppy](/images/poppy.png)
````                        Poppy -  Generation Robots````

This document describes the tools used to develop "Bert" and summarizes the construction process, both hardware and software.
In addition we discuss the core-architecture and interfaces between the main components.

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

![Bert Skeleton](/images/printed_parts.jpg)
```                  3D Printed Skeleton     ```


The following sections contain assembly links and describe modifications to the original parts, if any.


#### Legs <a id="skeleton-legs"></a>
#### Arms <a id="skeleton-arms"></a>
#### Torso <a id="skeleton-torso"></a>
#### Head <a id="skeleton-head"></a>

### Joints <a id="joints"></a>
[toc](#table-of-contents)

Once [pypot](#pypot) is installed on the Odroid then the *herborist* application is available to configure and address the Dynamixel motors.
```
python /usr/local/lib/python2.7/dist-packages/pypot/tools/herborist/herborist.py
```

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
  sudo apt install vsftp
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
  - PyPot: _Poppy_ code for controlling the Dynamixel motors. This is strictly for viewing.
  - YARP: C++ source code from the _iCub_ project. This code is for provided for ease of browsing and is not compiled.

When complete the project workspace should look like:
![Eclipse Setup](/images/eclipse_setup.png)
```                  Eclipse Projects     ```

Each java module has its own _eclipse_ project.

*** Build Scripts *** <br/>
Every project has a *build.xml* *ant* script that test-compiles code within the project. The _Build_ project contains additional *ant* scripts that build the main executables and copy them to the robot. These scripts are designed to be executed directly from ``eclipse``.

In addition to the *ant* scripts, there are a few shell scripts. To execute from ``eclipse``, in the Project browser, right-click on the script and select ``Run As->Run Shell Script``.

*** C++ *** <br/>
To add C++ support, under <u>Help->Install New Software</u>, in the _work with_ selector, enter http://download.eclipse.org/tools/cdt/releases/9.5. Then select "C++ Tools". Restart _eclipse_.

We use C++ support in browsing the original *iCub* and *YARP* code.

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
The tablet application, ***BertOp*** is the Human Machine Interface (HMI) in normal operation.  Most importantly it receives and analyzes voice commands, forming the only control interface. Additionally it maintains the voice transcript and displays results from the robot's internal health monitor. The tablet is a Samsung Galaxy S3, 10" Android 8.0.0 (SDK version 26).

The control application is a standard Android application built using Android Studio 3.0. (The studio may be downloaded from http://developer.android.com.) The studio requires a minor configuration of the host build system. Make the Android home environment variable available by adding to ~/.bashrc:
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

***
## Software Architecture <a id="architecture"/>

*** Why Java?*** <br/>
In the "Poppy" [thesis](https://hal.inria.fr/tel-01104641v1/document) (section 7.4.1 and following), the author considers use of the Robot Operating System (ROS) for the core software and concludes that it is overly complex and inefficient. This coincides with my own experience with [sarah-bella](https://github.com/chuckcoughlin/sarah-bella). Moreover, I discovered that, at least with Android, ROS messaging was not reliable. Messages were dropped under high load, a situation not acceptable for control applications.

This same author noted that the "Pypot" software developed in Python for Poppy had severe performance limitations that placed strict limits on its design.

While I can't simply try all the designs, [YARP](http://www.yarp.it/index.html) used by the "iCub" project seemed closest to my perceived needs. It is written in C++ and is loaded with features that I'll never use. Given my failure to create a cross-compilation environment, I've decided to let a simplified *YARP* serve as an inspiration for a Java-based solution. For simplicity, the ancillary code will be Java also.

Why did I select Java for this code when the iCub project chose Python?
  * Familiarity - over 2 decades of working with Java
  * Debugging - problems are discovered by the compiler rather than run-time
  * Performance - Java executes an order of magnitude faster than Python
  * Threading - the Java threading model is more straightforward (IMHO)
  * Cross-compiling - difficulties creating Odroid executables preclude use of C++.

  Here is a diagram that shows the major software components.
  ![Major Software Components](/images/software_components.png)
  ````                        Development - System Architecture ````

  ## Failures <a id="failures"/>
  This section documents some ideas that were tried and abandoned.

  *** Gradle *** <br/>
  ``Gradle`` is a modern tool for building software of several flavors.  To install ``Gradle`` use ``homebrew``:
  ```
     brew install gradle
     brew install gradle-completion
  ```
  I totally failed to create a C++ build script for cross-compilation (Mac to Odroid). Moreover after nearly a week of trying, I gave up on the Java building also. I was unable to create an environment to conveniently share re-usable code among separate top level targets. I'm sure it can be done, I just lost interest.

  *** Cross-compilation *** <br/>
  On the build machine, I downloaded [GNU Embedded Toolchain for ARM]( https://developer.arm.com/open-source/gnu-toolchain/gnu-rm/downloads), then followed the installation steps outlined [here](https://gnu-mcu-eclipse.github.io/toolchain/arm/install/#macos-1), Using a *makefile* and these tools, I compiled C++ code for the Odroid X64. It had a bad format and didn't execute. I gave up on C++ in favor of Java for the core control loop.
