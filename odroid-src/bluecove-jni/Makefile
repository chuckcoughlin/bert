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
LIBBLUECOVE  = libbluecove.so
OBJS    = $(patsubst src/%.c,obj/%.o,$(wildcard src/*.c))
OBJDIR  = obj
TESTS   = btj simplescan l2capserver l2capclient rfcommserver rfcommclient dbus


#  * Compile and link the native implementation
all: $(LIBBTJ) 

# Compile C code
$(OBJDIR)/%.o: src/%.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(LIBBLUECOVE): $(OBJS)
	$(CC) $(LDFLAGS) $(LIBS) -o $@ $(OBJS) -lbluetooth

tests: $(TESTS)

btj: $(OBJDIR)/btj.o
	$(CC) $(LIBS) -o $@ $< -lbluecove -lbluetooth

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

$(OBJDIR)/dbus.o: test/dbus.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/btj.o: test/btj.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/simplescan.o: test/simplescan.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/l2capclient.o: test/l2capclient.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/l2capserver.o: test/l2capserver.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/rfcommserver.o: test/rfcommserver.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/rfcommclient.o: test/rfcommclient.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<


install:
	-cp $(LIBBLUECOVE) $(BERT_HOME)/lib

clean:
	-rm -f $(LIBBLUECOVE) $(TESTS)
	-rm -rf $(OBJDIR)


$(OBJDIR):
	mkdir -p $(OBJDIR)


.PHONY: all test clean install
