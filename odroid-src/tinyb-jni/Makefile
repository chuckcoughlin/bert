#
#  Compile the shared JNI library needed by the BlueZLib java project.
#  Code from TinyB - Andrei Vasiliu (Intel).
#
BLUETOOTH_LIB = /usr/lib/arm-linux-gnuabihf/libbluetooth.so
BLUETOOTH_INC_DIR = /usr/include/bluetooth
INCDIR = include
INCS = $(wildcard include/tinyb/*.hpp)
OBJDIR = build
OBJS = $(patsubst src/%.cxx,build/%.o,$(wildcard src/*.cxx))
TARGET = libbluezjni.so

CFLAGS  += -I$(INCDIR) -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" -fPIC -Wno-psabi
LDFLAGS += -shared  -lbluetooth


# By default:
#  * Compile the native implementation
#  * Compile libbluezjni
all: $(TARGET)

$(TARGET): $(OBJS) 
	$(CC) $(LDFLAGS) -o $@ $(OBJS)

# Compile C++ code
$(OBJDIR)/%.o: src/%.cxx $(INCS) | $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

$(OBJDIR):
	mkdir -p $(OBJDIR)

.PHONY: clean
clean:
	-rm -f $(TARGET)
	-rm -rf $(OBJDIR)

.PHONY: install
install:
	-cp $(TARGET) $(BERT_HOME)/lib

