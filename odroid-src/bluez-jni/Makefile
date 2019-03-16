#
#  Compile the shared JNI library needed by the BlueZLib java project.
#
BLUETOOTH_LIB = /usr/lib/arm-linux-gnuabihf/libbluetooth.so
BLUETOOTH_INC_DIR = /usr/include/bluetooth
OBJDIR = build
OBJS = $(patsubst src/%.c,build/%.o,$(wildcard src/*.c))
TARGET = libbluezjni.so

# gcc -o simplescan simplescan.c -lbluetooth

MAKEFLAGS += --no-builtin-rules
.suffixes:
.PHONY: clean

CFLAGS  += -Iinclude -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" \
           -fPIC
LDFLAGS += -shared  -lbluetooth


# By default:
#  * Compile the native implementation
#  * Compile libbluezjni
all: $(TARGET)

# The header file is needed for the succesful compilation of the native code.
build/BluetoothManager.o: include/bluezlib_BluetoothManager.h

# Compile C code
$(OBJDIR)/%.o: src/%.c
	mkdir -p build
	$(CC) -c $(CFLAGS) -o $@ $<

$(TARGET): $(OBJS) | $(OBJDIR)
	$(CC) $(LDFLAGS) -o $@ $(OBJS)


$(OBJDIR):
	mkdir -p $(OBJDIR)

clean:
	-rm -f $(TARGET)
	-rm -rf $(OBJDIR)

