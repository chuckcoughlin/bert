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
	Salutation? (Command|Halt)                       			# handleSingleWordCommand
	;
      
// Request for information
question:
	  How Adjective 'are' 'you'               				    # attributeQuestion
	| What 'is' Article? Property Of Article? Side? Joint Axis? # jointPropertyQuestion
	| What 'is' Article? Metric   				                # metricsQuestion
	| What 'is' Article? Axis? Property Of Article? Side? Joint # positionQuestion
	;

// Convey information to the robot.
declaration:
	;
	
	
// Pardon the license taken with some of these categories ...
Article: 'a'|'an'|'the'|'this'|'that'|'your';
Adjective: 'old'|'tall';
Axis: 'x'|'y'|'z';
Command: 'attention'|'freeze'|'relax'|'wake up';
Halt: 'die'|'exit'|'halt'|'quit'|'shutdown'|'stop';
How: 'how';
Metric: 'age'|'cadence'|'cycle time'|'duty cycle'|'height'|'name';
Of: 'of';
Joint: 'ankle'|'arm'|'elbow'|'head'|'hip'|'knee'|'neck'|'shoulder';
Property: 'id'|'position'|'offset'|'minimum angle'|'maximum angle'|'motor type'|'orientation'|'speed'|'torque';
Salutation:'bert'|'burt';
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