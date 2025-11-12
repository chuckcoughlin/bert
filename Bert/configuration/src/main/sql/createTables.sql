-- Copyright 2019-2024. Charles Coughlin. All rights reserved.
-- These tables are used by the robot to access actions and poses, state
-- arrays and action lists.
--

-- Apply a series of Poses in order. The time increment between poses
-- is specified in the pose table. NextAction allows configuration
-- of a chain of actions or a repetition.
DROP TABLE IF EXISTS Action;
CREATE TABLE Action (
	name	text PRIMARY_KEY,
	poseseries  text NOT NULL,
	nextaction text NULL
);

-- The Face table merely maps names associated with faces to
-- an 'id' which is used to obtain identity specifics,
DROP TABLE IF EXISTS Face;
CREATE TABLE Face (
	name	text PRIMARY_KEY,
	faceid	integer NOT NULL
);
-- The landmark table holds normalized positions of
-- landmarks identified with a face
DROP TABLE IF EXISTS FaceContour;
CREATE TABLE FaceContour (
	faceid		  integer NOT NULL,
	contourcode   text    NOT NULL,
	indx          integer NOT NULL,
	x             float,
    y             float,
    UNIQUE (faceid,contourcode,indx)
);
-- The landmark table holds normalized positions of
-- landmarks identified with a face
DROP TABLE IF EXISTS FaceLandmark;
CREATE TABLE FaceLandmark (
	faceid	      integer NOT NULL,
	landmarkcode  text    NOT NULL,
	x             float,
	y             float,
	UNIQUE (faceid,landmarkcode)
);

-- Each Pose has a series name which can be incorporated
-- into an action.
DROP TABLE IF EXISTS Pose;
CREATE TABLE Pose (
	poseid	integer PRIMARY_KEY,
	series       text NOT NULL,
	executeOrder integer NOT NULL,
	delay        integer DEFAULT 1000
);

-- A pose is a collection of joint-positions
DROP TABLE IF EXISTS PoseJoint;
CREATE TABLE PoseJoint (
	poseid	integer NOT_NULL,
	joint		text NOT NULL,
	angle       real NOT NULL,
	torque		real DEFAULT 50.,
	speed		real DEFAULT 20.,
	UNIQUE (poseid,joint)
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
