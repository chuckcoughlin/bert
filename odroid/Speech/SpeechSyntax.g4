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
	Salutation? Command                       # handleSingleWordCommand
	;
      
// Request for information
question:
	  How HowAdjective 'are' 'you'            # howQuestion
	| What 'is' Possessive? Article? Property # whatQuestion
	;

// Convey information to the robot.
declaration:
	;
	
	

Article: 'a'|'an'|'the'|'this'|'that';
Command: 'attention'|'freeze'|'relax'|'stop'|'wake up';
How: 'how';
HowAdjective: 'old'|'tall';
Possessive: 'your';
Property: 'cadence'|'cycle time'|'duty cycle';
Salutation:'bert'|'burt';
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