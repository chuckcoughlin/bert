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
TESTS   = BlueCoveBlueZ_Tests


#  * Compile and link the native implementation
all: $(LIBBLUECOVE) 

# Compile C code
$(OBJDIR)/%.o: src/%.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(LIBBLUECOVE): $(OBJS)
	$(CC) $(LDFLAGS) $(LIBS) -o $@ $(OBJS) -lbluetooth

tests: $(TESTS)

BlueCoveBlueZ_Tests: $(OBJDIR)/BlueCoveBlueZ_Tests.o
	$(CC) $(LIBS) -o $@ $< -lbluecove -lbluetooth

$(OBJDIR)/BlueCoveBlueZ_Tests.o: tests/BlueCoveBlueZ_Tests.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

install:
	-cp $(LIBBLUECOVE) $(BERT_HOME)/lib

clean:
	-rm -f $(LIBBLUECOVE) $(TESTS)
	-rm -rf $(OBJDIR)


$(OBJDIR):
	mkdir -p $(OBJDIR)


.PHONY: all tests clean install
