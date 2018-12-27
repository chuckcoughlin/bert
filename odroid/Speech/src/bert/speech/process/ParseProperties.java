/**
 *   (c) 2014-2015  ILS Automation. All rights reserved.
 *  
 */
package bert.speech.process;


/**
 *  Define properties that are common to all scopes.
 */
public interface ParseProperties   {
	public final static String MODULE_ID = "testframe";        // See module-tf-test.xml
	public final static String MODULE_NAME = "TF";        // See build-atm-test.xml
	public final static String TEST_SCRIPT_PACKAGE = "system.ils.tf";         // Python package name for app test
	public final static String TIMESTAMP_FORMAT = "yyyy.MM.dd HH:mm:ss.SSS";  // Format for writing timestamp
	
	// These are the names of built-in variables/markups.
	public final static String BUILTIN_AVECPU                = "AVECPU";
	public final static String BUILTIN_AVEMEM                = "AVEMEM";
	public final static String BUILTIN_CPU                   = "CPU";
	public final static String BUILTIN_DATE                  = "DATE";
	public final static String BUILTIN_MEM                   = "MEM";
	public final static String BUILTIN_PROJECT               = "PROJECT";
	public final static String BUILTIN_TDATE                 = "TDATE";
	public final static String BUILTIN_TIME                  = "TIME";
	public final static String BUILTIN_TTIME                 = "TTIME";
	public final static String BUILTIN_USER                  = "USER";
	
	public final static String BUNDLE_PREFIX = "apptest";
	public final static String CHART_RESOURCE_TYPE = "sfc-chart-ui-model";
	
	// These are the Dataset column names for linking the ColumnTagMap table. Order is important
	public final static String[] COLUMN_TAG_MAP_COLUMN_NAMES      = {"ColumnName","TagPath","DataType"};
	public final static Class<?>[] COLUMN_TAG_MAP_COLUMN_CLASSES  = {java.lang.String.class,java.lang.String.class,java.lang.String.class};
	
	
	// These strings are recognized status values returned on an attempt to start a script
	public final static String SCRIPT_START_NO_FILE    = "FileNotFound";  // Failed to open script file
	public final static String SCRIPT_START_IN_PROCESS = "InProcess";     // Another test is running
	public final static String SCRIPT_START_SUCCESS    = "Started";       // Success, test now running
	public final static String SCRIPT_COMPLETE_SUCCESS    = "Completed";  // Success, test now done
	
	// These are keys used in the CommandPrototype shared dictionary
	public final static String CP_ARGLIST          = "ArgList";            // List<String>
	public final static String CP_ARGMAP           = "ArgMap";             // Map<String,String>
	public final static String CP_COMMENT          = "Comment";             // String
	public final static String CP_COUNT             = "Count";              // Integer
	public final static String CP_CURRENT_DATE     = "CurrentDate";         // String  (yyyy/MM/dd)
	public final static String CP_CURRENT_DIRECTORY = "CurrentDirectory";   // String  
	public final static String CP_CURRENT_PATH     = "CurrentPath";         // String 
	public final static String CP_CURRENT_PROJECT  = "CurrentProject";      // String 
	public final static String CP_CURRENT_TAGSET   = "CurrentTagSet";       // String (name of current tagset)
	public final static String CP_CURRENT_TIME     = "CurrentTime";         // String  (HH:mm:ss)
	public final static String CP_CURRENT_USER     = "CurrentUser";         // String 
	public final static String CP_DATABASE         = "DatabaseConnection";  // String
	public final static String CP_DEFAULT_POLL_INTERVAL   = "defaultpollinterval";// Long (msecs) 
	public final static String CP_DEFAULT_TIMEOUT         = "defaulttimeout";     // Long (msecs)
	public final static String CP_NAME         = "Name";              // String
	public final static String CP_NOT          = "Not";               // Boolean
	public final static String CP_NUMPARAM     = "NumericParameter";  // Double
	public final static String CP_OPERATOR     = "Operator";       // String
	public final static String CP_OPERATOR1     = "Operator1";     // String
	public final static String CP_OPERATOR2     = "Operator2";     // String
	public final static String CP_PATH         = "Path";              // String
	public final static String CP_POLL_INTERVAL     = "PollInterval"; // Long (msecs)
	public final static String CP_SCRIPT_PROTOTYPE  = "ScriptPrototype"; // String (python path)
	public final static String CP_SCRIPT_ARGS       = "ScriptArgs";      // String (coma-separated)
	public final static String CP_PROVIDER     = "TagProvider";    // String
	public final static String CP_PYLIB_DIRECTORY = "PylibDirectory";   // String 
	public final static String CP_TAG_DATABASE = "TagDatabaseConnection";   // String
	public final static String CP_TAGLIST      = "TagList";        // List<String>
	public final static String CP_TAGMAP       = "TagMap";         // Map<String,List<TagPath>>
	public final static String CP_TAGPATHS     = "TagPaths";       // List<TagPath>
	public final static String CP_TAG_TABLE    = "TagDatabaseTable";   // String
	public final static String CP_TAG_TIME_COL = "TagTimestampColumn"; // String
	public final static String CP_TEXTPARAM    = "TextParameter";  // String
	public final static String CP_TIMEFACTOR   = "TimeFactor";     // Double   Time speedup factor
	public final static String CP_VALUE        = "Value";          // String
	public final static String CP_VALUE1       = "Value1";         // Double
	public final static String CP_VALUE2       = "Value2";         // Double

	// This is a marker to indicate that the test should abort on an assertion failure
	public final static String CP_REQUIRE_MARKER    = "RequireMarker";  // String
	// This is a marker to indicate the presence of a timestamp in the script line
	public final static String CP_TIMESTAMP_MARKER    = "TimestampMarker";  // String
	
	// These are keys used in the shared dictionary for parsing errors
	public final static String EXPR_ERR_MESSAGE    = "message";
	public final static String EXPR_ERR_LINE       = "lineno";
	public final static String EXPR_ERR_POSITION   = "position";   // Character position
	public final static String EXPR_ERR_TOKEN      = "token";
	
	// These are one-time commands recognized by the NotificationHandler
	public final static String ANIMATE_CHART                 = "animate:chart";
	public final static String SHOW_BROWSER                  = "show:browser";
	public final static String SHOW_DIAGRAM                  = "show:diagram";
	public final static String COMMAND_BUTTON                = "button";
	public final static String SHOW_CHART                    = "show:chart";
	public final static String COMMAND_MENU                  = "menu";
	
	// These are the notification commands recognized by the NotificationRepeater
	public final static String REPEATER_START      = "start";
	public final static String REPEATER_STOP       = "stop";
	public final static String REPEATER_WATCH      = "watch";
	
	// These keys are used for user preferences
	public final static String PREFERENCES_NAME   = "TestFramePreferences";
	public final static String PREF_DATA_SOURCE = "TestDatasource";
	public final static String PREF_SCRIPT_PATH = "TestScriptPath";
	public final static String PREF_SETUP_PATH  = "TestSetupPath";
	public final static String PREF_LOG_PATH    = "TestLogPath";
	public final static String PREF_PY_PATH     = "TestPyPath";
	public final static String PREF_TEARDOWN_PATH = "TestTeardownPath";

	public final static String PREF_DB_INCONNECTION  = "DbInConnection";
	public final static String PREF_DB_INTABLE       = "DbInTable";
	public final static String PREF_DB_INTIMECOLUMN  = "DbInTimestampColumn";
	public final static String PREF_DB_ONLY          = "DbOnly";
	public final static String PREF_DB_OUTCONNECTION = "DbOutConnection";
	public final static String PREF_DB_OUTTABLE      = "DbOutTable";
	public final static String PREF_DB_TIMESPEEDUP   = "DbTimespeedup";
	public final static String PREF_INPROVIDER       = "InputTagProvider";
	public final static String PREF_OUTPROVIDER      = "OutputTagProvider";
	public final static String PREF_STEP_COMPLETION_PROTOTYPE  = "StepCompletionScriptPrototype";
	public final static String PREF_STEP_COMPLETION_ARGS       = "StepCompletionScriptArgs";
	public final static String PREF_TEST_END         = "TestEndDateTime";
	public final static String PREF_TEST_START       = "TestStartDateTime";
	
	// These are keys used for notifications, controller->client
	public final static String PROPERTY_CONTROLLER_STATE= "controllerstate";
	public final static String PROPERTY_CPU             = "cpu";
	public final static String PROPERTY_DEFAULT_POLL_INTERVAL   = "defaultpollinterval";// Long (msecs) 
	public final static String PROPERTY_DEFAULT_TIMEOUT         = "defaulttimeout";     // Long (msecs)
	public final static String PROPERTY_FAIL_COUNT      = "failures";
	public final static String PROPERTY_LOG_MESSAGE     = "logmessage";
	public final static String PROPERTY_LOGPATH         = "logfilepath";
	public final static String PROPERTY_MEM             = "memory";
	public final static String PROPERTY_PASS_COUNT      = "successes";
	public final static String PROPERTY_PROGRESS        = "progress";
	public final static String PROPERTY_PYLIB           = "pylib";
	public final static String PROPERTY_SCRIPTPATH      = "scriptfilepath";
	public final static String PROPERTY_SETUPPATH       = "setupfilepath";
	public final static String PROPERTY_STATUS_MESSAGE  = "statusmessage";
	public final static String PROPERTY_TAGMAP          = "tagmap";      // Map<String,String>
	public final static String PROPERTY_TEST_STEP       = "teststep";
	public final static String PROPERTY_TEST_NAME       = "testname";
	public final static String PROPERTY_TEARDOWNPATH    = "teardownfilepath";
	public final static String PROPERTY_TIMESCALE       = "timescale";
}
