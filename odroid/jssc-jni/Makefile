#
# Compile the shared JNI library needed by the jssc project.
# We also include 'porttest', a command-line test application
# with a canned test.
#
#  make porttest
#
CC=gcc
CFLAGS  += -I$(INCDIR) \
           -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" \
		   -fPIC -Wno-psabi
INCDIR = include
LDFLAGS = -shared
LD_LIBRARY_PATH = /usr/lib/arm-linux-gnueabihf:/usr/local/robot/lib
LIBS = -L/usr/local/robot/lib
OBJS    = $(patsubst src/%.c,obj/%.o,$(wildcard src/*.c))
OBJDIR  = obj
TESTS   = testport


#  * Compile and link the native implementation
all: $(TESTS) 

# Compile C code
testport: $(OBJDIR)/testport.o
	$(CC) $(LIBS) -o $@ $< 

$(OBJDIR)/testport.o: test/testport.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<


clean:
	-rm -f $(TESTS)


$(OBJDIR):
	mkdir -p $(OBJDIR)


.PHONY: all clean
