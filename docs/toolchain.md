# Architecture and Toolchain

```"Bert"``` is an amalgam of the [Poppy](https://www.poppy-project.org) platform from
[GenerationRobots](https://www.generationrobots.com/en/278-poppy-humanoid-robot) and the Italian Institute of Technology [iCub Project](http://www.icub.org/bazaar.php) (main project repository: https://github.com/robotology/icub-main).

The physical characteristics of "Bert" are derived from ``Poppy`` which is supplied in is open-source format, both hardware and software. Version 1.0.2 source as at: https://github.com/poppy-project/poppy-humanoid. A full authors reference may be found at: https://github.com/poppy-project/poppy-humanoid/doc/authors.md. This repository contains full assembly instructions.

The operating software is based on <i>iCub</i> which is at its core uses Yet Another Robot Platform [(YARP)](http://www.yarp.it/). We have simplified <i>YARP</i>, folded in the <i>Poppy</i> code, implemented extensions in Java (as opposed to the original Python), and added an Android tablet for speech processing.

This document describes the tools used to develop ```"Bert"``` and summarizes the construction process, both hardware and software.
In addition we discuss the core-architecture and interfaces between the main components.

***
## Table of Contents <a id="table-of-contents"></a>
  * [Hardware](#hardware)
    * [Skeleton](#skeleton)
  * [Configuration](#configuration)
    * [Odroid](#odroid)
  * [Software Development](#software)
    * [Eclipse](#eclipse)
    * [Android Studio](#android)
    * [Other Tools](#other)
  * [Software Architecture](#architecture)
***
## Hardware <a id="hardware"></a>

### Skeleton <a id="skeleton"></a>
[toc](#table-of-contents)

The skeletal print-files provided by GenerationRobots are in .STL format. This form is printable directly, but not conducive to modification.


The following sections describe modifications to the original parts.

#### Head <a id="skeleton-head"></a>
#### Torso <a id="skeleton-torso"></a>
#### Legs <a id="skeleton-legs"></a>
#### Arms <a id="skeleton-arms"></a>

## Configuration <a id="configuration"/>
### Odroid <a id="odroid"></a>
[toc](#table-of-contents)

The following sections describe setup of the main processor on the robot, an Odroid-XU4 running Ubuntu 16.04 Linux. A great Odroid setup guide may be found [here](https://magazine.odroid.com/wp-content/uploads/odroid-xu4-user-manual.pdf). At the suggestion of the Odroid developers, we have selected BlackBox as the Linux distribution for its simplicity and its careful use of resources.

*** Java ***<br/>
Download the latest Java 11 Development (JDK) version from http://www.oracle.com/technetwork/java/javase/downloads. Downloading the JDK allows Java to be compiled on-board if so needed.
Download and run the installer executable. Install the “Development tools” into the default location (e.g. /usr/local/bin). Extend the system path to include this area.


***
## Software Development <a id="software"/>
The development host is an iMac running OSX Mohave (10.14). The code repository resides on this machine. Code is cross-compiled and downloaded onto the robot target over a WiFi connection.

The iMac requires the same Java version as the Odroid (Java 11). It is downloadable from [here](http://www.oracle.com/technetwork/java/javase/downloads). Make sure to download the JDK.


### Eclipse <a id="eclipse"></a>
[toc](#table-of-contents)

*** Installation *** <br/>
_eclipse_ is an open-source Integrated Development Environment (IDE) for Java, C++ and Python. The available eclipse versions are listed at: http://www.eclipse.org/downloads/packages. At the time of this writing, the latest version is “2018-09”. Download the package “Eclipse IDE for Java Developers” and follow installation instructions.
Start eclipse and point it to our initial workspace, ```workspace``` in the `git` repository. Add a ```/.metadata/``` entry in _.gitignore_ to avoid saving the workspace configuration in the repository.

Under ```Preferences->Java->Installed JREs``` make sure that the only available JRE is Java 11.

*** Gradle *** <br/>
Building the source code and installing on the Odroid is controlled by ``Gradle`` scripts and plugins launched from Bash scripts. These scripts can be executed either from ``eclipse`` or the command line. The Install project contains directories corresponding to the ``eclipse`` projects to be built. Each directory contains an ``install.sh`` script.

Install ``Gradle`` via ``homebrew``.
```
   brew install gradle
   brew install gradle-completion
```

*** C++ *** <br/>
To add C++ support, under <u>Help->Install New Software</u>, in the _work with_ selector, enter http://download.eclipse.org/tools/cdt/releases/9.5. Then select "C++ Tools". Restart _eclipse_.

*** Cross-compilation *** <br/>
The C++ code must be compiled for the Odroid X64. Follow these steps to configure the compile for the target architecture.

*** Python *** <br/>
PyDev is an eclipse plugin for development of Python code. Under the _eclipse_ <u>Help->Install New Software</u> menu, add a new update source:
```
Name: PyDev
Location: http://pydev.org/updates
```


*** Projects *** <br/>
From the _eclipse_ <u>File->Import</u> menu,"General","Existing Projects into Workspace", import the following projects.
  - Core: C++ source for the application on the robot which runs the main event loop.
  - Install: _gradle_ scripts for building and installing the project build targets onto the robot. There are three separate projects: C/C++, Java and Configuration.
  - Joint: Java code for control of the various servo motors on the robot. These are executed by the _core_ application.
  - Lib: Third party open-source libraries. In general, this area does not include source.
  - Poppy: The _Poppy_ python source code from _GenerationRobots_. This code is for reference only and is not installed on the robot. It consists largely of sample applications.
  - PyPot: _Poppy_ code for controlling the DYnamixel motors. This is strictly for viewing.
  - YARP: C++ source code from the _iCub_ project. This code is for provided for ease of browsing and is not compiled.

When complete the project workspace should look like:


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
