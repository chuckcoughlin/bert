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
	| Greeting Greeting? Salutation?                                        # handleGreeting
	| Salutation? Initialize Article? Motors   					            # initializeJoints
	| Salutation? List Article? (Limits|Goals) Of Article? Side? Joint Axis?    # handleBulkPropertyRequest
	| Salutation? List Article? Properties Of Article? Motors       # handleListCommand
	| Salutation? List Article? Motor? Properties                   # handleListCommand
	| Salutation? Move (It | Article? Side? Joint Axis?) To? Value Unit?       # moveMotor
	| Salutation? Move Speed                                                   # setSpeed
	| Salutation? (Hold|Freeze|Relax) Article? Side? (It|Joint|Limb)? Axis?	   # enableTorque
    | Salutation? Set Article? Side? Joint? Axis? Property To (Value|On|Off|Speed) Unit?		     # setMotorPrpoerty
	| Salutation? Set Article? Property Of Article? Side? Joint Axis? To (Value|On|Off|Speed) Unit?  # setMotorProperty
	| Salutation? Straighten (It|Article? Side? (Joint|Limb) Axis? )    	    # straightenJoint
	| Salutation? Forget NAME                                                   # deletePose
	| Salutation? phrase                                    		            # handleArbitraryCommand
	;

// Request for information
question:
      Salutation?  How Attribute Are You 						# attributeQuestion
    | Salutation?  Are You There					            # personalQuestion
    | What Are Article? (Dynamic|Static) Motor? Parameters		# parameterListQuestion
    | What Are Article? (Dynamic|Static) Parameters Of Article? Motors # parameterListQuestion
    | What Are Article? Names Of Article? (Motors|Limbs|Appendages)   # bodyPartListQuestion
    | What Are Article? Names Of Article? (Faces|Poses)               # databaseListQuestion
    | What Are Article? (Limits|Goals) Of Article? Side? Joint Axis?  # handleBulkPropertyQuestion
    | What Is Article? Side? Joint Axis? Property               # jointPropertyQuestion
    | What Is Article? Axis? Property Of Article? Side? Joint   # motorPropertyQuestion1
    | What Is Article? Property Of Article? Side? Joint Axis?   # motorPropertyQuestion2
    | What Is Article? Metric   				                # metricsQuestion
    | What Is Article? Adjective? Pose                          # poseQuestion
    | Where Is Article? Side? (Appendage|Joint)	Axis?       	# limbLocationQuestion
    | Why Do You Have Mittens									# whyMittens
	;

// Convey information to the robot.
declaration:
      You Are phrase								    # declarePose1
    | Article Pose Is phrase 						    # declarePose2
	| Save Article? Pose (As phrase)?           	    # declareNoNamePose
	| To phrase Means To Take Article? Pose phrase	    # mapPoseToCommand1
	| To phrase Means You Are phrase            	    # mapPoseToCommand2
	| To phrase Is To Be phrase						    # mapPoseToCommand3
	| When Isay phrase Then? Take Article? Pose phrase	# mapPoseToCommand4
	| When You phrase Then You Are phrase			    # mapPoseToCommand5
	
	;

// Arbitrary string of words
phrase: (NAME|Value|Appendage|Are|As|Article|Axis|Freeze|Hold|It|Joint|Move|Of|Relax|Reset|Set|Side|Straighten|Take|To)+    # wordList
    ;

// First is a list of terms that are used below or use word that appear elsewhere
Freeze: 'freeze'|'stiffen'|'tighten'|'go rigid';
Relax:'loosen'|'relax'|'go limp';
Why: 'why';
// Pardon the license taken with some of these categories ...
Article: 'a'|'an'|'the'|'this'|'that'|'your';
Adjective: 'current';
Appendages: 'appendages';
Appendage: 'ear'|'eye'|'eyes'|'finger'|'foot'|'hand'|'heel'|'nose'|'toe';
Are: 'are';
As: 'as';
Attribute: 'old'|'tall';
Axis: 'ex'|Why|'x'|'y'|'z'|'horizontal'|'vertical';
Be: 'become'|'be';
Do: 'do';
Dynamic: 'dynamic';
Faces: 'faces';
Forget: 'forget';
Goals: 'goals'|'targets';
Greeting: 'hello'|'high'|'hi'|'hey';
Have: 'have'|'wear';
Hold: 'hold';
How: 'how';
Initialize: 'initialize';
Isay: 'i say';
Is: 'is';
It: 'it';
List: ('tell me'|'describe'|'list'|'show me');
Limbs:'limbs';
Limb: 'arm'|'back'|'head'|'leg'|'torso';
Limits: 'limits';
Means: 'means';
Metric: 'age'|'cadence'|'cycles'|'cycle count'|'cycle time'|'duty cycle'|'height'|'name';
Mittens: 'mittens';
Motors: 'devices'|'joints'|'motors';
Motor: 'device'|'joint'|'motor';
Move: 'bend'|'go'|'move'|'turn';
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
Salutation:'bert'|'burt'|'now'|'please'|'wake up';
Save: 'save';
Set: 'set';
Side: 'left'|'right'|'other';
Speed: 'in slow motion'|'very fast'|'fast'|'normally'|'very quickly'|'quickly'|'very slowly'|'slowly'|'very slow'|'slow'|'faster'|'slower'|'quicker'|'normal';
Static: 'static';
Straighten: 'straighten';
Take: 'assume' | 'take';
Then: 'then';
There: 'there'|'their';
To: 'to';
Unit: 'degrees';
Value: (INTEGER|DECIMAL);
You: 'you';
What: 'what';
When: 'when';
Where: 'where';

COMMA: ',';
COLON: ':';
DECIMAL: DASH? DIGIT* PERIOD DIGIT*;
INTEGER: DASH?DIGIT+;
NAME:  (ALPHA+);


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