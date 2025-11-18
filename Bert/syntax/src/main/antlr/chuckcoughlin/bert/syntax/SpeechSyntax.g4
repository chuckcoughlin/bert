/*
 * Copyright 2018-2019 Chuck coughlin. All Rights Reserved.
 * Define a formal grammar for speech recognized by "Bert".
 * All incoming text has been converted to lower case to allow for
 * case-insensitive syntax.
 */
grammar SpeechSyntax;

/** Parser entry point */
line: statement EOF;

statement:
      command
	| question
	| declaration
	| enumeration
	;

// Imperatives directing the robot to take an action
command:
	Salutation                                                              # handleSalutation
	| Describe Pose phrase Value          	    							# poseDescription
	| Describe Article? Adjective? Pose                                     # currentPoseDescription
	| Greeting Greeting? Salutation?                                        # handleGreeting
	| Salutation? Initialize Article? Motors   					            # initializeJoints
    | Straighten (It|Article? Side? (Joint|Limb) Axis? )                    # straightenJoint
	| Salutation? Move (It | Article? Side? Joint Axis?) To? Value Units?    # moveMotor
	| Salutation? (Move| Place) (It | Article? Side? Appendage) (At|To)? Value Value Value # placeAppendage
	| Salutation? (Move| Place) (It | Article? Side? Appendage) (Side|Direction) By? Value Units?               # jogAppendage
	| Salutation? (Move| Place) (It | Article? Side? Appendage) By? Value Units? To? Article? (Side?|Direction?) # jogAppendage
	| Salutation? Move Speed                                                # setSpeed
	| Salutation? (Hold|Freeze|Relax) Article? Side? (It|Joint|Limb)? Axis?	# enableTorque
    | Salutation? Set Article? Side? Joint? Axis? Property To (Value|On|Off|Speed) Units?		     # setMotorPrpoerty
	| Salutation? Set Article? Property Of Article? Side? Joint Axis? To (Value|On|Off|Speed) Units?  # setMotorProperty
	| Salutation? phrase                                    		         # handleArbitraryCommandOrResponse
	;

// Lists of various attributes
enumeration:
      enumerate Article? (Motors|Limbs|Appendages)                              # listBodyParts
    | enumerate Article? (Actions|Faces|Poses)                                  # listDatabaseElements
    | enumerate Article? (Limits|Goals) Of Article? Side? Joint Axis?    		# listLimits
    | enumerate Article? (Joint|Limb) Locations 	                            # listLocations
    | enumerate Article? (Dynamic|Static) Parameters Of Article? Motors         # listMotorParameters
    | enumerate Article? (Dynamic|Static) Motor? Parameters		                # listMotorParameters
    | enumerate  Article? Properties Of Article? Motors                         # listProperty
    | enumerate Article? Motor? Properties                                      # listProperty
    | What Actions Do You Know													# listActionNames
    | What Poses Do You Know													# listPoseNames
    | Where Are Article (Motors|Limbs)										    # listLocations
    | Who Do You Know															# listFaceNames
    ;

// Request for information
question:
      Salutation?  How Attribute Are You 						                # attributeQuestion
    | Salutation?  Are You There					                            # personalQuestion
    | (What Is|Tell Me) Article? (Location|Orientation) Of Article? Side? (Appendage|Joint)	Axis? # jointLocationQuestion
    | (What Is|Tell Me) Article? Axis? Property Of Article? Side? Joint                       # jointPropertyQuestion
    | (What Is|Tell Me) Article? Property (Of|On) Article? Side? Joint Axis?                  # jointPropertyQuestion
    | (What Is|Tell Me) Article? Side? Joint Axis? Property                                   # jointPropertyQuestion
    | (What Is|Tell Me) Article? Metric   				    # metricsQuestion
    | Where Is Article? Side? (Appendage|Joint) Axis?       # jointLocationQuestion
    | Why Do You Have Mittens								# whyMittens
	;

// Convey information to the robot.
declaration:
	  Forget (Face|Action|Pose) phrase                  # deleteUserData
	| Forget Pose phrase Value                          # deleteUserData
	| My Metric Is phrase                               # setUserName
	| Iam phrase                                        # setUserName
	| Take Article? Pose phrase Value          	        # assumePose
    | Article Pose Is phrase Value						# definePose
	| Save Article? Pose As? phrase Value          	    # definePose
	| This Is phrase Value          	                # definePose
	| You Are phrase Value          	                # definePose
	| Define phrase As Article Series Of phrase Poses   # defineAction1
	| Define phrase (From|As) phrase                    # defineAction1
	| When Isay phrase Use phrase                       # defineAction1
	| Use phrase Poses? To Define phrase				# defineAction2
	| Follow phrase With phrase				            # defineActionFollowOn
	| Stop phrase				                        # stopAction
	;

// Commands to obtain attributes. Of these 'list' returns a JSON string. Otherwise values are comma-separated
enumerate: (Download|List|What Are|Tell Me|Name) (Article Names Of)?;

// Arbitrary string of words - inclue some key words. Digits are allowed only as a suffix to a word
phrase: (NAME|Appendage|Are|As|Article|Axis|Freeze|Hold|It|Joint|Move|Of|Relax|Reset|Set|Side|Straighten|Take|To)+    # wordList
    ;

// First is a list of terms that are used below or use words that appear elsewhere
Are: 'are';
Freeze: 'freeze'|'stiffen'|'tighten up'|'go rigid';
Is: 'is';
Relax:'loosen up'|'relax'|'go limp';
Side: 'left'|'right'|'other';
Why: 'why';

// Pardon the license taken with some of these categories ...
Actions: 'actions';
Action: 'action';
Article: 'a'|'an'|'the'|This|'that'|'your';
Adjective: 'current';
Appendages: 'appendages'|'end effectors';
Appendage: 'ear'|'eye'|'eyes'|'finger'|'hand'|'heel'|'nose'|'toe';
As: 'as';
At: 'at';
Attribute: 'old'|'tall';
Axis: 'ex'|Why|'x'|'y'|'z'|'horizontal'|'vertical';
Be: 'become'|'be';
By: 'by';
Define: 'create'|'define'|'make';
Describe: 'describe';
Direction: 'back'|'forward'|'up'|'down';
Do: 'do';
Download: 'download';
Dynamic: 'dynamic';
Faces: 'faces';
Face: 'face';
Follow: 'follow';
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
List: 'list'|'what are'|'tell me';
Locations: 'locations';
Location: 'location';
Me: 'me';
Means: 'means';
Metric: 'age'|'cadence'|'cycles'|'cycle count'|'cycle time'|'duty cycle'|'height'|Name;
Mittens: 'mittens';
Motors: 'devices'|'joints'|'motors';
Motor: 'device'|'joint'|'motor';
Move: 'bend'|'go'|'move'|'turn';
My: 'my';
Names: 'names';
Name: 'name';
Of: 'of'|'for';
Off: 'off'|'disabled';
On: 'on'|'enabled';
Orientation: 'orientation';
Joint: 'ankle'|'elbow'|'hip'|'thigh'|'knee'|'neck'|'shoulder'|'chest'|'bust'|'abdomen'|'abs'|'imu';
Parameters: 'properties'|'parameters'|'settings';
Place: 'place';
Poses: 'poses';
Pose: 'pose';
Properties: 'ids'|'positions'|'offsets'|'minimum angles'|'maximum angles'|'angles'|'motor types'|'orientations'|'ranges'|'speeds'|'states'|'torques'|'loads'|'temperatures'|'temps'|'voltages'|'velocities';
Property: 'id'|'position'|'offset'|'min angle'|'max angle'|'max speed'|'max torque'|'minimum angle'|'maximum angle'|'maximum speed'|'maximum torque'|'angle'|'motor type'|Orientation|'range'|'speed'|'state'|'torque'|'load'|'temperature'|'temp'|'voltage'|'velocity';
Reset: 'reset';
Salutation:'bert'|'burt'|'now'|'please'|'wake up'|'isaid';
Save: 'save'|'record';
Series: 'series';
Set: 'set';
Speed: 'in slow motion'|'very fast'|'normally'|'very quickly'|'quickly'|'very slowly'|'slowly'|'slower'|'very slow'|'slow'|'faster'|'quicker'|'fast'|'normal';
Static: 'static';
Stop: 'stop';
Straighten: 'straighten';
Take: 'assume' | 'execute' | 'take';
Tell: 'tell';
Then: 'then';
There: 'there'|'their';
This: 'this';
To: 'to';
Units: 'degrees'|'millimeters'|'mm'|'deg';
Use: 'use';
Value: (INTEGER|DECIMAL);
You: 'you';
What: 'what';
When: 'when';
Where: 'where';
Who: 'who'|'whom';
With: 'with';

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