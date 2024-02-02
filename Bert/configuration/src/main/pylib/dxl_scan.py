#!/usr/bin/env python
"""
   Copyright 2024. Charles Coughlin. All Rights Reserved.
   GPL2 License because of PyPot

  Command-line tool to discover which Dynamixel motors are configured.
  We check both /dev/ttyACM0 and /dev/ttyACM1.
  There are no command-line arguments
"""

import pypot.dynamixel
import sys
import threading
import time

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

# Main class. Execute a fixed sequence of commands.
class DxlScan():
    def __init__(self, argv):
        self.baudrate = 1000000
        self.id_range = range(1,60)
  
    # Run the test
    def execute(self):
        # Scan the specified port and protocol
        self.scan('/dev/ttyACM0')
        self.scan('/dev/ttyACM1')
        
    def scan(self,port):
        dxl_io = get_dxl_connection(port, self.baudrate)

        for id in self.id_range:
            if dxl_io.ping(id):
                model = dxl_io.get_model((id, ))[0]
                print( "dxl_scan: Found "+port+":"+str(id)+" ("+str(model)+")")

        release_dxl_connection()

        
def main():
    app = DxlScan(sys.argv)
    app.execute()
    sys.exit(0)

    __lock.acquire()


if __name__ == '__main__':
    main()