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
	Greeting Salutation?                                        # handleGreeting
	| Salutation? Hold (It | Article? Pose)         			# holdIt
	| Salutation? Initialize Article? Motors   					# initializeJoints
    | Salutation? List Article? Configuration                   # configurationRequest
	| Salutation? List Article? (Limits|Goals) Of Article? Side? Joint Axis?    # handleBulkPropertyRequest
	| Salutation? List Article? Properties Of Article? Motors                   # handleListCommand1
	| Salutation? List Article? Motor? Properties                               # handleListCommand2
	| Salutation? Move (It | Article? Side? Joint Axis?) To? Value Unit?        # moveMotor
	| Salutation? Move Adverb                                                   # moveSpeed
	| Salutation? (Freeze|Relax) Article? Side? (It|Joint|Limb)? Axis?			# setTorque
    | Salutation? Set Article? Side? Joint Axis? Property? To? Value Unit?		# setMotorPosition
	| Salutation? Set Article? Property Of Article? Side? Joint Axis? To Value Unit?  # setMotorProperty
    | Salutation? Straighten Yourself? Up   					# straightenUp
	| Salutation? Straighten (It|Article? Side? Joint Axis? )   # straightenJoint
	| Salutation? (Command|Halt|Reset|Shutdown)                 # handleSingleWordCommand
    | Salutation? (Move|Take|Set) To Article NAME+ Property		# moveToPose
	| Salutation? (Move|Take|Set) NAME+                         # handleCompoundCommand
	| Salutation? NAME+                                         # handleArbitraryCommand
	;

// Request for information
question:
        Salutation?  How Attribute Are You 							# attributeQuestion
    | What Is Article? Configuration								# configurationQuestion
    | What Are Article? (Limits|Goals) Of Article? Side? Joint Axis? # handleBulkPropertyQuestion
    | What Is Article? Side? Joint Axis? Property               # jointPropertyQuestion
    | What Is Article? Axis? Property Of Article? Side? Joint   # motorPropertyQuestion1
    | What Is Article? Property Of Article? Side? Joint Axis?   # motorPropertyQuestion2
    | What Is Article? Metric   				    # metricsQuestion
    | What Is Article? Adjective? Pose                          # poseQuestion
    | Where Is Article? Side? (Appendage|Joint)	Axis?       	# limbLocationQuestion
    | Why Do You Have Mittens									# whyMittens
	;

// Convey information to the robot.
declaration:
    You Are NAME						# declarePose1
	| Article Pose Is NAME 				# declarePose2
	| When Isay NAME Take Article? Pose NAME		# mapPoseToCommand1
	| NAME Means To NAME							# mapPoseToCommand2
	;


// First is a list of words that appear in lists
Why: 'why';
// Pardon the license taken with some of these categories ...
Article: 'a'|'an'|'the'|'this'|'that'|'your';
Adjective: 'current';
Adverb: 'in slow motion'|'very fast'|'fast'|'normally'|'very quickly'|'quickly'|'very slowly'|'slowly'|'very slow'|'slow';
Appendage: 'ear'|'eye'|'eyes'|'finger'|'foot'|'hand'|'heel'|'nose'|'toe';
Are: 'are';
Attribute: 'old'|'tall';
Axis: 'x'|'y'|'z'|'horizontal'|'vertical'|Why;
Command: ('go to sleep'|'ignore me'|'pay attention'|'sleep'|'wake up');
Configuration: 'configuration';
Do: 'do';
Goals: 'goals'|'targets';
Halt: 'die'|'exit'|'halt'|'quit'|'stop';
Freeze: 'freeze'|'stiffen'|'tighten';
Greeting: 'hello'|'high'|'hi';
Have: 'have'|'wear';
Hold: 'hold';
How: 'how';
Initialize: 'initialize';
Isay: 'i say';
Is: 'is';
It: 'it';
List: ('tell me'|'describe'|'list');
Limb: 'arm'|'back'|'leg'|'torso';
Limits: 'limits';
Means: 'means';
Metric: 'age'|'cadence'|'cycle time'|'duty cycle'|'height'|'name';
Mittens: 'mittens';
Motors: 'devices'|'joints'|'motors';
Motor: 'device'|'joint'|'motor';
Move: 'bend'|'go'|'move'|'turn';
Of: 'of'|'on'|'for';
Joint: 'ankle'|'arm'|'elbow'|'head'|'hip'|'thigh'|'knee'|'neck'|'shoulder'|'chest'|'bust'|'abdomen'|'abs';
Pose: 'pose';
Properties: 'ids'|'positions'|'offsets'|'minimum angles'|'maximum angles'|'angles'|'motor types'|'orientations'|'speeds'|'torques'|'loads'|'temperatures'|'voltages'|'velocities';
Property: 'id'|'position'|'offset'|'min angle'|'max angle'|'minimum angle'|'maximum angle'|'angle'|'motor type'|'orientation'|'speed'|'torque'|'load'|'temperature'|'voltage'|'velocity';
Relax:'loosen'|'relax';
Reset: 'reset';
Salutation:'bert'|'burt'|'now'|'please';
Shutdown: 'power off'|'shut down'|'shutdown';
Set: 'set';
Side: 'left'|'right'|'other';
Straighten: 'straighten';
Take: 'assume' | 'take';
To: 'to become'|'to';
Unit: 'degrees';
Up: 'up';
Value: NUMBER;
You: 'you';
Yourself: 'yourself';

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