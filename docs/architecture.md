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

## Appendices <a id="appendices"/>
#### Why Java?<a id="whyjava"/>
In the "Poppy" [thesis](https://hal.inria.fr/tel-01104641v1/document) (section 7.4.1 and following), the author considers use of the Robot Operating System (ROS) for the core software and concludes that it is overly complex and inefficient. This coincides with my own experience with [sarah-bella](https://github.com/chuckcoughlin/sarah-bella). Moreover, I discovered that, at least with Android, ROS messaging was not reliable. Messages were dropped under high load, a situation not acceptable for control applications.

This same author noted that the "Pypot" software developed in Python for Poppy had severe performance limitations that placed strict limits on its design.

While I can't simply try all the designs, [YARP](http://www.yarp.it/index.html) used by the "iCub" project seemed closest to my perceived needs. It is written in C++ and is loaded with features that I'll never use. Given my failure to create a cross-compilation environment, I've decided to let a simplified *YARP* serve as an inspiration for a Java-based solution. For simplicity, the ancillary code will be Java also.

Why did I select Java for this code when the iCub project chose Python?
  * Familiarity - over 2 decades of working with Java
  * Debugging - problems are discovered by the compiler rather than run-time
  * Performance - Java executes an order of magnitude faster than Python
  * Threading - the Java threading model is more straightforward (IMHO)
  * Cross-compiling - difficulties creating Odroid executables preclude use of C++.



  #### Failures <a id="failures"/>
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
