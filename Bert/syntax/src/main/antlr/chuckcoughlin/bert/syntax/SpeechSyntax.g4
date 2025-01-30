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
	Salutation                                                              # handleSalutation
	| Describe Pose phrase Value          	    							# poseDescription
	| Describe Article? Adjective? Pose                                     # currentPoseDescription
	| Greeting Greeting? Salutation?                                        # handleGreeting
	| Salutation? Initialize Article? Motors   					            # initializeJoints
	| List Article? (Limits|Goals) Of Article? Side? Joint Axis?    		# handleBulkPropertyRequest
	| List Article? Properties Of Article? Motors       	# handleListCommand
	| List Article? Motor? Properties                   	# handleListCommand
	| List Article? (Dynamic|Static) Motor? Parameters		# parameterListQuestion
	| List Article? Location Article? Side? Limb		    # limbLocationList
    | List Article? (Dynamic|Static) Parameters Of Article? Motors # parameterListQuestion
    | List Article? Article? (Motors|Limbs|Extremities)     # bodyPartListQuestion
    | List Article? Article? (Actions|Faces|Poses)          # databaseListQuestion
    | Straighten (It|Article? Side? (Joint|Limb) Axis? )    # straightenJoint
	| Salutation? Move (It | Article? Side? Joint Axis?) To? Value Unit?       # moveMotor
	| Salutation? Move Speed                                                   # setSpeed
	| Salutation? (Hold|Freeze|Relax) Article? Side? (It|Joint|Limb)? Axis?	   # enableTorque
    | Salutation? Set Article? Side? Joint? Axis? Property To (Value|On|Off|Speed) Unit?		     # setMotorPrpoerty
	| Salutation? Set Article? Property Of Article? Side? Joint Axis? To (Value|On|Off|Speed) Unit?  # setMotorProperty
	| Salutation? phrase                                    		            # handleArbitraryCommandOrResponse
	;

// Request for information
question:
      Salutation?  How Attribute Are You 						# attributeQuestion
    | Salutation?  Are You There					            # personalQuestion
    | (What Are|Tell Me) Article? (Dynamic|Static) Motor? Parameters		# parameterNamesQuestion
    | (What Are|Tell Me) Article? (Dynamic|Static) Parameters Of Article? Motors # parameterNamesQuestion
    | (What Are|Tell Me) Article? Names Of Article? (Motors|Limbs|Extremities)   # bodyPartNamesQuestion
    | (What Are|Tell Me) Article? Names Of Article? (Actions|Faces|Poses)       # databaseNamesQuestion
    | (What Are|Tell Me) Article? (Limits|Goals) Of Article? Side? Joint Axis?  # handleBulkPropertyQuestion
    | (What Are|Tell Me) Article? Side? Joint Axis? Property              # jointPropertyQuestion
    | (What Is|Tell Me) Article? Axis? Property Of Article? Side? Joint   # jointPropertyQuestion
    | (What Is|Tell Me) Article? Property Of Article? Side? Joint Axis?   # jointPropertyQuestion
    | (What Is|Tell Me) Article? Metric   				                  # metricsQuestion
    | Where Is Article? Side? (Extremity|Joint)	Axis?       # jointLocationQuestion
    | What Actions Do You Know								# databaseActionNamesQuestion
    | What Poses Do You Know								# databasePoseNamesQuestion
    | Who Do You Know										# databaseFaceNamesQuestion
    | Why Do You Have Mittens								# whyMittens
	;

// Convey information to the robot.
declaration:
	  Forget phrase                                     # deleteUserData
	| My Metric Is phrase                               # setUserName
	| Iam phrase                                        # setUserName
	| Take Article? Pose phrase Value          	        # assumePose
    | Article Pose Is phrase Value						# definePose
	| Save Article? Pose As? phrase Value          	    # definePose
	| Define phrase As Article Series Of phrase Poses   # defineAction1
	| Define phrase (From|As) phrase                    # defineAction1
	| Use phrase Poses? To Define phrase				# defineAction2
	;

// Arbitrary string of words - inclue some key words. Digits are allowed only as a suffix
phrase: (NAME|Extremity|Are|As|Article|Axis|Freeze|Hold|It|Joint|Move|Of|Relax|Reset|Set|Side|Straighten|Take|To)+    # wordList
    ;

// First is a list of terms that are used below or use words that appear elsewhere
Are: 'are';
Freeze: 'freeze'|'stiffen'|'tighten up'|'go rigid';
Is: 'is';
Relax:'loosen up'|'relax'|'go limp';
Why: 'why';

// Pardon the license taken with some of these categories ...
Actions: 'actions';
Article: 'a'|'an'|'the'|'this'|'that'|'your';
Adjective: 'current';
As: 'as';
Attribute: 'old'|'tall';
Axis: 'ex'|Why|'x'|'y'|'z'|'horizontal'|'vertical';
Be: 'become'|'be';
Define: 'create'|'define'|'make';
Describe: 'describe';
Do: 'do';
Dynamic: 'dynamic';
Extremities: 'extremities';
Extremity: 'ear'|'eye'|'eyes'|'finger'|'foot'|'hand'|'heel'|'nose'|'toe';
Faces: 'faces';
From: 'from';
Forget: 'forget'|'delete';
Goals: 'goals'|'target positions'|'targets';
Greeting: 'hello'|'high'|'hi'|'hey';
Have: 'have'|'wear';
Hold: 'hold';
How: 'how';
Initialize: 'initialize';
Iam: 'i am';
Isay: 'i say';
Isaid: 'i said';
It: 'it';
Know: 'know';
Limbs:'limbs';
// Note legs and arms must be modified by a side
Limb: 'arm'|'head'|'leg'|'torso';
Limits: 'limits';
List: 'list';
Location: 'location';
Me: 'me';
Means: 'means';
Metric: 'age'|'cadence'|'cycles'|'cycle count'|'cycle time'|'duty cycle'|'height'|'name';
Mittens: 'mittens';
Motors: 'devices'|'joints'|'motors';
Motor: 'device'|'joint'|'motor';
Move: 'bend'|'go'|'move'|'turn';
My: 'my';
Names: 'names';
Of: 'of'|'for';
Off: 'off'|'disabled';
On: 'on'|'enabled';
Joint: 'ankle'|'elbow'|'hip'|'thigh'|'knee'|'neck'|'shoulder'|'chest'|'bust'|'abdomen'|'abs';
Parameters: 'properties'|'parameters'|'settings';
Poses: 'poses';
Pose: 'pose';
Properties: 'ids'|'positions'|'offsets'|'minimum angles'|'maximum angles'|'angles'|'motor types'|'orientations'|'ranges'|'speeds'|'states'|'torques'|'loads'|'temperatures'|'temps'|'voltages'|'velocities';
Property: 'id'|'position'|'offset'|'min angle'|'max angle'|'minimum angle'|'maximum angle'|'angle'|'motor type'|'orientation'|'range'|'speed'|'state'|'torque'|'load'|'temperature'|'temp'|'voltage'|'velocity';
Reset: 'reset';
Salutation:'bert'|'burt'|'now'|'please'|'wake up'|Isaid;
Save: 'save'|'record';
Series: 'series';
Set: 'set';
Side: 'left'|'right'|'other';
Speed: 'in slow motion'|'very fast'|'normally'|'very quickly'|'quickly'|'very slowly'|'slowly'|'slower'|'very slow'|'slow'|'faster'|'quicker'|'fast'|'normal';
Static: 'static';
Straighten: 'straighten';
Take: 'assume' | 'execute' | 'take';
Tell: 'tell';
Then: 'then';
There: 'there'|'their';
To: 'to';
Unit: 'degrees';
Use: 'use';
Value: (INTEGER|DECIMAL);
You: 'you';
What: 'what';
When: 'when';
Where: 'where';
Who: 'who';

COMMA: ',';
COLON: ':';
DECIMAL: DASH? DIGIT* PERIOD DIGIT*;
INTEGER: DASH?DIGIT+;
NAME:   ALPHA+ (DASH|PERIOD)? DIGIT*;


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