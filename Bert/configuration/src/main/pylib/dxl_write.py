#!/usr/bin/env python
"""
   Copyright 2024. Charles Coughlin. All Rights Reserved.
   GPL3 License because of PyPot

  Command-line tool to configure a Dynamixel motor, based on
  pypot dxlconfig.py (removing factory reset)

   Examples:
  dxl_write --id=23 --port=/dev/ttyACM0 --angle-limit=(-100, 100) --zeroposition
  dxl_write                // Usage

"""
import pypot.dynamixel
import sys
import threading
import time
from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter
from pypot.dynamixel.conversion import dynamixelModels
from pypot.dynamixel import DxlIO, Dxl320IO, get_available_ports
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
    parser = ArgumentParser(description='Configure a dynamixel motor ',
                            formatter_class=ArgumentDefaultsHelpFormatter)

    parser.add_argument('--id', type=int, required=True,
                        help='Chosen motor id.')
    parser.add_argument('--port', type=str, required=True,
                        choices=['/dev/ttyACM0','/dev/ttyACM1'],
                        help='Serial port connected to the motor.')

    parser.add_argument('--angle-limit', type=float, nargs=2,
                        help='Set new angle limits.')
    parser.add_argument('--goal-position', type=float,
                        help='Set goal position.')
    parser.add_argument('--max-torque', type=float,
                        help='Set max torque.')
    parser.add_argument('--moving-speed', type=float,
                        help='Set moving speed.')
    parser.add_argument('--return-delay-time', type=int,
                        help='Set new return delay time.')
    parser.add_argument('--timeout', type=int, default=2,
                        help='Timeout for the motor config (seconds)')
    parser.add_argument('--torque-enable', type=bool, default=False,
                        help='Enable torque.')
    parser.add_argument('--wheel-mode', type=bool, default=False,
                        help='Set wheel mode.')

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
        print("   Motor -------------- "+str(serial_port)+"("+str(id)+") "+str(dxl_io.get_model(ids)[0])+" --------------")
        # Set angle limit
        if args.angle_limit is not None:
            print('Changing angle limits to {}...'.format(args.angle_limit))
            dxl_io.set_angle_limit({args.id: args.angle_limit})

            time.sleep(.5)
            check(all(map(lambda p1, p2: abs(p1 - p2) < 1.,
                          dxl_io.get_angle_limit([args.id])[0],args.angle_limit)),
                  'Could not change angle limit to {}'.format(args.angle_limit))
        # Set goal position
        if args.goal_position is not None:
            print('  Set gol position to {}...'.format(args.goal_position))
            dxl_io.set_moving_speed({args.id: 100.0})
            dxl_io.set_goal_position({args.id: args.goal_position})
            time.sleep(2)
            check(dxl_io.get_present_position([args.id])[0] == args.return_delay_time,
                  'Could not reach goal position {}'.format(args.return_delay_time))
        # Set moving speed
        if args.moving_speed is not None:
            print('  Set moving speed to {}...'.format(args.moving_speed))
            dxl_io.set_moving_speed({args.id: args.max_torque})
            time.sleep(.5)
            check(dxl_io.get_moving_speed([args.id])[0] == args.moving_speed,
                  'Could not set max torque to {}'.format(args.moving_speed))
        # Set max torque
        if args.max_torque is not None:
            print('  Set max torque to {}...'.format(args.max_torque))
            dxl_io.set_max_torque({args.id: args.max_torque})
            time.sleep(.5)
            check(dxl_io.get_max_torque([args.id])[0] == args.max_torque,
                  'Could not set max torque to {}'.format(args.max_torque))
        # Set return delay time
        if args.return_delay_time is not None:
            print('  Set delay time to {}...'.format(args.return_delay_time))
            dxl_io.set_return_delay_time({args.id: args.return_delay_time})
            time.sleep(.5)
            check(dxl_io.get_return_delay_time([args.id])[0] == args.return_delay_time,
                  'Could not set return delay time to {}'.format(args.return_delay_time))
        # Enable torque
        if args.torque_enable == True:
            print('  Enable torque')
            dxl_io.torque_enable({args.id})
            time.sleep(.5)
            check(dxl_io.is_torque_enabled([args.id])[0],
                  'Could not enable torque')
        # Set wheel mode
        if args.wheel_mode == True:
            print('  Set wheel mode')    # Only option reasonable for robot
            dxl_io.set_control_mode({args.id :'wheel'})
            time.sleep(.5)
            check(dxl_io.get_control_mode([args.id])[0] == 'wheel',
                  'Could not set wheel mode')

    else:
        print("   motor "+str(id)+" not found")

    release_dxl_connection()
    print('Done!')
    sys.exit(0)


if __name__ == '__main__':
    main()