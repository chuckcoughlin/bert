#
# Compile the shared JNI library needed by the bluez project.
# We also include 'bluez', a command-line application used
# for testing. It is an interactive send-receive tool.
#
#  make -e
#  make install
#  make btj
#
CC=gcc
CFLAGS  += -Iinclude \
		   -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" \
		   -fPIC -Wno-psabi
LDFLAGS = -shared
LD_LIBRARY_PATH = /usr/lib/arm-linux-gnueabihf:/usr/local/robot/lib
OBJS    = $(patsubst src/%.c,obj/%.o,$(wildcard src/*.c))
OBJDIR  = obj
DAEMON  = blueserverd
TESTS   = blueserver rfcommserver


#  * Compile and link the native implementation
all: $(DAEMON) $(TESTS)

# Compile C code
$(OBJDIR)/%.o: src/%.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(DAEMON): $(OBJS)
	$(CC) -o $@ $(OBJS) -lbluetooth

blueserver: $(OBJDIR)/blueserver.o
	$(CC) -o $@ $< -lbluetooth

rfcommserver: $(OBJDIR)/rfcommserver.o
	$(CC) -o $@ $< -lbluetooth

$(OBJDIR)/blueserver.o: tests/blueserver.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/rfcommserver.o: tests/rfcommserver.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<


install:
	-cp $(DAEMON) $(BERT_HOME)/bin

clean:
	-rm -f $(DAEMON) $(TESTS) $(BERT_HOME)/bin/$(DAEMON)
	-rm -rf $(OBJDIR)


$(OBJDIR):
	mkdir -p $(OBJDIR)


.PHONY: all clean install
