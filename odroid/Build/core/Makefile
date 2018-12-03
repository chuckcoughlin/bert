#
# Cross compile and copy the result to the robot.
# PROJECT_ROOT refers to the root of the source code
# BUILD_DIR is where the output is stored.
# We first build in the source area, then move the results.
#
OBJS = Core.o
PROG = Core

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g
else
	CFLAGS += -O2
endif

all: $(PROG)
	mv $(PROG) $(BUILD_DIR)/$(PROG)
	@rm $(OBJS)
	@echo "Build of Core is complete."

$(PROG): $(OBJS)
	$(CXX) -o $@ $^

%.o: $(PROJECT_ROOT)/%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

%.o: $(PROJECT_ROOT)/%.c
	$(CC) -c $(CFLAGS) $(CPPFLAGS) -o $@ $<

.PHONY: clean all install

clean:
	@ $(RM) -fr $(PROG) $(OBJS)
	
install:
	cd $(BUILD_DIR); rsync -a $(PROG) bert:/usr/local/robot
	@echo "Installation complete."