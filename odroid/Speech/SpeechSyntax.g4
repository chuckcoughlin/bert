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
	Salutation? Command      # handleSingleWordCommand
	;
      
// Request for information
question:
	;

// Convey information to the robot.
declaration:
	;
	
	

// Single-word imperatives
Command: 'relax'|'stop'|'wake u';
Property: 'cadence';
Salutation:'bert'|'burt';


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
fragment WS:  [\t\r\n,]+ ->skip;   // Whitespace only matters around operators
