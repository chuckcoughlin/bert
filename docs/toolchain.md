# Architecture and Toolchain

"Bert" is an amalgam of the [Poppy](https://www.poppy-project.org) platform from
[GenerationRobots](https://www.generationrobots.com/en/278-poppy-humanoid-robot) and the Italian Institute of Technology [iCub Project](http://www.icub.org/bazaar.php). The main project repository is at: https://github.com/robotology/icub-main.

<i>Poppy</i> is open-source format, both hardware and software. Version 1.0.2 source as at: https://github.com/poppy-project/poppy-humanoid. A full authors reference may be found at: https://github.com/poppy-project/poppy-humanoid/doc/authors.md. The repository contains full assembly instructions.

The operating software is based on <i>iCub</i> which is at its core uses Yet Another Robot Platform ((YARP)[http://www.yarp.it/]). We have simplified <i>YARP</i>, folded in the <i>Poppy</i> code, implemented extensions in Java (as opposed to the original Python), and added an Android tablet for speech processing.

This document describes the tools used to develop "Bert" and summarizes the construction process, both hardware and software.
In addition we discuss the core-architecture and interfaces between the main components.

***
## Table of Contents <a id="table-of-contents"></a>
  * [Hardware](#hardware)
    * [Skeleton](#skeleton)
  * [Software Development](#software)
    * [Eclipse](#eclipse)
    * [Android Studio](#android)
    * [Other Tools](#other)
  * [Software Architecture](#architecture)
***
## Hardware <a id="hardware"/>
### Processor <a id="processor"></a>
[toc](#table-of-contents)
Main processor is an Odroid.
### Skeleton <a id="skeleton"></a>
[toc](#table-of-contents)
The skeletal print-files provided by GenerationRobots are in .STL format. This form is printable directly, but not conducive to modification.


The following sections describe modifications to the original parts.

#### Head <a id="skeleton-head"></a>
#### Torso <a id="skeleton-torso"></a>
#### Legs <a id="skeleton-legs"></a>
#### Arms <a id="skeleton-arms"></a>

***
## Software Development <a id="software"/>
The development host is an iMac running OSX High Sierra (10.13). The code repository resides here. As shown below, code is cross-compiled and downloaded onto the robot target over a WiFi connection.
An auxiliary application resides on an Android tablet. It translates voice commands for control of the robot.

![System Setup for Development](/images/development-layout.png)
````                        Development - System Setup ````


### Eclipse <a id="eclipse"></a>
[toc](#table-of-contents)

***General***<br/>

### Android Studio <a id="android"></a>
[toc](#table-of-contents)

***General***<br/>
The tablet application, ***BertOp*** is the Human Machine Interface (HMI) in normal operation.  Most importantly it receives and analyzes voice commands, forming the only control interface. Additionally it maintains the voice transcript and displays results from the robot's internal health monitor. The tablet is a Samsung Galaxy S3, 10" Android 8.0.0 (SDK version 26).

The control application is a standard Android application built using Android Studio 3.0. (The studio may be downloaded from http://developer.android.com.) The studio requires a minor configuration of the host build system. Make the Android home environment variable available by adding to ~/.bashrc:
    ```ANDROID_HOME=~/Library/Androd/sdk```

***Voice Commands***<br/>
Voice commands are implemented purely via the Android tablet using the builtin speech-to-text features of Android. On the tablet, the ***BertOp*** app must be given microphone permissions.

For production of speech from robot output,  text-to-speech can be configured in the settings under "Accessibility". Parameters include languages supported and characteristics of the speaker such as volume.

***Network Configuration***<br/>
Communication between the tablet and main robot processor is over Bluetooth. On the tablet's settings "Connections" page, make sure that bluetooth is enabled. Under "More Connection Settings", enable "Nearby device scanning". Network or device pairing selections are made from menus accessed from the main screen.

Pairing with the robot must be completed before the ***BertOp*** application is started. Pairing may be initiated either from the robot or the tablet.

Note that the emulator does not support Bluetooth and is, therefore, not available as a test tool.

***Persistent Storage***<br/>
Configuration parameters, vocabulary and other data that are meant to be stored long-term reside in a SQLite database accessible through the tablet application.

***Transfer to Tablet***<br/>
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
