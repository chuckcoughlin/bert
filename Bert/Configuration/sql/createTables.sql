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
	executeOrder integer not null,
	delay   integer NOT NULL,
	poseId	integer NOT NULL,
	UNIQUE (name,executeOrder)
);


-- The Pose table shows motor positions for named "poses".
-- Each pose is represented on a single line
-- If not otherwise specified, SQLite will create an id on insert
DROP TABLE IF EXISTS Pose;
CREATE TABLE Pose (
	id	integer PRIMARY KEY,
	name		text NOT NULL,
	parameter   text NOT NULL,
	ABS_X	integer NULL,
	ABS_Y	integer NULL,
	ABS_Z	integer NULL,
	BUST_X	integer NULL,
	BUST_Y	integer NULL,
	NECK_Y	integer NULL,
	NECK_Z	integer NULL,
	LEFT_ANKLE_Y	integer NULL,
	LEFT_ARM_Z	integer NULL,
	LEFT_ELBOW_Y	text NULL,
	LEFT_HIP_X	integer NULL,
	LEFT_HIP_Y	integer NULL,
	LEFT_HIP_Z	integer NULL,
	LEFT_KNEE_Y	integer NULL,
	LEFT_SHOULDER_X	integer NULL,
	LEFT_SHOULDER_Y	integer NULL,
	RIGHT_ANKLE_Y	integer NULL,
	RIGHT_ARM_Z	integer NULL,
	RIGHT_ELBOW_Y	integer NULL,
	RIGHT_HIP_X	integer NULL,
	RIGHT_HIP_Y	integer NULL,
	RIGHT_HIP_Z	integer NULL,
	RIGHT_KNEE_Y	integer NULL,
	RIGHT_SHOULDER_X	integer NULL,
	RIGHT_SHOULDER_Y	integer NULL,
	UNIQUE (name,parameter)
);
-- The PoseMap table maps commands to poses.
DROP TABLE IF EXISTS PoseMap;
CREATE TABLE PoseMap (
	command	text PRIMARY_KEY,
	pose text NOT NULL
);

-- The MotorState table holds configurable parameters of each motor.
-- The intent is hold presets rather than current state.
DROP TABLE IF EXISTS MotorState;
CREATE TABLE MotorState (
	id      integer PRIMARY KEY,
	name	text NOT NULL,
	controller text NOT NULL,
	type    text NOT NULL,
	offset  integer NULL,
	speed   float NULL,
	torque  float NULL,
	minAngle float NULL,
	maxAngle float NULL,
	direct integer DEFAULT 1,
	UNIQUE (name,id,controller)
);
