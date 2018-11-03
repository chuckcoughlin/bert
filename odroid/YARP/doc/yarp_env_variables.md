YARP Environmental Variables {#yarp_env_variables}
============================

YARP uses several to changes its behavior at runtime.
All the Environmental Variables used by YARP are listed in this page.


Logger and print configuration
==============================

| Environmental variable        | Description | Related documentation page |
|:----------------------       :|:-----------:|:--------------------------:|
| `YARP_QUIET`                  | If this variables exists and is set to a positive integer, it disables the (internal) YARP messages prints. Note: this variable **do not** modify the behavior of `yError`, `yDebug`, ... . | |
| `YARP_VERBOSE`                | If this variables exists and is set to a nonnegative integer, it sets the verbosity level (the higher the integer, the more messages will be printed) for YARP messages prints. This variable is ignored if `YARP_QUIET` is set to a positive integer. Note: this variable **do not** modify the behavior of `yError`, `yDebug`, ... . | |
| `YARP_LOGGER_STREAM`          | By default, the `yError`, `yDebug`, ... messages are printed on the `stderr` stream. If this variables exists and is set to `stdout`, the messages will be printed to `stdout` instead. | |
| `YARP_COLORED_OUTPUT`         | If this variable exists and is set to 1, it enables the YARP colored prints. Otherwise disable the colored prints. | |
| `YARP_VERBOSE_OUTPUT`         | If this variable exists and is set to 1, it adds to the YARP prints the filename, the line of the source and the function from which the message is printed. | |
| `YARP_TRACE_ENABLE`           | If this variable exists and is set to 1, it enables the YARP trace prints. Otherwise disable the trace prints. | |
| `YARP_DEBUG_ENABLE`           | If this variable exists and is set to 0, it disables the YARP debug prints. Otherwise leaves them enabled. | |
| `YARP_FORWARD_LOG_ENABLE`     | If this variable exists and is set to 1, enables the forwarding of log over ports to be used by the yarplogger. Otherwise disable the forwarding. | |


Configuration files
===================

| Environmental variable        | Description | Related documentation page |
|:-----------------------------:|:-----------:|:--------------------------:|
| `YARP_CONFIG_HOME`            | Location where user config files are stored. | \ref resource_finder_spec |
| `XDG_CONFIG_HOME`             | Location where user config files are stored (only if `YARP_CONFIG_HOME` is not set). | \ref resource_finder_spec |
| `YARP_DATA_HOME`              | Location where user data files are stored. | \ref resource_finder_spec |
| `XDG_DATA_HOME`               | Location where user data files are stored (only if `YARP_DATA_HOME` is not set). | \ref resource_finder_spec |
| `YARP_CONFIG_DIRS`            | Locations where system administrator data and config files are stored. | \ref resource_finder_spec |
| `XDG_CONFIG_DIRS`             | Locations where system administrator data and config files are stored (only if `YARP_CONFIG_DIRS` is not set). | \ref resource_finder_spec |
| `YARP_DATA_DIRS`              | Locations where installed data and config files are stored. | \ref resource_finder_spec |
| `XDG_DATA_DIRS`               | Locations where installed data and config files are stored (only if `YARP_DATA_DIRS` is not set). | \ref resource_finder_spec |
| `YARP_ROBOT_NAME`             | Variable used to refer to the name of the specific robot used in the system, to load its specific configuration files. | \ref yarp_data_dirs |

Note that more platform-specific non-YARP environmental variables are used when
searching for YARP configuration files.
See \ref resource_finder_spec for a full description of the environmental
variables used for finding configuration files.


UDP Carrier configuration
=========================

| Environmental variable        | Description | Related documentation page |
|:-----------------------------:|:-----------:|:--------------------------:|
| `YARP_DGRAM_SIZE`             | Large UDP messages are broken into a series of datagrams. This variable control the size of this datagrams in bytes for both the `udp` and `mcast` carriers. | \ref carrier_config |
| `YARP_MCAST_SIZE`             | This variable controls the size of datagrams in bytes for just the `mcast` carrier. | \ref carrier_config  |
| `YARP_UDP_SIZE`               | This variable controls the size of datagrams in bytes for just the `udp` carrier.  | \ref carrier_config |
| `YARP_DGRAM_BUFFER_SIZE`      | This variable controls the size in bytes of the UDP socket buffer, both for receiving and sending. | |
| `YARP_DGRAM_RECV_BUFFER_SIZE` | This variable controls the size in bytes of the UDP socket receiving buffer. | |
| `YARP_DGRAM_SND_BUFFER_SIZE`  | This variable controls the size in bytes of the UDP socket sending buffer. | |


ROS configuration
=================

| Environmental variable        | Description | Related documentation page |
|:-----------------------------:|:-----------:|:--------------------------:|
| `ROS_MASTER_URI`              | If a ROS1 roscore is present in the network, this variable should contain its URI. If a ROS1 installation is present on the machine, this variable is typically set by ROS configuration scripts. | http://wiki.ros.org/ROS/EnvironmentVariables |
| `YARP_USE_ROS`                | If this variable is set to something different from an empty string, the `yarpserver` will always be launched with `ROS` support. For example ``YARP_USE_ROS=1 yarpserver` is equivalent to `yarpserver --ros`.    | \ref yarp_with_ros_nameservers |


Misc configuration
==================

| Environmental variable        | Description | Related documentation page |
|:-----------------------------:|:-----------:|:--------------------------:|
| `YARP_CLOCK`                  | If this variable is set, the default YARP time facilities will read the time from the yarp port whose name is contained in this variable. | |
| `YARP_PORT_PREFIX`            | If this variable is set, its content is prepended to the name of the port whenever a port is opened.  For example: `YARP_PORT_PREFIX=/prefix yarp read /read` will open a port named `/prefix/read` for shells where this syntax is permitted. | |
| `YARP_RENAME<???>`            | Suppose a program has a port called `/foo/bar` and there is no way provided to change the name of that port other than source code modification.  The port name can be changed entirely setting the `YARP_RENAME_foo_bar` variable to the desired name of the port. For example: `YARP_RENAME_read=/logger yarp read /read` will open a port named `/logger` for shells where this syntax is permitted.  Renames (if present) are applied before prefixes specified with `YARP_PORT_PREFIX` (if present). | |
| `YARP_NAMESPACE`              | If this variable is set, its content is used by YARP as namespace, overriding the value set by `yarp namespace` | |
| `YARP_IP`                     | If this variable is set, it forces the IP address used for registering YARP ports to be in a particular family.  Prefixes are allowed.  For example, on a machine with a 10.11.4.4 address and a 192.168.1.10 address, seeting YARP_IP to 192 or 192.168 or 192.168.1.10 all result in the 192.xxx.xxx.xxx IP address being used. | |


Other
=====

Environment variables related to YARP, but not consumed by YARP itself

| Environmental variable        | Description | Related documentation page |
|:-----------------------------:|:-----------:|:--------------------------:|
| `YARP_DIR`                    | Variable consumed by CMake to find the location of YARP. See https://cmake.org/cmake/help/latest/command/find_package.html for more info | |
| `YARP_ROOT`                   | Variable that usually points to the source directory of the YARP repository, used typically in YARP documentation. | |


Deprecated Environmental Variables
==================================

| Environmental variable        | Description | Related documentation page |
|:-----------------------------:|:-----------:|:--------------------------:|
| `YARP_POLICY`                 | Legacy variable used to change the behavior of the ResourceFinder. Deprecated since YARP 2.3.65 | |
| `YARP_STACK_SIZE`             | Default stack size (in bytes) for YARP threads. Deprecated since YARP 3.0.0 | |


Build system
============

| Environmental variable        | Description | Related documentation page |
|:-----------------------------:|:-----------:|:--------------------------:|
| `CLICOLOR_FORCE`              | Enable colors in CMake output | https://bixense.com/clicolors/ |
