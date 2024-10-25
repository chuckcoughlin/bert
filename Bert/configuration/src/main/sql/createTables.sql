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

-- The Face table merely maps known faces to
-- an 'id' which is used to obtain identity specifics,
-- If not otherwise specified, SQLite will create an id on insert
DROP TABLE IF EXISTS Face;
CREATE TABLE Face (
	name	text PRIMARY_KEY,
	faceid	integer NOT NULL,
);
-- The landmark table holds normalized positions of
-- landmarks identified with a face
DROP TABLE IF EXISTS FaceContour;
CREATE TABLE FaceContour (
	faceidid	integer PRIMARY KEY,
	contourid   integer NOT NULL
);
DROP TABLE IF EXISTS FaceContourPoints;
CREATE TABLE FaceContourPoints (
	contourid	  integer PRIMARY KEY,
	contourcode   text    NOT NULL,
	indx          integer NOT NULL,
	x             float,
    y             float
);
-- The landmark table holds normalized positions of
-- landmarks identified with a face
DROP TABLE IF EXISTS FaceLandmark;
CREATE TABLE FaceLandmark (
	faceidid	  integer PRIMARY KEY,
	landmarkcode  text    NOT NULL,
	x             float,
	y             float
);
-- The Pose table shows motor positions for named "poses".
-- The joint position for each pose is represented on a single row

DROP TABLE IF EXISTS Pose;
CREATE TABLE Pose (
	poseid	integer NOT_NULL,
	joint		text NOT NULL,
	position    integer NOT NULL,
	UNIQUE (poseid,joint)
);
-- The PoseNames table maps commands to poses.
-- If not otherwise specified, SQLite will create an id on insert
DROP TABLE IF EXISTS PoseName;
CREATE TABLE PoseName (
	pose text PRIMARY_KEY,
	poseid integer NOT NULL,
	UNIQUE (poseid,pose)
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

-- Clean up obsolete tables
DROP TABLE IF EXISTS PoseMap;
