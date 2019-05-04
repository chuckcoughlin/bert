#
# Compile the shared JNI library needed by the btj project.
# We also include 'btj', a command-line application used
# for testing. "btj -h" for a list of options.
#
#  make -e
#  make install
#  make btj
#
CC=gcc
CFLAGS  += -I$(INCDIR) \
           -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" \
		   -fPIC -Wno-psabi
INCDIR = include
LDFLAGS = -shared
LD_LIBRARY_PATH = /usr/lib/arm-linux-gnueabihf:/usr/local/robot/lib
LIBS = -L/usr/local/robot/lib
LIBLLUEZ  = libbluezjni.so
OBJS    = $(patsubst src/%.c,obj/%.o,$(wildcard src/*.c))
OBJDIR  = obj
TESTS   = bluez simplescan l2capserver l2capclient rfcommserver rfcommclient gatt


#  * Compile and link the native implementation
all: $(LIBLLUEZ) 

# Compile C code
$(OBJDIR)/%.o: src/%.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(LIBLLUEZ): $(OBJS)
	$(CC) $(LDFLAGS) $(LIBS) -o $@ $(OBJS) -lbluetooth

tests: $(TESTS)

bluez: $(OBJDIR)/bluez.o
	$(CC) $(LIBS) -o $@ $< -lbluezjni -lbluetooth

l2capclient: $(OBJDIR)/l2capclient.o
	$(CC) $(LIBS) -o $@ $< -lbluetooth

l2capserver: $(OBJDIR)/l2capserver.o
	$(CC) $(LIBS) -o $@ $< -lbluetooth

simplescan: $(OBJDIR)/simplescan.o
	$(CC) $(LIBS) -o $@ $< -lbluetooth

rfcommclient: $(OBJDIR)/rfcommclient.o
	$(CC) $(LIBS) -o $@ $< -lbluetooth

rfcommserver: $(OBJDIR)/rfcommserver.o
	$(CC) $(LIBS) -o $@ $< -lbluetooth

$(OBJDIR)/bluez.o: tests/bluez.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/simplescan.o: tests/simplescan.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/l2capclient.o: tests/l2capclient.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/l2capserver.o: tests/l2capserver.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/rfcommserver.o: tests/rfcommserver.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/rfcommclient.o: tests/rfcommclient.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<


install:
	-cp $(LIBLLUEZ) $(BERT_HOME)/lib

clean:
	-rm -f $(LIBLLUEZ) $(TESTS)
	-rm -rf $(OBJDIR)


$(OBJDIR):
	mkdir -p $(OBJDIR)


.PHONY: all tests clean install
