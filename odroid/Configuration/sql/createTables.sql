-- Copyright 2019. Charles Coughlin. All rights reserved.
-- These tables are used by the robot to access poses, state
-- arrays and action lists.
--

-- The Action table shows motor positions over time for named "actions".
-- Each row in the table represents an actioun at an instant of time.
-- The time increment between rows is unspecified, by default 1 second.
DROP TABLE IF EXISTS Action;
CREATE TABLE Action (
	name	text NOT NULL,
	cycle   int NOT NULL,
	ABS_X	int NULL,
	ABS_Y	int NULL,
	ABS_Z	int NULL,
	BUST_X	int NULL,
	BUST_Y	int NULL,
	HEAD_Y	int NULL,
	HEAD_Z	int NULL,
	LEFT_ANKLE_Y	int NULL,
	LEFT_ARM_Z	int NULL,
	LEFT_ELBOW_Y	text NULL,
	LEFT_HIP_X	int NULL,
	LEFT_HIP_Y	int NULL,
	LEFT_HIP_Z	int NULL,
	LEFT_KNEE_Y	int NULL,
	LEFT_SHOULDER_X	int NULL,
	LEFT_SHOULDER_Y	int NULL,
	RIGHT_ANKLE_Y	int NULL,
	RIGHT_ARM_Z	int NULL,
	RIGHT_ELBOW_Y	int NULL,
	RIGHT_HIP_X	int NULL,
	RIGHT_HIP_Y	int NULL,
	RIGHT_HIP_Z	int NULL,
	RIGHT_KNEE_Y	int NULL,
	RIGHT_SHOULDER_X	int NULL,
	RIGHT_SHOULDER_Y	int NULL,
	UNIQUE (name,cycle)
);


-- The Pose table shows motor positions for named "poses".
-- Each pose is represented on a single line
DROP TABLE IF EXISTS Pose;
CREATE TABLE Pose (
	name	text PRIMARY KEY,
	ABS_X	int NULL,
	ABS_Y	int NULL,
	ABS_Z	int NULL,
	BUST_X	int NULL,
	BUST_Y	int NULL,
	HEAD_Y	int NULL,
	HEAD_Z	int NULL,
	LEFT_ANKLE_Y	int NULL,
	LEFT_ARM_Z	int NULL,
	LEFT_ELBOW_Y	text NULL,
	LEFT_HIP_X	int NULL,
	LEFT_HIP_Y	int NULL,
	LEFT_HIP_Z	int NULL,
	LEFT_KNEE_Y	int NULL,
	LEFT_SHOULDER_X	int NULL,
	LEFT_SHOULDER_Y	int NULL,
	RIGHT_ANKLE_Y	int NULL,
	RIGHT_ARM_Z	int NULL,
	RIGHT_ELBOW_Y	int NULL,
	RIGHT_HIP_X	int NULL,
	RIGHT_HIP_Y	int NULL,
	RIGHT_HIP_Z	int NULL,
	RIGHT_KNEE_Y	int NULL,
	RIGHT_SHOULDER_X	int NULL,
	RIGHT_SHOULDER_Y	int NULL
);

-- The MotorState table holds configurable parameters of each motor.
-- The intent is hold presets rather than current state.
DROP TABLE IF EXISTS MotorState;
CREATE TABLE MotorState (
	id      int PRIMARY KEY,
	name	text NOT NULL,
	controller text NOT NULL,
	type    text NOT NULL,
	offset  int NULL,
	speed   float NULL,
	torque  float NULL,
	minAngle float NULL,
	maxAngle float NULL,
	direct int DEFAULT 1,
	UNIQUE (name,id,controller)
);