#!/usr/bin/env python
"""
   Copyright 2024. Charles Coughlin. All Rights Reserved.
   GPL3 License because of PyPot
   Command-line tool to read a Dynamixel motor configuration

   Examples:
  dxl_write --type=MX-28 --id=23 --angle-limit=(-100, 100) --zeroposition --port=/dev/ttyAMA0
  dxl_write --help
"""

import sys
import time

from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter

from pypot.dynamixel.conversion import dynamixelModels
from pypot.dynamixel import DxlIO, Dxl320IO, get_available_ports
from pypot.utils import flushed_print as print


def check(pred, msg):
    if not pred:
        print(msg)
        print('Exiting now...')
        sys.exit(1)


def main():
    available_ports = get_available_ports()
    default_port = available_ports[0] if available_ports else None
    parser = ArgumentParser(description='Configuration tool for dynamixel motors ',
                            formatter_class=ArgumentDefaultsHelpFormatter)

    parser.add_argument('--id', type=int, required=True,
                        help='Chosen motor id.')
    parser.add_argument('--type', type=str, required=True,
                        choices=list(dynamixelModels.values()),
                        help='Type of the motor to configure.')
    parser.add_argument('--port', type=str,
                        choices=available_ports + ['auto'], default=default_port,
                        help='Serial port connected to the motor.')
    parser.add_argument('--return-delay-time', type=int,
                        help='Set new return delay time.')
    parser.add_argument('--wheel-mode', type=bool, default=False,
                        help='Set wheel mode.')
    parser.add_argument('--angle-limit', type=float, nargs=2,
                        help='Set new angle limit.')
    parser.add_argument('--goto-zero', action='store_true',
                        help='Go to zero position after configuring the motor')
    parser.add_argument('--timeout', type=int, default=2,
                        help='Timeout for the motor config (seconds)')
    args = parser.parse_args()

    check(1 <= args.id <= 253,
          'Motor id must be in range [1:253]')

    check(available_ports,
          'Could not find an available serial port!')

    protocol = 2 if args.type in 'XL-320' else 1
    serial_port = default_port if args.port == "auto" else args.port
    DxlIOPort = DxlIO if protocol == 1 else Dxl320IO


    factory_baudrate = 57600 if args.type.startswith('MX') else 1000000

    # Wait for the motor to "reboot..."
    for _ in range(10):
        with DxlIOPort(serial_port, baudrate=factory_baudrate, timeout=args.timeout) as io:
            if io.ping(1):
                break

            time.sleep(.5)
    else:
        print('Could not communicate with the motor...')
        print('Make sure one (and only one) is connected and try again')
        print('If the issue persists, use Dynamixel wizard to attempt a firmware recovery')
        sys.exit(1)

    # Switch to 1M bauds
    if args.type.startswith('MX') or args.type.startswith('SR'):
        print('Changing to 1M bauds...')
        with DxlIO(serial_port, baudrate=factory_baudrate, timeout=args.timeout) as io:
            io.change_baudrate({1: 1000000})

        time.sleep(.5)
        print('Done!')

    # Set return delay time
    if args.return_delay_time is not None:
        print('Changing return delay time to {}...'.format(args.return_delay_time))
        with DxlIOPort(serial_port, timeout=args.timeout) as io:
            io.set_return_delay_time({args.id: args.return_delay_time})

            time.sleep(.5)
            check(io.get_return_delay_time([args.id])[0] == args.return_delay_time,
                  'Could not set return delay time to {}'.format(args.return_delay_time))
        print('Done!')

    # Set wheel Mode
    if args.wheel_mode == True:
        print('Set wheel mode')
        with DxlIOPort(serial_port, timeout=args.timeout) as io:
            io.set_control_mode({args.id :'wheel'})

            time.sleep(.5)
            check(io.get_control_mode([args.id])[0] == 'wheel',
                  'Could not set wheel Mode')
        print('Done!')


    # Set Angle Limit
    if args.angle_limit is not None:
        print('Changing angle limit to {}...'.format(args.angle_limit))
        with DxlIOPort(serial_port, timeout=args.timeout) as io:
            io.set_angle_limit({args.id: args.angle_limit})

            time.sleep(.5)
            check(all(map(lambda p1, p2: abs(p1 - p2) < 1.,
                          io.get_angle_limit([args.id])[0],
                          args.angle_limit)),
                  'Could not change angle limit to {}'.format(args.angle_limit))
        print('Done!')

    # GOTO ZERO
    if args.goto_zero:
        print('Going to position 0...')
        with DxlIOPort(serial_port, timeout=args.timeout) as io:
            io.set_moving_speed({args.id: 100.0})
            io.set_goal_position({args.id: 0.0})

            time.sleep(2.0)
            check(abs(io.get_present_position([args.id])[0]) < 5,
                  'Could not go to 0 position')

        print('Done!')


if __name__ == '__main__':
    main()