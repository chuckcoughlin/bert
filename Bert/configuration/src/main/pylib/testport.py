"""
  Command-line port test based on the "herborist" tool.
"""
import sys
import threading
import pypot.dynamixel

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
class PortTestApp():
    def __init__(self, argv):
        self.baudrate = 1000000
        self.id_range = range(1,60)
        self.port = '/dev/ttyACM0'
        self.protocol = 'MX'
  
    # Run the test
    def execute(self):
        # Scan the specified port
        self.scan()
        
    def scan(self):
        dxl_io = get_dxl_connection(self.port, self.baudrate)

        for id in self.id_range:
            if dxl_io.ping(id):
                model = dxl_io.get_model((id, ))[0]
                print "Got ping from "+str(id)

        release_dxl_connection()
        
        
        
def main():
    app = PortTestApp(sys.argv)
    app.execute()
    sys.exit(0)

    __lock.acquire()


if __name__ == '__main__':
    main()