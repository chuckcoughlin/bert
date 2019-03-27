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
LIB = /usr/lib/arm-linux-gnueabihf
LIBBTJ  = libbluetoothjni.so
LIBS += -L$(LIB) -lbluetooth
OBJS    = $(patsubst src/%.c,obj/%.o,$(wildcard src/*.c))
OBJDIR  = obj


#  * Compile and link the native implementation
all: $(LIBBTJ) 

btj: $(OBJDIR)/btj.o
	$(CC) $(LIBS) -lbluetoothjni -o $@ $<

# Compile C code
$(OBJDIR)/%.o: src/%.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR)/btj.o: test/btj.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(LIBBTJ): $(OBJS)
	$(CC) $(LDFLAGS) $(LIBS) -o $@ $(OBJS)

install:
	-cp $(LIBBTJ) $(BERT_HOME)/lib

clean:
	-rm -f $(LIBBTJ) btj
	-rm -rf $(OBJDIR)


$(OBJDIR):
	mkdir -p $(OBJDIR)


.PHONY: all test clean install
