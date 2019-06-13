/*
 * Copyright 2018-2019 Chuck coughlin. All Rights Reserved.
 * Define a formal grammar for speech recognized by "Bert".
 * All incoming text has been converted to lower case to allow for
 * case-insensitive syntax.
 */
grammar SpeechSyntax;

/** Initial rule, begin parsing */
line: statement EOF;

statement:
      command
	| question
	| declaration
	;

// Imperatives directing the robot to take an action
command:
      Salutation? List Article? Configuration                    # configurationRequest
            | Salutation? List Article? (Limits|Goals) Of Article? Side? Joint Axis?    # handleBulkPropertyRequest
	| Salutation? List Article? Properties Of Article? Motors                   # handleListCommand1
	| Salutation? List Article? Motor? Properties                               # handleListCommand2
	| Salutation? Move (It | Article? Side? Joint Axis?) To? Value Unit?        # moveMotor
	| Salutation? Move Adverb                                                   # moveSpeed
            | Salutation? Set Article? Side? Joint Axis? Property? To? Value Unit?  # setMotorPosition
	| Salutation? Set Article? Property Of Article? Side? Joint Axis? To Value Unit?  # setMotorProperty
	| Salutation? Straighten (It|Article? Side? Joint Axis? )   # straightenJoint
        | Salutation? (Command|Halt|Shutdown)                       # handleSingleWordCommand
        | Salutation? (Move|Take|Set) NAME+                         # handleCompoundCommand
        | Salutation? NAME+                                         # handleArbitraryCommand
	;

// Request for information
question:
      How Attribute Is 'you'                                     # attributeQuestion
	| What Is Article? Configuration                          # configurationQuestion
	| What 'are' Article? (Limits|Goals) Of Article? Side? Joint Axis? # handleBulkPropertyQuestion
	| What Is Article? Property Of Article? Side? Joint Axis? # jointPropertyQuestion1
    | What 'is' Article? Side? Joint Axis? Property             # jointPropertyQuestion2
	| What 'is' Article? Metric   				    # metricsQuestion
    | What 'is' Article? Adjective?  Pose                       # poseQuestion
	| What 'is' Article? Axis? Property Of Article? Side? Joint # motorPropertyQuestion
	| Where Is Article? Side? Limb								# limbLocationQuestion 
	;

// Convey information to the robot.
declaration:
	'you' Is NAME					# declarePose1
	| Article Pose Is NAME                                # declarePose2
	| When 'i' 'say' NAME Take Article? Pose NAME		# mapPoseToCommand1
	| NAME Means To NAME					# mapPoseToCommand2
	;


// Pardon the license taken with some of these categories ...
Article: 'a'|'an'|'the'|'this'|'that'|'your';
Adjective: 'current';
Adverb: 'in slow motion'|'very fast'|'fast'|'normally'|'very quickly'|'quickly'|'very slowly'|'slowly'|'very slow'|'slow';
Attribute: 'old'|'tall';
Axis: 'x'|'y'|'z'|'horizontal'|'vertical';
Command: ('go to sleep'|'ignore me'|'pay attention'|'sleep'|'wake up');
Configuration: 'configuration';
Goals: 'goals'|'targets';
Halt: 'die'|'exit'|'halt'|'quit'|'stop';
How: 'how';
Is: 'is'|'are';
It: 'it';
List: ('tell me'|'describe'|'list');
Limb: 'foot'|'hand'|'lumbar'|'pelvis';
Limits: 'limits';
Means: 'means';
Metric: 'age'|'cadence'|'cycle time'|'duty cycle'|'height'|'name';
Motors: 'devices'|'joints'|'motors';
Motor: 'device'|'joint'|'motor';
Move: 'bend'|'go'|'move'|'turn';
Of: 'of'|'on'|'for';
Joint: 'ankle'|'arm'|'elbow'|'head'|'hip'|'thigh'|'knee'|'neck'|'shoulder'|'chest'|'bust'|'abdomen'|'abs';
Pose: 'pose';
Properties: 'ids'|'positions'|'offsets'|'minimum angles'|'maximum angles'|'angles'|'motor types'|'orientations'|'speeds'|'torques'|'loads'|'temperatures'|'voltages'|'velocities';
Property: 'id'|'position'|'offset'|'min angle'|'max angle'|'minimum angle'|'maximum angle'|'angle'|'motor type'|'orientation'|'speed'|'torque'|'load'|'temperature'|'voltage'|'velocity';
Salutation:'bert'|'burt';
Shutdown: 'power off'|'shut down'|'shutdown';
Set: 'set';
Side: 'left'|'right';
Straighten: 'straighten';
Take: 'assume' | 'take';
To: 'to become'|'to';
Unit: 'degrees';
Value: NUMBER;

What: 'what';
When: 'when';
Where: 'where';


COMMA: ',';
COLON: ':';
NAME:  (ALPHA+);
NUMBER: (DASH?DIGIT*PERIOD?DIGIT+);



// Fragments are never evaluated,
// nor can they appear in an evaluated expression
fragment ALPHA:  [a-zA-Z];
fragment DIGIT: [0-9];
fragment DASH: '-';
EQUAL: '=';
fragment PERIOD: '.';
SLASH: '/';
fragment UNDERSCORE: '_';
PCLOSE: ')';
POPEN:  '(';
DBLQUOTE:  '"';
SNGLQUOTE: '\'';