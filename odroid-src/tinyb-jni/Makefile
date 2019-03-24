#
#  Compile the shared JNI library needed by the TinyB java project.
#  Code from TinyB - Andrei Vasiliu (Intel).
#  https://github.com/intel-iot-devkit/tinyb
#  There are two builds combined here, one for the C++ native
#  implementation, the other for the JNI java interface.
#
#  sudo chmod +w /usr/lib/arm-linux-gnuabihf
#
#  make tinyb
#  make -e java
#  make install
#
CC=gcc
CXX=g++
CFLAGS  += -I$(INCDIR) \
           -I$(APIDIR) \
           -I$(APIDIR)/tinyb \
           -I/usr/include/glib-2.0 \
		   -fPIC -Wno-psabi
APIDIR = api
INCDIR = include
LDFLAGS = -shared
LIB = /usr/lib/arm-linux-gnueabihf
LIBTINYB  = libtinyb.so
LIBJNI    = libtinybjni.so
LIBS += -L$(LIB) -lbluetooth
JNICFLAGS += -I$(INCDIR) \
           -I$(APIDIR) \
           -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" \
		   -fPIC -Wno-psabi 
JNILDFLAGS = -shared 
JNILIBS += -L$(LIB) -ltinyb -lbluetooth
JNIOBJDIR = jniobj
JNIOBJS = $(patsubst jni/%.cxx,jniobj/%.o,$(wildcard jni/*.cxx))
OBJS    = $(patsubst src/%.cpp,obj/%.o,$(wildcard src/*.cpp))
COBJS   = jniobj/generated-code.o
OBJDIR  = obj



#  * Compile and link the native implementation
tinyb: $(LIBTINYB)
	-cp $(LIBTINYB) $(LIB)

#  * Compile and link libtinybjava
jni: $(LIBJNI) 

$(LIBTINYB): $(OBJS) $(COBS)
	$(CXX) $(LDFLAGS) $(LIBS) -o $@ $(OBJS)

$(LIBJNI): $(JNIOBJS)
	$(CXX) $(JNILDFLAGS) $(JNILIBS) -o $@ $(JNIOBJS)

# Compile C++ code
$(OBJDIR)/%.o: src/%.cpp| $(OBJDIR)
	$(CXX) -c $(CFLAGS) -o $@ $<

# Compile C code
$(COBS): src/*.c| $(OBJDIR)
	$(CC) -c $(CFLAGS) -o $@ $<

# Compiles JNI code
$(JNIOBJDIR)/%.o: jni/%.cxx | $(JNIOBJDIR)
	$(CC) -c $(JNICFLAGS) -o $@ $<

install:
	-cp $(LIBJNI) $(BERT_HOME)/lib

clean:
	-rm -f $(LIBTINYB) $(LIBJNI)
	-rm -rf $(OBJDIR) $(JNIOBJDIR)


$(JNIOBJDIR):
	mkdir -p $(JNIOBJDIR)

$(OBJDIR):
	mkdir -p $(OBJDIR)


.PHONY: jni tinyb clean install
