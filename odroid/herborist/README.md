# Herborist
Herborist is a graphical tool that helps you detect and configure Dynamixel motors for Poppy creatures or other robotic projects. 

More precisely, Herborist can be used to:

* Find and identify available serial ports
* Scan multiple baud rates to find all connected motors
* Modify the EEPROM configuration (of single or multiple motors)
* Make motors move (e.g. to test the angle limits).

**Compatible motors:** Identical to `pypot`: MX-106, MX-64, MX-28, MX-12, AX-12, AX-18, RX-24, RX-28, RX-64, XL-320, SR-RH4D, EX-106. Derivated versions are also supported (e.g. MX-28AT, MX-28R, MX-28T, ...).

Install herborist with `pip install herborist` and run it with command `herborist`:

![](/doc/img/herborist.png)
