#
#  Compile the shared JNI library needed by the bluez java project.
#  Code based on JBluez-1.0 Edward Kay <ed.kay@appliancestudio.com>
#  There are two libraries combined here, one the C++ native
#  implementation, the other the JNI java interface.
#
#  make -e
#  make install
#
CC=gcc
CFLAGS  += -I$(INCDIR) \
           -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" \
		   -fPIC -Wno-psabi
INCDIR = include
LDFLAGS = -shared
LIB = /usr/lib/arm-linux-gnueabihf
LIBBLUEZ  = libbluez.so
LIBS += -L$(LIB) -lbluetooth
OBJS    = $(patsubst src/%.c,obj/%.o,$(wildcard src/*.c))
OBJDIR  = obj


#  * Compile and link the native implementation
all: $(LIBBLUEZ)

# Compile C code
$(OBJDIR)/%.o: src/%.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(LIBBLUEZ): $(OBJS)
    $(CC) $(LDFLAGS) $(LIBS) -o $@ $(OBJS)

install:
	-cp $(LIBBLUEZ) $(BERT_HOME)/lib

clean:
	-rm -f $(LIBBLUEZ)
	-rm -rf $(OBJDIR)


$(OBJDIR):
	mkdir -p $(OBJDIR)


.PHONY: all clean install
