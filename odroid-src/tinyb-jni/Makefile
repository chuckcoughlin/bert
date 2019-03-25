#
#  Compile the shared JNI library needed by the TinyB java project.
#  Code from TinyB - Andrei Vasiliu (Intel).
#  https://github.com/intel-iot-devkit/tinyb
#  There are two libraries combined here, one the C++ native
#  implementation, the other the JNI java interface.
#
#  make -e tinyb
#  make install
#
CC=gcc
CXX=g++
CFLAGS  += -I$(INCDIR) \
           -I$(APIDIR) \
           -I$(APIDIR)/tinyb \
           -I/usr/include/glib-2.0 \
           -I/usr/include/gio-unix-2.0 \
		   -fPIC -Wno-psabi
APIDIR = api
INCDIR = include
LDFLAGS = -shared
LIB = /usr/lib/arm-linux-gnueabihf
LIBTINYB  = libtinyb.so
LIBS += -L$(LIB) -lbluetooth
JNICFLAGS += -I$(INCDIR) \
           -I$(APIDIR) \
           -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" \
		   -fPIC -Wno-psabi 
JNIOBJDIR = jniobj
JNIOBJS = $(patsubst jni/%.cxx,jniobj/%.o,$(wildcard jni/*.cxx))
OBJS    = $(patsubst src/%.cpp,obj/%.o,$(wildcard src/*.cpp))
COBJS    = $(patsubst src/%.c,cobj/%.o,$(wildcard src/*.c))
OBJDIR  = obj
COBJDIR  = cobj



#  * Compile and link the native implementation
tinyb: $(LIBTINYB)
	-cp $(LIBTINYB) $(LIB)

$(LIBTINYB): $(OBJS) $(COBJS) $(JNIOBJS)
	$(CXX) $(LDFLAGS) $(LIBS) -o $@ $(OBJS) $(COBJS) $(JNIOBJS)

# Compile C++ code
$(OBJDIR)/%.o: src/%.cpp| $(OBJDIR)
	$(CXX) -c $(CFLAGS) -o $@ $<

# Compile C code
$(COBJDIR)/%.o: src/%.c| $(COBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

# Compiles JNI code
$(JNIOBJDIR)/%.o: jni/%.cxx | $(JNIOBJDIR)
	$(CXX) -c $(JNICFLAGS) -o $@ $<

install:
	-cp $(LIBJNI) $(BERT_HOME)/lib

clean:
	-rm -f $(LIBTINYB) $(LIBJNI)
	-rm -rf $(OBJDIR) $(JNIOBJDIR) $(COBJDIR)


$(JNIOBJDIR):
	mkdir -p $(JNIOBJDIR)

$(OBJDIR):
	mkdir -p $(OBJDIR)

$(COBJDIR):
	mkdir -p $(COBJDIR)


.PHONY: jni tinyb clean install
