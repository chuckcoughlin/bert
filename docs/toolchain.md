# Toolchain

This document describes the software tools used to develop "Bert", including their procurement and configuration. It also gives a step-by-step guide to assembly of the skeleton, including visual aids.

"Bert" is a based on the [Poppy](https://www.poppy-project.org) platform from
[Generation Robots](https://www.generationrobots.com/en/278-poppy-humanoid-robot).  *Poppy* is supplied in open-source format, both hardware and software. Version 1.0.2 source is at: <https://github.com/poppy-project/poppy-humanoid>. The repository contains both hardware and software components plus full assembly instructions.

![bert](/images/bert_scaffold.png)
```                        Bert Supported by Scaffolding```


***
## Table of Contents <a id="table-of-contents"></a>
  * [Hardware](#hardware)
    * [Skeleton](#skeleton)
    * [Joints](#joints)
    * [Tablet](#tablet)
  * [System Setup](#system)
    * [Android](#androidprep)
    * [Odroid](#odroid)
  * [Software Development](#software)
    * [IntelliJ](#intellij)
    * [Android Studio](#android)
    * [Other Tools](#other)
***
## Hardware <a id="hardware"></a>
Details of the hardware assembly are documented in [The Assembly Guild for the Poppy_Humanoid](https://docs.poppy-project.org/en/assembly-guides/poppy-humanoid)

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

We applied 12V power to each SMPS2Dynamixel separately from our power supply. There is no connection between SMPS2Dynamixel boards on the RS485 ports as shown in the assembly video. The RS485 port from the upper SMPs2Dynamixel board is used to power the 12V Odroid board.

#### Head <a id="skeleton-head"></a>
The head design was revised in our latest version to accommodate the Odroid N2+ processor.
In the photos, the revised head is gold in color.  

A frontal view of the skull:
![Skull Front](/images/revised_head_front.jpeg)
```                  Skull Front View     ```

A back view of the skull before cabling is attached :
![Skull Back](/images/revised_head_back_unplugged.jpg)
```                  Skull Back View     ```

The umbilical cable bundle extends from the rear-end of the head to torso. It includes:
 * 12V cable for power to Odroid
 * USB extension bundle for USB2Serial
 * HDMI 8" extension to save wear and tear on Odroid HDMI port


Here are two views of the bundle from the back:
 ![Umbilical undle1](/images/umbilical1.png)
 ```                  Processor Connections     ```

 ![Umbilical undle2](/images/umbilical2.jpg)
 ```                  Umbilical Bundle     ```

 And finally we show the back view with cabling attached and dongles inserted :
 ![Skull Back Wired](/images/revised_head_back.jpeg)
 ```                  Skull Back View     ```


#### Power Supply <a id="power-supply"></a>
12V power for the Odroid processor is supplied directly through a custom cable. Note that
the outermost wire is connected to the outside sheath of the power connector.

![12V Processor Power](/images/12V_power_cord.png)
```                  12V Processor Power     ```

The main power pack, shown below, is constructed from a 3D-printed box (source code included). Inside it contains 2 AC/12V DC converters. The 12V connectors plug directly into the back of the robot.

![Power Supply](/images/power_supply.png)
```                  12V DC Power     ```

![Power Supply Mounted](/images/power_supply_mounted.png)
```                  Power Supply Mount     ```

### Joints <a id="joints"></a>
[toc](#table-of-contents)

The [pypot](#pypot) python package must be installed on the Odroid to configure and test the Dynamixel motors.

The Dynamixel setup for flashing EEPROM is shown below:

![poppy](/images/dynamixel_configuration.png)
```                        Dynamixel Flashing Setup```

More detailed information may be found [here](https://docs.poppy-project.org/en/assembly-guides/poppy-torso/addressing_dynamixe)
```
python3 /usr/local/lib/python3.10/dist-packages/pypot/tools/dxlconfig.py
```
Initially, the devices can be found with the following parameters:
```
    Port:       /dev/tty/ACM0
    Protocol:   MX
    USB Device: USB2AX
    Baud rate:  57600
    ID       :  1
```

<em> Note: When the motors were fresh from the manufacturer, I used Robotis [Dynamixel Wizard](http://www.robotis.us/roboplus1) on a Windows machine to make the initial ID and baudrate settings.  Once a motor is configured, use ``dxl_write`` for further setup.</em>


In the motor EEPROM and RAM configuration, angles are defined such that 0 deg is at the bottom when viewing the front of the motor, 180 deg is to the top. Values decrease going clock-wise. Thus, for our purposes, decreasing values of position, load and speed all refer to the clock-wise direction.
Refer to the worksheet in the *git* repository at ``cad/DynamixelConfiguration.ods`` to find id and angle limit settings for each motor. In each case, set the baud rate to 1000000 and return delay time to 0.


In order to correct for a consistent user-view of position, joints may be corrected for orientation and given a fixed offset. A joint configured as *indirect* might be considered as if viewed from the back-side. The corrections give the following meanings to angular positions. These definitions should apply equally to joints on both sides of the body.

| Axis | <center>Description</center> |
| :--: | :---------------------- |
| x | Front-to-back |
| y | Side-to-side |
| z | Vertical |

| Joint | <center>Definition</center> | Min Angle | Max Angle |
| :------ | :--------------------------------------------------- | --------: | ---------: |
| Abs(x) | Angle of side-to-side bend of the torso. 180 is straight, lower values are to the right. | 150 | 210 |
| Abs(y) | Angle between the torso and pelvis, front-to-back, measured frontwards. | 150 | 210 |
| Abs(z) | Angle of twist in the torso. Zero is no twist. Positive is toward the right. | 45 | 45 |
| Ankle | Angle between the bottom of the foot and shin, frontwards.| 45 | 180 |
| Arm(z) | Degrees of twist of upper arm. Zero is straight ahead with the palm inward. Positive is outward. | -45 | 45 |
| Bust(x) | Angle of side-to-side bend of the upper body. 180 is straight, lower values are to the right. | 150 | 210 |
| Bust(y) | Angle between the chest and torso, front-to-back, measured toward the front. | 150 | 210 |
| Elbow | Angle between the upper and lower arms, measured frontwards. | 45 | 180 |
| Hip(x) | Angle of spread of thighs. 180 is straight down, decreases as the spread increases. | 155 | 205 |
| Hip(y) | Angle between the torso and thigh, in front. 180 is straight, 90 is forward, horizontal. | 75 | 225 |
| Hip(z) | Twist of leg with respect to torso. Zero is straight ahead. Negative is knock-kneed. | -45 | 20 |
| Knee | Angle between the thigh and lower leg, measured toward the back. | 50 | 180 |
| Neck(y)| Degrees the head is tipped. 0 is horizontal, positive is up. |-5|20|
| Neck(z)| Angle the head is facing. 0 is straight ahead, positive to the right. |-45|45|
| Shoulder(x) | Angle of spread of the arms. 180 is straight down, 90 is straight out to the side. | 90 | 215 |
| Shoulder(y) | Angle between the upper arm and chest. 180 is straight down, 90 is forward, horizontal. | 0 | 225 |
<center>``Angular Position Definitions``                     </center>


## System Setup <a id="system"/>
### Android <a id="androidprep"></a>
[toc](#table-of-contents)

##### Persistent Storage
Configuration parameters, vocabulary and other data that are meant to be stored long-term reside in a SQLite database accessible through the tablet application.
Run the following scripts in order to prepare a SQLite database on the MacOSX development system to be accessible when running the Android Studio simulator with Android production code. In the
source ``install`` directory:

``./android_dev_data_dir.sh``
``./android_dev_db_setup.sh``

Note that the system must be restarted after the
first script.


##### Transfer to Tablet
In order for the application to be transferred to the tablet from the build system, the tablet must be connected via USB. Use the same USB cable that is used to charge the device.

The tablet must also be set in "developer" mode. This is accomplished under Settings->About Tablet->Software Information. Tap on "Build number" 7 times. (Yes, really). Under Settings->Developer options, enable USB debugging. Once the cable is connected a dialog should popup asking you to allow file transfer. (If this does not appear, you may have to fiddle with Developer options->USB Configuration).

On the build system, configure Android Studio (Tools->Run>Edit Configurations) to target the build output directly to a USB device. After a successful build, merely select the "run" button to transfer the **apk** executable to the tablet.

#### Tablet <a id="tablet"></a>

The tablet is a 12" Samsung Galaxy S8 running Android 13.0, API level 33. Android was chosen for its
speech capabilities. All speech analysis is done on the tablet hosting both text-to-speech and a speech
recognizer. At one time the tablet become stuck in headset mode. As a workaround, we bought an external
speaker that can ultimately be located in the head.

Make sure to update the language packages (Settings->General management->Language packs) and Google Speech Recognition and Synthesis application. The application is configured to speak English with a British accent. The voice is female (I'd prefer male).

### Odroid <a id="odroid"></a>
[toc](#table-of-contents)

##### Initial Configuration
The following sections describe setup of the main processor on the robot, an Odroid-N2+ running Linux, described [here](https://wiki.odroid.com/odroid-n2/odroid-n2). The initial board setup used an eMMC preloaded with Ubuntu Mate 22.04 purchased directly from [HardKernel](https://www.hardkernel.com/product-category/memories. Installation was very straightforward.

Unfortunately during an upgrade to Ubuntu 4.9.1, due to a supposed error in the kernel, we lost our wi-fi driver, effectively isolating the robot system and making it useless. To recover, we downloaded the image from [here](https://wiki.odroid.com/getting_started/os_installation_guide#operating_systems_we_re_providing), then flashed using `balenaEtcher` and an eMMC Reader board per instructions [here](https://linuxhint.com/etcher-image-writer-ubuntu-burn-images/). The Ubuntu desktop versions with Mate refused to copy to our eMMC (too big?). However the Ubuntu 22.04-4.9-minimal-odroid-n2 image flashed successfully. The text below describes setup from the minimal configuration.   

The N2 has 4 USB slots that are used for:
 * Keyboard/Mouse
 * WiFi
 * Bluetooth
 * USB2AX (2)

If the boot selector switch is to the left, startup will boot into Pettiboot. To boot from the SD card, move the switch to the right. The filesystems are pre-configured with the root at 64gb, 96% free. Place all user directories under `/home`.

#### Users
The initial root password is `root/odroid`. Change password to provide some minimal security. As `root` add an administrative user `odroid/odroid`.
Create a user for the robot's use . Call it `bert`. Add these to the `sudo`, `dialout` and `uucp` groups. For example:
```
   adduser bert
   usermod -aG sudo bert
```
Add the following to ``~/.bashrc`` and equivalent to ``~/Library/LaunchAgents/environment.plist``:
```
   PS1="\u: "
   export BERT_HOME=/home/bert
   export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-armhf
```
#### Initial Network
The wi-fi connection must be configured before any additional packages can be installed. Our wireless interface is `wlan0`. The command
```
   nmcli device wifi connect "SSID" password "PASSWD"
```
should result in a wireless connection to the internet. (Substituting name and password for the local network,
of course). A reboot may be required. As soon as the network was live, I was presented with an invitation to upgrate to release 24.04.1. I proceeded with the upgrade, only to once again lose the wifi and had to start over again. The lesson here is to avoid the temptation to upgrade to 24.04.1.

Once a WiFi connection has been made, again as `root`,
```
  apt update
  apt upgrade -y
```

then install the `Budgie` desktop. The use of `aptitude` is required because of a dependency issue (which appeared common to all desktops I tried).
```
  add-apt-repository ppa:deadsnakes/ppa
  apt install aptitude
  aptitude install ubuntu-budgie-desktop
  apt install nautilus gnome-terminal
  shutdown -r now

```
If starting from a purchased SD card, remove the `Mate` desktop.
```
  apt purge mate-desktop --autoremove
```

#### Further Configuration

Now that a wi-fi connection is established and desktop installed, continue the configuration tasks that are required even with the pre-looaded SD card.

Set the hostname to `bert`.
```
  hostnamectl set-hostname bert
```

Set the timezone:
```
  timedatectl set-timezone America/Denver
```

 Configure a static IP address. This allows us to connect to the robot even if it comes up "headless". Using the Budgie desktop, under the main menu network icon, Network Settings, edit the WiFi connection via settings icon. On the IPV4 tab, change the Method: to "Manual". Add a static address, e.g. 10.0.0.42; 255.255.255.0; 10.0.0.1. Depending on the network, another common address choice might be: 192.168.1.42. In the same dialog, set Domain Name Servers, comma-separated.
```
  75.75.75.75
  75.75.76.76
```
Validate these setting by inspecting another machine on the same network.


Restart the `odroid`. When running again,
make sure you can ping the new address from a different machine.

Next, make sure these lines exist in `/etc/ssh/sshd_config`.
```
PubkeyAuthentication yes
AuthorizedKeysFile .ssh/authorized_keys
```


Then on each development system on the network, generate SSH keys (if not already done).
```
  ssh-keygen  (use default location, no password)
```
Also on each remote system, add to `/etc/hosts`:
```
  10.0.0.42 bert
```
Also on each remote system (appropriately replacing the username),
```
  cd ~/.ssh
  ssh-copy-id -i id_rsa.pub chuckc@bert
```
If there are old references to `bert` in *known_hosts*, they must be deleted.

Upgrade the operating system for any changes since the image was posted and install missing tools. If these *apt* commands repeatedly throw your wi-fi router off-line, you may be forced to execute them using a direct ethernet connection. These must be run as root (or sudo).

In the `Software Sources` application, enable main (canonical free and open source software)
```
  apt update
  apt upgrade -y
  apt autoremove -y
  apt autoclean -y
  apt install rsync
  apt install sqlite3
  apt install libjssc-java
  apt install libbluetooth-dev
  apt install build-essential
  apt-get install manpages-dev
```
```
   add-apt-repository ppa:xtradeb/apps -y
   apt update
   apt upgrade
   apt install chromium
   apt install firefox
```



As super-user, set the serial port permissions by creating file `/etc/udev/rules.d/50-ttyusb.rules` with the following contents:
```
KERNEL="ttyACM*",MODE="0666"
```
Reboot then
```
  sudo chmod 666 /dev/ttyACM*
```

The reason for ``firefox`` is that we were not able to properly configure the proxy server in ``chromium``, the default browser.

In `/etc/ld.so.conf.d`, add a file named `robot.conf` containing the single line:
```
  /usr/local/robot/lib
```

Edit `/lib/systemd/system/bluetooth.service`
Replace the existing similar line with:
```
   ExecStart=/usr/lib/bluetooth/bluetoothd -C
```

To shutdown,
```
  sudo shutdown -h now
```
Wait until the blue LED has gone out, then unplug.

##### Java
The Java JVM is used to run the application even though it is written in Kotlin. Download the latest Java Development (JDK) version using:
```
  sudo apt update
  sudo apt install openjdk-18-jre-headless
```
Installing the JDK allows us to compile on the Odroid, if necessary. Of course,
we also need the runtime. The java version on the odroid (18) must match the JDK on the development system.

Once the build has been executed on the Development system (and deployed), edit ``/etc/environment``, adding JAVA_HOME and BERT_HOME/bin to the **PATH** variable (before  */usr/bin*):
```
  /usr/lib/jvm/java-18-openjdk-arm64
   /usr/local/robot/bin
```


#### PyPot <a id="pypot"></a>
*PyPot* provides demonstration code and the **herborist** tool that is used to configure Dynamixel stepper motors. Motors must be assigned unique IDs before being made part of the robot. ID's are recorded in `bert.xml`. Documentation may be found [here](https://github.com/poppy-project/herborist).
```
  sudo apt-get install python3-pip
  sudo apt-get install python3-pyqt5
  pip install pypot
  pip install herborist
```

## Software Development <a id="software"/>

The development host is an iMac running OSX Sonoma (14.3.1). The code repository resides on this machine. Code is cross-compiled and downloaded onto the robot target over a WiFi connection. Here is a diagram that summarizes the flow of the build and major tools used:
  ![Build Plan](/images/development_layoutsvg)
  ````                        Development Tools ````

Code is compiled and downloaded onto the robot target over a WiFi connection using *rsync*.

The iMac requires the same Java version as the Odroid (Java 18). It is downloadable from [here](https://www.oracle.com/java/technologies/javase/jdk18-archive-downloads.html). Make sure to download the JDK and install the “Development tools” into the default location (e.g. /usr/local/bin). Extend the system path to include this area. Avoid the temptation to download "the latest" as we need Java 10 for compatibility with the Odroid.

### IntelliJ <a id="intellij"></a>
[toc](#table-of-contents)

##### Installation
*IntelliJ* is a powerful Integrated Development Environment (IDE) for Java, and Kotlin from Jet Brains. It can be downloaded [here]( https://www.jetbrains.com/idea/download/?section=mac). Make sure to download the community edition which is completely free. At the time of this writing, the latest version is “2022.2.1”.
Start *IntelliJ* and point it to the root of the `git` repository. Once started choose `File/Upload All From Disk` to synchronize with the repository. An internet connection is required as all third party libraries will be downloaded directly from public repositories during the build. Use the build configuration `install` to perform a complete build.

Our *IntelliJ* environment makes use of the `ANTLR` and `Python` plugins. See the menu page *IntelliJ* `Idea/Settings` to load.

##### Projects
`Bert` is a multi-module project. Each code group is its own project as follows:
  - `bertApp`: This contains the main class for the "Bert" application. It also contains entry classes for stand-alone tests.
  - `buildSrc`: _IntelliJ_ executes this project first. It is used to define plugins with dependencies and repositories that are common to the remaining source modules.
  - `common`: _common_ code contains utility functions and constant definitions used throughout the application.
  - `command`: _command_ is the module that interfaces with the tablet to
  acquire command and request strings. It communicates these to the _Dispatcher_
  and obtains responses.
  - `configuration`: This area is a collection of files that configure the robot. These include: scripts for installing and running the SQL database for poses and actions.
  It contains XML hardware definitions with dimensions and connectivity information.
  - `control`: Parse the URDF file and apply various control algorithms.
  - `database`: This is a common area for interaction with the SQLite database.
  - `dispatcher`: The _dispatcher_ is the central module for handling commands,
  passing them along to the _Motor_ controllers and then forwarding results
  to the original requestor.
  - `motor`: Java code for control of the various servo motors on the robot. These are executed by the _dispatcher_ application.
  - `speech`: This project is the interface between the _command_ module and syntax parser.
  - `syntax`: This project holds the _ANTLR_ code which defines the recognized syntax of "natural
    language" request strings.
  - `terminal`: _terminal_ is a separate application for command-line control of the robot. It provides the same interface as the Android tablet except in a typed instead of spoken form.


When complete the project workspace should look like:
![IntelliJ Setup](/images/intellij_setup.png)
```                  IntelliJ Projects     ```


##### Gradle Scripts
`Gradle` is a build system which follows a "convention over configuration" style. Each project has its own Kotlin build script named `gradle-build.kts`.


##### Scripts
The *IntelliJ* `Configuration` project has a collection of *bash* scripts and other configuration files as described below. When the project is built these files will be properly placed into `$BERT_HOME` subdirectories. These scripts perform both installation and run-time functions.

`bin`
 - bert - Execute *bertApp* from the command line. This is meant to be run on the Odroid target system
 - bert-blueserver - Script to start and stop the bluetooth server
 - bert-server - Script for starting and stopping the *bert* robot code
 - clear_logs - Remove current log files in preparation for the next test sequence.
 - install_blueserver_init_scripts - Configure the scripts that start and stop the bluetooth server. Thus
 must be run as superuser
 - install_blueserver_init_scripts - Configure the scripts that start and stop the robot. Thus
 must be run as superuser
 - mkdatabase - Create the SQLite *bert.db* database on either development or deployment system
 - start_bert - Start the robot
 - stop_bert - Stop the robot
 - test_port - test the all the serial ports

`csv`
 - Pose.csv -

`etc`
 - bert.xml -      Robot properties including a list of all controllable joints
 - urdf.xml -      A complete description of the skeletal connectivity

`pylib`
 - testport.py - Command-line tool to test serial ports based on the "herborist" tool

`sbin`
- bertdev - Execute *bertApp* in offline mode (no bluetooth nor serial) for testing.
- deploy - copy executable and configuration files onto the Odroid target
- install_odroid_source - copy code that must be compiled on the Odroid from the development source area
- unpack_distribution - This script is executed as a final step by the build. It unpacks library *.jar* files from the build into a proper distribution directories development machine.

`sql`
- createTables.sql - SQL commands used by *mkdatabase* to create tables


##### ANTLR
*ANTLR* is a parsing framework used for understanding natural language. Use the plugin available from the
*IntelliJ* settings page.

##### Git
*git* is the source code control tool. For development the repository is checked out as:
```
git clone http://[token]@github.com/chuckcoughlin/bert.git bert
```
where [token] is the private user token for the repository.

##### Python
*Python* is required for the *herborist* Dynamixel configuration application. Use the plugin available from the
*IntelliJ* settings page.

##### Modularized Jar Files
A few of the jar files were not available as `gradle` dependencies or were built using a too-old version of Java. These were updated manually for Java10 module compatibility.

Thanks to [Michael Easter](https://github.com/codetojoy/easter_eggs_for_java_9/blob/master/egg_34_stack_overflow_47727869/run.sh) for the following example that shows how to modularize the ``Jackson`` jar files.
A local directory holding the class files is taken as the root directory. This example uses 3 original non-modularized *Jackson* jar files have been downloaded into ``jars`as the starting point. Modularized results will be stored into ``mods``. In this particular example the modules are inter-dependent. In simpler cases only the first third of these steps are applicable.
```
   ROOT=`pwd`
   jdeps --generate-module-info work jars/jackson-core-2.9.7.jar
   cp jars/jackson-core-2.9.7.jar mods/jackson-core.jar
   rm -rf classes
   mkdir classes
   cd classes
   jar -xf ${ROOT}/jars/jackson-core-2.9.7.jar
   cd ${ROOT}/work/com.fasterxml.jackson.core
   javac -p jackson.core -d ${ARCHIVE}/classes module-info.java
   jar -uf ${ROOT}/mods/jackson-core.jar -C ${ROOT}/classes module-info.class

   cd $ROOT
   jdeps --generate-module-info work jars/jackson-annotations-2.9.7.jar
   cp jars/jackson-annotations-2.9.7.jar mods/jackson-annotations.jar
   rm -rf classes
   mkdir classes
   cd classes
   jar -xf ${ROOT}/jars/jackson-annotations-2.9.7.jar
   cd ${ROOT}/work/com.fasterxml.jackson.annotation
   javac -p jackson.annotations -d ${ROOT}/classes module-info.java
   jar -uf ${ROOT}/mods/jackson-annotations.jar -C ${ROOT}/classes module-info.class

   cd $ROOT
   jdeps --module-path ${generationrobots}/mods --add-modules com.fasterxml.jackson.annotation,com.fasterxml.jackson.core --generate-module-info work jars/jackson-databind-2.9.7.jar
   cp jars/jackson-databind-2.9.7.jar mods/jackson-databind.jar
   rm -rf classes
   mkdir classes
   cd classes
   jar -xf ${ROOT}/jars/jackson-databind-2.9.7.jar
   cd ${ROOT}/work/com.fasterxml.jackson.databin
   javac --module-path ${ARCHIVE}/mods --add-modules com.fasterxml.jackson.annotation,com.fasterxml.jackson.core -d ${ARCHIVE}/classes module-info.java
   jar -uf ${ROOT}/mods/jackson-databind.jar -C ${ROOT}/classes module-info.class
   ${ROOT}
   rm -rf work classes
```


### Android Studio <a id="android"></a>
[toc](#table-of-contents)

##### General
The tablet application, ***BertSpeak*** is the only Human Machine Interface (HMI) in normal operation.  Most importantly, it receives and analyzes voice commands, forming the only control interface. Additionally it maintains the voice transcript and displays results from the robot's internal health monitor. The tablet is a Samsung Galaxy Tab S8+, SM-X800, 10" Android 13 (SDK version 33).

The control application is a standard Android application built using Android Studio 3.4. (The studio may be downloaded from http://developer.android.com.) The studio requires a minor configuration of the host build system. Make the Android home environment variable available by adding to ~/.bashrc

    ```ANDROID_HOME=~/Library/Androd/sdk```

The password to the application keystore is: ```Andr0id```

##### Voice Commands
Voice commands are implemented purely via the Android tablet using the builtin speech-to-text features of Android. On the tablet, the ***BertSpeak*** app must be given microphone permissions.

For production of speech from robot output,  text-to-speech can be configured in the settings under "Accessibility". Parameters include languages supported and characteristics of the speaker such as volume.

##### Network Configuration
Communication between the tablet and main robot processor is over Bluetooth. On the tablet's settings "Connections" page, make sure that bluetooth is enabled. Under "More Connection Settings", enable "Nearby device scanning". Network or device pairing selections are made from menus accessed from the main screen.

Pairing with the robot must be completed before the ***BertSpeak*** application is started. Pairing may be initiated either from the robot or the tablet.

Note that the emulator does not support Bluetooth and is, therefore, not available as a test tool.

#### Other Tools <a id="other"/>
We make use of the following freely-available applications:
  * [Atom](https://atom.en.softonic.com/mac) - Text editor, handles markdown syntax
  * [OpenSCAD](http://www.openscad.org) - Construct 3D CAD drawings in script. Export in .stl format. This is useful where the parts are composed from simple geometric shapes.
  * [MeshLab](http://www.meshlab.net) - Use this tool to optimize and convert between 3D formats.
  * [Blender](http://www.blender.org) - Create 3D models. Use `Blender-2.8` for modeling parts that are not part of the
  original `poppy` design. Export in .obj format for printing.
  * [pypot.dynamixel](https://poppy-project.github.io/pypot/dynamixel.html) - Configure Dynamixel servos using a command-line interface.
  * [Boxy](https://apps.apple.com/us/app/boxy-svg/id611658502) - Construct SVG diagrams and charts ($19.95).
  * [iCircuit](https://itunes.apple.com/us/app/icircuit/id454347770?ls=1&mt=12) - Draw and analyze electrical circuits.

##### Blender Cheat Sheet
*Blender* is a sophisticated (read complex)
modeling application. There are numerous
keyboard shortcuts that not at all obvious from the interface. Here are a few:
  * ctl-a - launch apply menu
  * D - duplicate selected object
  * L - link (ctl/shift)
  * g - enter mode for direct manipulation of objects. Follow with x,y,z,s or r. Exit with <ENTER>.
  * m - create collection of selected objects
  * n - expand the navigation widget to allow
  precise positioning of objects
  * r - rotate (after "g")
  * s - scale (after "g")
  * t - toggle left-side toolbar


##### Useful Commands I Always Forget

  To query the module in a jar file: `jar --file="<file>" --describe-module`
