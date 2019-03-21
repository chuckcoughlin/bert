#
#  Compile the shared JNI library needed by the TinyB java project.
#  Code from TinyB - Andrei Vasiliu (Intel).
#
LIB = /usr/lib/arm-linux-gnuabihf
BLUETOOTH_LIB = $(LIB)/libbluetooth.so
BLUETOOTH_INC_DIR = /usr/include/bluetooth
CC=g++
INCDIR = include
INCS = $(wildcard include/tinyb/*.hpp)
OBJDIR = build
OBJS = $(patsubst src/%.cxx,build/%.o,$(wildcard src/*.cxx))
TARGET = libtinyb.so

CFLAGS  += -I$(INCDIR) -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" -fPIC -Wno-psabi
LIBS += -L$(LIB) -lbluetooth
LDFLAGS = -shared


# By default:
#  * Compile the native implementation
#  * Compile libtinyb
all: $(TARGET) install

$(TARGET): $(OBJS) 
	$(CC) $(LDFLAGS) $(LIBS) -o $@ $(OBJS)

# Compile C++ code
$(OBJDIR)/%.o: src/%.cxx $(INCS) | $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR):
	mkdir -p $(OBJDIR)

clean:
	-rm -f $(TARGET)
	-rm -rf $(OBJDIR)

install:
	-cp $(TARGET) $(BERT_HOME)/lib

.PHONY: all clean install
