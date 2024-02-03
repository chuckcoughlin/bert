#!/usr/bin/env python
"""
   Copyright 2024. Charles Coughlin. All Rights Reserved.
   GPL3 License because of PyPot

   Command-line tool to read a Dynamixel motor configuration.
   Legal ports are: ACM0, ACM1

   Examples:
  dxl_read --port=/dev/ttyACM0 --id=23
  dxl_read               // Usage
"""

import pypot.dynamixel
import sys
import threading
import time
from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter
from pypot.utils import flushed_print as print

__dxl_io = None
__lock = threading.Lock()

# For MX protocol
def get_dxl_connection(port, baudrate):
    global __dxl_io
    __lock.acquire()
    __dxl_io = pypot.dynamixel.DxlIO(port, baudrate, use_sync_read=False)
    return __dxl_io


def release_dxl_connection():
    __dxl_io.close()
    __lock.release()

def check(pred, msg):
    if not pred:
        print(msg)
        sys.exit(1)

def main():
    parser = ArgumentParser(description='Read a dynamixel motor configuration ',
                            formatter_class=ArgumentDefaultsHelpFormatter)

    parser.add_argument('--id', type=int, required=True,
                        help='Chosen motor id.')
    parser.add_argument('--port', type=str, required=True,
                        choices=['/dev/ttyACM0','/dev/ttyACM1'],
                        help='Serial port connected to the motor.')
    args = parser.parse_args()


    check(1 <= args.id <= 253,
          'Motor id must be in range [1:253]')

    check(args.port=="/dev/ttyACM0" or args.port=="/dev/ttyACM1",
          'Port must be one of /dev/ttyACM0 or /dev/ttyACM1')

    baudrate = 1000000      # Assume controllers baud rate set to 1000000.
    id        = args.id
    protocol = 1            # Always 1
    serial_port = args.port
    timeout   = 2           # Secs


    dxl_io = get_dxl_connection(serial_port,baudrate)
    ids = dxl_io.scan([id])
    if len(ids) > 0 :
        print("   Motor -------------- "+str(serial_port)+"("+str(id)+") --------------")
        print("       Model         : "+str(dxl_io.get_model(ids)[0]))
        print("       Return delay  : "+str(dxl_io.get_return_delay_time(ids)[0]))
        print("       Firmware      :"+str(dxl_io.get_firmware(ids)[0]))
        print("       Max torque    :"+str(dxl_io.get_max_torque(ids)[0]))
        print("       Angle limit   :"+str(dxl_io.get_angle_limit(ids)[0]))
        print("       Goal position :"+str(dxl_io.get_goal_position(ids)[0]))
        print("       Current pos   :"+str(dxl_io.get_present_position(ids)[0]))
        print("       Torque enable :"+str(dxl_io.is_torque_enabled(ids)[0]))
    else:
        print("   motor "+str(id)+" not found")
    release_dxl_connection()
    sys.exit(0)


if __name__ == '__main__':
    main()