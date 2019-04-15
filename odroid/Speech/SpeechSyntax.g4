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
	  Salutation? (Command|Halt|Shutdown)                        # handleSingleWordCommand
    | Salutation? List Article? Configuration                    # configurationRequest
	| Salutation? List Article? (Limits|Goals) Of Article? Side? Joint Axis? # handleBulkPropertyRequest
	| Salutation? List Article? Properties Of Article? Motors	# handleListCommand1
	| Salutation? List Article? Motor? Properties				# handleListCommand2
	| Salutation? (Command|Halt|Shutdown)                      	# doNothing
	;

// Request for information
question:
        How Adjective 'are' 'you'                               # attributeQuestion
	| What 'is' Article? Configuration                          # configurationQuestion
	| What 'are' Article? (Limits|Goals) Of Article? Side? Joint Axis? # handleBulkPropertyQuestion
	| What 'is' Article? Property Of Article? Side? Joint Axis? # jointPropertyQuestion
	| What 'is' Article? Metric   				    			# metricsQuestion
	| What 'is' Article? Axis? Property Of Article? Side? Joint # motorPropertyQuestion
	;

// Convey information to the robot.
declaration:
	;


// Pardon the license taken with some of these categories ...
Article: 'a'|'an'|'the'|'this'|'that'|'your';
Adjective: 'old'|'tall';
Axis: 'x'|'y'|'z';
Command: ('attention'|'freeze'|'relax'|'wake up');
Configuration: 'configuration';
Goals: 'goals'|'targets';
Halt: 'die'|'exit'|'halt'|'quit'|'stop';
How: 'how';
List: ('tell me'|'describe'|'list'|'what are');
Limits: 'limits';
Metric: 'age'|'cadence'|'cycle time'|'duty cycle'|'height'|'name';
Motors: 'devices'|'joints'|'motors';
Motor: 'device'|'joint'|'motor';
Of: 'of'|'on'|'for';
Joint: 'ankle'|'arm'|'elbow'|'head'|'hip'|'knee'|'neck'|'shoulder'|'chest'|'bust'|'abdomen'|'abs';
Properties: 'ids'|'positions'|'offsets'|'minimum angles'|'maximum angles'|'angles'|'motor types'|'orientations'|'speeds'|'torques'|'loads'|'temperatures'|'voltages'|'velocities';
Property: 'id'|'position'|'offset'|'minimum angle'|'maximum angle'|'angle'|'motor type'|'orientation'|'speed'|'torque'|'load'|'temperature'|'voltage'|'velocity';
Salutation:'bert'|'burt';
Shutdown: 'power off'|'shut down'|'shutdown';
Side: 'left'|'right';
What: 'what';


COMMA: ',';   // Not a fragment because of "value"
COLON: ':';
NAME:  (ALPHA (ALPHA|DIGIT|DASH|SLASH|UNDERSCORE|PERIOD)*);



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