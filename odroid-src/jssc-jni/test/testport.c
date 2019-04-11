/*
 * Test the ability to read/write to a port.
 */

#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <termios.h>
#include <time.h>
#include <stdbool.h>
#include <string.h>
#include <strings.h>


#define BAUD_RATE 1000000
#define PORT_NAME "/dev/ttyACM1"

int set_interface_attribs(int fd, int speed)
{
    struct termios tty;

    if (tcgetattr(fd, &tty) < 0) {
        printf("Error from tcgetattr: %s\n", strerror(errno));
        return -1;
    }

    cfsetospeed(&tty, (speed_t)speed);
    cfsetispeed(&tty, (speed_t)speed);

    tty.c_cflag |= (CLOCAL | CREAD);    /* ignore modem controls */
    tty.c_cflag &= ~CSIZE;
    tty.c_cflag |= CS8;         /* 8-bit characters */
    tty.c_cflag &= ~PARENB;     /* no parity bit */
    tty.c_cflag &= ~CSTOPB;     /* only need 1 stop bit */
    tty.c_cflag &= ~CRTSCTS;    /* no hardware flowcontrol */

    /* setup for non-canonical mode */
    tty.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
    tty.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
    tty.c_oflag &= ~OPOST;

    /* fetch bytes as they become available */
    tty.c_cc[VMIN] = 1;
    tty.c_cc[VTIME] = 1;

    if (tcsetattr(fd, TCSANOW, &tty) != 0) {
        printf("Error from tcsetattr: %s\n", strerror(errno));
        return -1;
    }
    return 0;
}

void set_mincount(int fd, int mcount) {
    struct termios tty;

    if (tcgetattr(fd, &tty) < 0) {
        printf("Error tcgetattr: %s\n", strerror(errno));
        return;
    }

    tty.c_cc[VMIN] = mcount ? 1 : 0;
    tty.c_cc[VTIME] = 5;        /* half second timer */

    if (tcsetattr(fd, TCSANOW, &tty) < 0)
        printf("Error tcsetattr: %s\n", strerror(errno));
}

// There are no options, the test is hard-coded
int main(int argc, char* argv[]) {
	// open port
	char* portname = PORT_NAME;
	int fd;
	int wlen;

    fd = open(portname, O_RDWR | O_NOCTTY | O_SYNC);
    if (fd < 0) {
        printf("testport: error opening %s: %s\n", portname, strerror(errno));
        return -1;
    }
	printf("testport: successfully opened %s\n",portname);

	/*baudrate 1000000, 8 bits, no parity, 1 stop bit */
	set_interface_attribs(fd, B1000000);
	set_mincount(fd, 0);                /* set to pure timed read */
	printf("testport: set baudrate and timed read\n");

    /* BROADCAST PING */
	unsigned char bbuf[10];
	bbuf[0] = 0xFF;
	bbuf[1] = 0xFF;
	bbuf[2] = 0xFD;
	bbuf[3] = 0x00;
	bbuf[4] = 0xFE;
	bbuf[5] = 0x03;
	bbuf[6] = 0x00;
	bbuf[7] = 0x01;
	bbuf[8] = 0x31;
	bbuf[9] = 0x42;
	wlen = write(fd, bbuf, 10);
	if (wlen != sizeof(bbuf)) {
	    printf("testport: Error from write: %d, %d\n", wlen, errno);
		return -1;
	}
	printf("testport: Successfully wrote %d bytes\n",wlen);
	int result = tcdrain(fd);    /* delay for output to complete */
	if( result!=0 ) {
	  printf("testport: Error executing tcdrain (%d)\n",errno);
	}

    /* simple noncanonical input */
    do {
        unsigned char buf[80];
        int rdlen;

		printf("testport: reading ...\n");
        rdlen = read(fd, buf, sizeof(buf) - 1);
        if (rdlen > 0) {
/* display hex */
            unsigned char   *p;
            printf("testport: Read %d bytes:", rdlen);
            for (p = buf; rdlen-- > 0; p++)
                printf(" 0x%x", *p);
            printf("\n");
        } else if (rdlen < 0) {
            printf("testport: Error from read: %d: %s\n", rdlen, strerror(errno));
        } else {  /* rdlen == 0 */
            printf("testport: Timeout from read\n");
        }
        /* repeat read to get full message */
    } while (1);
	close(fd);
	return 0;
}
