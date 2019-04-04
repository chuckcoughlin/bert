/*
 * A RFComm client. The address of my Android tablet is hardcoded.
 * Albert Huang - "Bluetooth for Programmers"
 */
#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>

int main(int argc, char **argv) {
    struct sockaddr_rc addr = { 0 };
    int s, status;
    //char dest[18] = "23:A1:57:F8:A1:89";  // ??
    char dest[18] = "C0:D3:C0:72:94:6A";

    // allocate a socket
    s = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);

    // set the connection parameters (who to connect to)
    addr.rc_family = AF_BLUETOOTH;
    addr.rc_channel = (uint8_t) 1;
    str2ba( dest, &addr.rc_bdaddr );

    // connect to server
    printf("rfcommclient: connecting ...\n");
    status = connect(s, (struct sockaddr *)&addr, sizeof(addr));

    if(status){
        printf("rfcommclient: failed to connect the device!\n");
        return -1;
    }


    do {
        len = read(s, buf, sizeof buf);

     if( len>0 ) {
         buf[len]=0;
         printf("%s\n",buf);
         write(s, buf, strlen(buf));
     }
    } while(len>0)nt");

    close(s);
    return 0;
}
