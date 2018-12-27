/**
 *   (c) 2014-2015  ILS Automation. All rights reserved.
 *  
 */
package com.ils.tf.gateway.command;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ils.common.tag.DatabaseStoringProviderDelegate;
import com.ils.common.tag.ProviderRegistry;
import com.ils.common.tag.TagWriter;
import com.ils.tf.common.TestFrameProperties;
import com.ils.tf.gateway.TestFrameGatewayHook;
import com.ils.tf.gateway.controller.ProgressRecorder;
import com.ils.tf.gateway.player.PlayerController;
import com.ils.tf.gateway.player.ScriptPlayer;
import com.ils.tf.gateway.python.PythonScript;
import com.inductiveautomation.ignition.common.sqltags.model.TagPath;
import com.inductiveautomation.ignition.common.sqltags.parser.TagPathParser;
import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;



/**
 *  A command prototype contains the various attributes needed to describe
 *  (and execute) one of the many commands available in the test script.
 *  The command is built up as it is passed among the various nodes in the 
 *  parse tree. A single prototype instance executes a single line in the
 *  script file.
 */
public class CommandPrototype  {
	private static final String CLSS = "CommandPrototype";
	private static final String NO_VALUE = "Script did not return a value";
	private static final Double TOLERANCE = .0001;   // For numeric equivalence
	private String commandName = "";
	private CommandType commandType = CommandType.NONE;
	private final LoggerEx log;
	private final HashMap<String,Object> sharedDictionary;
	private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
	private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
	private Date timestamp = null;
	private final AliasRegistry registry;

	/**
	 * Create a command prototype. This object has enough information
	 * to construct and execute any of the commands in the module. 
	 * @param shared
	 */
	public CommandPrototype(HashMap<String,Object>shared) {
		this.sharedDictionary = shared;
		this.log = LogUtil.getLogger(getClass().getPackage().getName());
		this.registry = AliasRegistry.getInstance();
	}
	
	public String getCommandName() { return commandName;}
	public CommandType getCommandType() { return commandType;}
	/**
	 * The delay is zero for most commands without timestamps. If there
	 * is a timestamp, return -1 to indicate that there is no delay.
	 * @return the number of milliseconds to delay for this command.
	 */
	public long getDelay() { 
		Object marker = sharedDictionary.get(TestFrameProperties.CP_TIMESTAMP_MARKER);
		if( marker!=null ) return -1;  // There is a timestamp
		// For the wait command, the interval is msecs
		long delay = 0;
		if( commandType.equals(CommandType.WAIT)  ) {
			Long interval = (Long)sharedDictionary.get(TestFrameProperties.CP_VALUE);
			if( interval!=null) { 
				delay = interval.longValue();
			}
		}
		// Until command in effect
		else if(sharedDictionary.get(TestFrameProperties.CP_COUNT)!=null) {
			int count = ((Integer)sharedDictionary.get(TestFrameProperties.CP_COUNT)).intValue();
			if( count>0 ) {
				delay = ((Long)sharedDictionary.get(TestFrameProperties.CP_POLL_INTERVAL)).longValue();
				//log.tracef("%s.getDelay: Count %d, delay = %d",TAG,count,delay);
			}
		}
		return delay;
	}
	/**
	 * @return the timescale as configured in the test file.
	 *         If there is none, return 1.0
	 */
	public double getTimescale() { 
		Double scale = (Double)sharedDictionary.get(TestFrameProperties.CP_TIMEFACTOR);
		if( scale!=null ) return scale.doubleValue();
		else return 1.0;
	}
	/**
	 * Most commands do not have a time-stamp, but neither do they clear time-stamps from
	 * previous commands. Unless the command is of a type that is test-time aware, return 0.
	 * @return the test-time time-stamp from the test file. 
	 */
	public long getTimestamp() { 
		long result = 0;
		// The marker indicates that the script line has a timestamp.
		Object marker = sharedDictionary.get(TestFrameProperties.CP_TIMESTAMP_MARKER);
		if( marker!=null ) {
			String date = (String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_DATE);
			String time = (String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_TIME);
			String datetime = String.format("%s %s",date, time);
			try {
				timestamp = dateTimeFormat.parse(datetime);
				result = timestamp.getTime();
			}
			catch(ParseException pe){
				log.warnf("%s.getTimestamp: %s is an illegal date (%s)", CLSS,datetime,pe.getLocalizedMessage());
			}
		}
		return result;
	}
	public void setCommandName(String name) { this.commandName = name; }
	public void setCommandType(CommandType type) { this.commandType = type; }
	
	/**
	 * Perform the action which we've just configured. 
	 * @param lineno for error messages
	 * @param globals
	 * @param record
	 * @return true if the script player should proceed to the next command.
	 */
	public ResponseOption execute(GatewayContext context,long lineno,Map<String,Object> globals,ProgressRecorder recorder) {
		log.debugf("%s.execute: %s (%s)",CLSS,commandType,commandName);
		if( handleError(globals,lineno,recorder) ) return ResponseOption.HALT;     // Found a parsing error
		ResponseOption response = ResponseOption.CONTINUE;
		if(commandType.equals(CommandType.HALT)) {

			// Halt immediately.
			return ResponseOption.HALT;
		}
		// Assert a result for list contents. 
		else if(commandType.equals(CommandType.LIST_ASSERTION)) {
			boolean require = (sharedDictionary.get(TestFrameProperties.CP_REQUIRE_MARKER)!=null);
			Object val1 = sharedDictionary.get(TestFrameProperties.CP_VALUE1);
			Object val2 = sharedDictionary.get(TestFrameProperties.CP_VALUE2);
			String comment = (String)sharedDictionary.get(TestFrameProperties.CP_COMMENT);
			log.debugf("%s.execute: asserting %s contains %s",CLSS,val1,val2);
			String reason = null;
			// If the arguments are aliases, then replace with the lookup
			if( registry.aliasExists(val1.toString())) {
				Object arg = registry.valueForAlias(val1.toString());
				if( arg!=null ) val1 = arg;
				else reason = NO_VALUE;
			}
			if( registry.aliasExists(val2.toString())) {
				Object arg = registry.valueForAlias(val2.toString());
				if( arg!=null ) val2 = arg;
				else reason = NO_VALUE;
			}
	
			boolean outcome = false;
			if( reason == null ) {
				reason = computeContains(val1,val2);
				outcome = (reason==null);
				log.tracef("%s.execute: assert %s contains %s is %s",CLSS,val1,val2,(reason==null?"ok":comment));
				boolean negate = false;
				if( sharedDictionary.get(TestFrameProperties.CP_NOT)!=null) negate = true;
				if( negate ) outcome = !outcome;
			}
			response = handleAssertionResult(globals,recorder,outcome,comment,reason);
			if(require && !outcome ) return ResponseOption.HALT;
		}
		// Assert a result for list size. 
		else if(commandType.equals(CommandType.LIST_SIZE_ASSERTION)) {
			boolean require = (sharedDictionary.get(TestFrameProperties.CP_REQUIRE_MARKER)!=null);
			Object val1 = sharedDictionary.get(TestFrameProperties.CP_VALUE1);
			String opr =  (String)sharedDictionary.get(TestFrameProperties.CP_OPERATOR);
			Object val2 = sharedDictionary.get(TestFrameProperties.CP_VALUE2);
			String comment = (String)sharedDictionary.get(TestFrameProperties.CP_COMMENT);
			log.debugf("%s.execute: asserting %s contains %s",CLSS,val1,val2);
			String reason = null;
			// If the arguments are aliases, then replace with the lookup
			if( registry.aliasExists(val1.toString())) {
				Object arg = registry.valueForAlias(val1.toString());
				if( arg!=null ) val1 = arg;
				else reason = NO_VALUE;
			}
			if( registry.aliasExists(val2.toString())) {
				Object arg = registry.valueForAlias(val2.toString());
				if( arg!=null ) val2 = arg;
				else reason = NO_VALUE;
			}
			boolean outcome = false;
			if( reason==null ) {
				reason = compareListSize(val1,val2,opr);
				outcome = (reason==null);
				log.tracef("%s.execute: assert %s contains %s is %s",CLSS,val1,val2,(reason==null?"ok":comment));
				boolean negate = false;
				if( sharedDictionary.get(TestFrameProperties.CP_NOT)!=null) negate = true;
				if( negate ) outcome = !outcome;
			}
			response = handleAssertionResult(globals,recorder,outcome,comment,reason);
			if(require && !outcome ) return ResponseOption.HALT;
		}
		// Analyze the log message for embedded markups and variable references.
		else if(commandType.equals(CommandType.LOG_MESSAGE)) {
			@SuppressWarnings("unchecked")
			List<String> args = (List<String>)sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
			if( args!=null) {
				StringBuilder msg = new StringBuilder();
				for(String arg:args) {
					arg = expandMarkupsInString(arg,globals);
					if(msg.length()>0) msg.append(" ");
					msg.append(arg);
				}
				//log.debuf("%s = %s",commandName,msg.toString());
				recorder.recordProgress(TestFrameProperties.PROPERTY_LOG_MESSAGE, msg.toString());
			}
		}
		// Notification definition is a static thing by its very nature.
		// If the expression is a Python script, we need to evaluate it here
		else if(commandType.equals(CommandType.NOTIFICATION_DEFINITION)) {
			String name = (String)sharedDictionary.get(TestFrameProperties.CP_NAME);
			String alias = (String)sharedDictionary.get(TestFrameProperties.CP_VALUE);
			Object arg = registry.valueForAlias(alias);
			if( arg==null ) arg = alias;
			registry.setNotificationAlias(name, arg.toString());
		}
		// NOTE: Parser validates value of argument.
		else if(commandType.equals(CommandType.NUMERIC_PARAMETER_SETTER)) {
			Double arg = (Double)sharedDictionary.get(TestFrameProperties.CP_NUMPARAM);
			if( arg!=null) {
				if( commandName.equalsIgnoreCase("timefactor")) {
					globals.put(TestFrameProperties.PROPERTY_TIMESCALE, arg);
					sharedDictionary.put(TestFrameProperties.CP_TIMEFACTOR, arg);
					response = ResponseOption.TIMESCALE;
				}
				else {
					log.warnf("%s.execute: Unrecognized numeric parameter command %s",CLSS,commandName);
				}
			}
		}
		// Assert a result to within a range. For scripts we've waited to evaluate until now (execution time).
		else if(commandType.equals(CommandType.RANGE_ASSERTION)) {
			boolean require = (sharedDictionary.get(TestFrameProperties.CP_REQUIRE_MARKER)!=null);
			String val = (String)sharedDictionary.get(TestFrameProperties.CP_VALUE);
			String opr1 =  (String)sharedDictionary.get(TestFrameProperties.CP_OPERATOR1);
			String opr2 =  (String)sharedDictionary.get(TestFrameProperties.CP_OPERATOR2);
			Double val1 = (Double)sharedDictionary.get(TestFrameProperties.CP_VALUE1);
			Double val2 = (Double)sharedDictionary.get(TestFrameProperties.CP_VALUE2);
			String comment = (String)sharedDictionary.get(TestFrameProperties.CP_COMMENT);
			log.tracef("%s.execute: asserting %s %s %s %s %s",CLSS,val1,opr1,val,opr2,val2);
			String reason = null;
			Object arg = val;  
			if( registry.aliasExists(val)) {
				arg = registry.valueForAlias(val);
				if( arg==null )  reason = NO_VALUE;
			}
			if( arg==null ) arg = val;   // We've gotten a constant as the expression
			
			if( reason==null ) reason = computeComparison(val1,arg,opr1);
			if( reason==null ) reason = computeComparison(arg,val2,opr2);
			boolean outcome = false;
			if( reason==null) {
				outcome = (reason==null);
				log.tracef("%s.execute: assert %s %s %s %s %s is %s",CLSS,val1,opr1,arg,opr2,val2,(reason==null?"ok":comment));
				boolean negate = false;    // Negate the entire expression, not just one clause
				if( sharedDictionary.get(TestFrameProperties.CP_NOT)!=null) negate = true;
				if( negate ) outcome = !outcome;
			}
			response = handleAssertionResult(globals,recorder,outcome,comment,reason);
			if(require && !outcome ) return ResponseOption.HALT;
		}
		// Assert a result. For scripts we've waited to evaluate until now (execution time).
		else if(commandType.equals(CommandType.RELATIONAL_ASSERTION)) {
			boolean require = (sharedDictionary.get(TestFrameProperties.CP_REQUIRE_MARKER)!=null);
			String val1 = (String)sharedDictionary.get(TestFrameProperties.CP_VALUE1);
			String opr =  (String)sharedDictionary.get(TestFrameProperties.CP_OPERATOR);
			String val2 = (String)sharedDictionary.get(TestFrameProperties.CP_VALUE2);
			String comment = (String)sharedDictionary.get(TestFrameProperties.CP_COMMENT);
			String reason = null;
			// If the arguments are aliases, then replace with the lookup
			if( registry.aliasExists(val1.toString())) {
				Object arg = registry.valueForAlias(val1.toString());
				if( arg!=null ) val1 = arg.toString();
				else reason = NO_VALUE;
			}
			if( registry.aliasExists(val2.toString())) {
				Object arg = registry.valueForAlias(val2.toString());
				if( arg!=null ) val2 = arg.toString();
				else reason = NO_VALUE;
			}
			boolean outcome = false;
			if( reason==null ) {
				reason = computeComparison(val1,val2,opr);
				outcome = (reason==null);
				log.debugf("%s.execute: assert %s %s %s is %s",CLSS,val1,opr,val2,(reason==null?"ok":comment));
				boolean negate = false;
				if( sharedDictionary.get(TestFrameProperties.CP_NOT)!=null) negate = true;
				if( negate ) {outcome = !outcome;}
			}
			response = handleAssertionResult(globals,recorder,outcome,comment,reason);
			if(require && !outcome ) return ResponseOption.HALT;
		}
		if(commandType.equals(CommandType.REQUIRE)) {
			// We should never reach this point. The command type should be replaced by an assertion type
			return ResponseOption.HALT;
		}
		else if(commandType.equals(CommandType.RUN)) {
			 /* NOTE: For Windows, specify path as: C:/home/work/method.txt
			  *       For Mac/Linux:    /home/work/method.txt
			  *  We automatically adjust windows path, if specified with backslashes.
			  */
			String path = (String)sharedDictionary.get(TestFrameProperties.CP_PATH);
			// With Windows, we don't have the concept of a current directory. Use the current path.
			String currentPath = sharedDictionary.get(TestFrameProperties.CP_CURRENT_PATH).toString();
			if( currentPath.contains(":")) {   // Test for colon is Windows
				int pos = currentPath.lastIndexOf("/");
				if( pos>=0 ) currentPath = currentPath.substring(0,pos+1);
				path = currentPath+path;
			}
			else {  
				// Get the root path and append the partial path
				String rootPath = (String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_DIRECTORY);
				path = String.format("%s/%s", rootPath,path);
			}
			path=path.replace("\\", "/");
			log.infof("%s.evaluate: run %s", CLSS,path);
			// Create a player for the file and execute "in-line"
			PlayerController pc = new PlayerController(context,recorder);
			pc.setSpeedupFactor(((Double)globals.get(TestFrameProperties.PROPERTY_TIMESCALE)).doubleValue());
			ScriptPlayer inlinePlayer = new ScriptPlayer(context,recorder);
			if( inlinePlayer.initialize(path)) {
				inlinePlayer.setGlobals(globals);
				pc.addPlayer(inlinePlayer);
				pc.play();
			}
			else {	
				recorder.recordError(String.format("In-line script player error reading %s",path));
			}
			if(pc.hasBeenHalted() ) return ResponseOption.HALT;  // In case the runs are nested.
		}
		else if(commandType.equals(CommandType.SCRIPT)) {
			String alias = (String)sharedDictionary.get(TestFrameProperties.CP_NAME);
			@SuppressWarnings("unchecked")
			List<String> args = (List<String>)sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
			try {
				registry.executePython(alias, args);
			}
			catch(Exception ex) {
				String reason = ex.getMessage();
				ex.printStackTrace();
				recorder.recordError(reason);
			}
		}
		else if(commandType.equals(CommandType.SCRIPT_DEFINITION)) {
			String alias = (String)sharedDictionary.get(TestFrameProperties.CP_NAME);
			String path = (String)sharedDictionary.get(TestFrameProperties.CP_PATH);
			@SuppressWarnings("unchecked")
			List<String> args = (List<String>)sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
			// Convert into a comma-delimited string
			StringBuffer argnames = new StringBuffer();
			for( String arg:args ) {
				if( argnames.length()>0) argnames.append(",");
				argnames.append(arg);
			}
			
			String root = (String)sharedDictionary.get(TestFrameProperties.CP_PYLIB_DIRECTORY);
			PythonScript runner = new PythonScript(context,root,path,argnames.toString());
			registry.setScriptAlias(alias, runner);
		}
		else if(commandType.equals(CommandType.SELECT)) {
			String path = (String)sharedDictionary.get(TestFrameProperties.CP_PATH);
			String opt = (String)sharedDictionary.get(TestFrameProperties.CP_NAME);
			registry.pushImmediate(opt, path);
		}
		else if(commandType.equals(CommandType.SHOW)) {
			String path = (String)sharedDictionary.get(TestFrameProperties.CP_PATH);
			String opt = (String)sharedDictionary.get(TestFrameProperties.CP_NAME);
			registry.pushImmediate(opt, path);
		}
		else if(commandType.equals(CommandType.START)) {
			response = ResponseOption.START;
		}
		else if(commandType.equals(CommandType.STOP)) {
			response = ResponseOption.STOP;
		}
		else if(commandType.equals(CommandType.TAG_DEFINITION)) {
			String name = (String)sharedDictionary.get(TestFrameProperties.CP_NAME);
			String path = (String)sharedDictionary.get(TestFrameProperties.CP_VALUE);
			registry.setTagAlias(name, path);
		}
		// Write to the current tagset
		else if(commandType.equals(CommandType.TAG_DATA)) {
			String set = (String)globals.get(TestFrameProperties.CP_CURRENT_TAGSET);
			@SuppressWarnings("unchecked")
			List<String> args = (List<String>)sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
			Iterator<String> argWalker = args.iterator();
			@SuppressWarnings("unchecked")
			Map<String,List<TagPath>> tagmap = (Map<String,List<TagPath>>)globals.get(TestFrameProperties.CP_TAGMAP);
			if( tagmap!=null) {
				List<TagPath> tags = tagmap.get(set);
				if( tags!=null && !tags.isEmpty() ) {
					TagWriter writer = ((TestFrameGatewayHook)(context.getModule(TestFrameProperties.MODULE_ID))).getTagWriter();
					for(TagPath path:tags) {
						if( argWalker.hasNext()) {
							String arg = argWalker.next();
							if(!arg.trim().isEmpty() ) writer.write(path,arg,timestamp);
						}
						else {
							break;     // We're out of data
						}
					}
				}
			}
		}
		else if(commandType.equals(CommandType.TAG_PROVIDER_SETTER)) {
			ProviderRegistry providerRegistry = ((TestFrameGatewayHook) context.getModule(TestFrameProperties.MODULE_ID)).getProviderRegistry();
			String provider = (String)sharedDictionary.get(TestFrameProperties.CP_TEXTPARAM);
			@SuppressWarnings("unchecked")
			Map<String,String> map = (Map<String,String>)sharedDictionary.get(TestFrameProperties.CP_ARGMAP);
			boolean isPrimary = true;
			String mode = ProviderRegistry.PROVIDER_TYPE_CURRENT;
			String db = "";
			String table = "";	
			for(String key:map.keySet()) {
				String arg = map.get(key);
				Object lookup = registry.valueForAlias(arg);
				if( lookup!=null ) arg = lookup.toString();
				if( key.equalsIgnoreCase("mode")) {
					if(mode.equalsIgnoreCase(ProviderRegistry.PROVIDER_TYPE_TEST)) {
						mode = ProviderRegistry.PROVIDER_TYPE_TEST;
					}
					else if(mode.equalsIgnoreCase(ProviderRegistry.PROVIDER_TYPE_HISTORY)) {
						mode = ProviderRegistry.PROVIDER_TYPE_HISTORY;
					}
				}
				else if( key.equalsIgnoreCase("db") || key.equalsIgnoreCase("database") ) {
					db = arg;
				}
				else if( key.equalsIgnoreCase("table")  ) {
					table = arg;
				}
				else if( key.equalsIgnoreCase("primary")  ) {
					isPrimary = arg.equalsIgnoreCase("true");
				}
			}
			
			if(mode.equalsIgnoreCase(ProviderRegistry.PROVIDER_TYPE_CURRENT) ) {
				providerRegistry.removeProvider(provider);
			}
			else if(mode.equalsIgnoreCase(ProviderRegistry.PROVIDER_TYPE_TEST) ) {
				providerRegistry.defineTestProvider(provider);
			}
			else if(mode.equalsIgnoreCase(ProviderRegistry.PROVIDER_TYPE_HISTORY) ) {
				providerRegistry.addDatabaseStorageDelegate(new DatabaseStoringProviderDelegate(context,provider,db,table));
			}
			if( isPrimary ) registry.setProvider(provider);
		}
		// Define a tag set. Place in the map of tag sets keyed by its name
		else if(commandType.equals(CommandType.TAG_SET_DEFINITION)) {
			String name = (String)sharedDictionary.get(TestFrameProperties.CP_NAME);
			globals.put(TestFrameProperties.CP_CURRENT_TAGSET, name);
			@SuppressWarnings("unchecked")
			List<String> tags = (List<String>)sharedDictionary.get(TestFrameProperties.CP_TAGLIST);
			if( tags!=null && !tags.isEmpty() ) {
				@SuppressWarnings("unchecked")
				Map<String,List<TagPath>> tagmap = (Map<String,List<TagPath>>)globals.get(TestFrameProperties.CP_TAGMAP);
				if( tagmap==null ) {
					tagmap = new HashMap<>();
					globals.put(TestFrameProperties.CP_TAGMAP, tagmap);
				}
				List<TagPath> tagpaths = new ArrayList<>();
				for(String tagpath:tags ) {
					if( tagpath.length()==0) continue;
					// The translator has resolved any variable evaluations.
					// All we have in the list are the actual tag paths or aliases.
					Object arg = registry.pathForAlias(tagpath);
					if( arg!=null ) {
						tagpath = arg.toString();
					}
					else {
						// Path was provided directly, not an alias
						String provider = registry.getProvider();
						if( tagpath.indexOf("[")<0) tagpath = String.format("[%s]%s",provider,tagpath);
					}
					TagPath path = TagPathParser.parseSafe(tagpath);
					if( path!=null ) {
						tagpaths.add(path);
					}
					else {
						log.warnf("%s.execute: %s could not parse tag path %s", CLSS,commandType.name(),tagpath);
					}
				}
				tagmap.put(name, tagpaths);
			}
		}
		else if(commandType.equals(CommandType.TAG_SET_SELECTION)) {
			String name = (String)sharedDictionary.get(TestFrameProperties.CP_NAME);
			globals.put(TestFrameProperties.CP_CURRENT_TAGSET, name);
		}
		else if(commandType.equals(CommandType.TEXT_PARAMETER_SETTER)) {
			String arg = (String)sharedDictionary.get(TestFrameProperties.CP_TEXTPARAM);
			arg = expandMarkupsInString(arg,globals);
			if( arg!=null) {
				if( commandName.equalsIgnoreCase("reset") &&
					arg.equalsIgnoreCase("metrics")) {
					response = ResponseOption.RESET_METRICS;
				}
				else if( commandName.equalsIgnoreCase("step")) {
					globals.put(TestFrameProperties.PROPERTY_TEST_STEP, arg);
					recorder.recordProgress(TestFrameProperties.PROPERTY_TEST_STEP, arg);
				}
				else if( commandName.equalsIgnoreCase("status")) {
					recorder.recordProgress(TestFrameProperties.PROPERTY_STATUS_MESSAGE, arg);
				}
				else if( commandName.equalsIgnoreCase("test")) {
					globals.put(TestFrameProperties.PROPERTY_TEST_NAME, arg);
					recorder.recordProgress(TestFrameProperties.PROPERTY_TEST_NAME, arg);
				}
				else {
					log.warnf("%s.execute: Unrecognized text parameter command %s",CLSS,commandName);
				}
			}
		}
		// NOTE: Parser validates value of argument. Value in dictionary is a Long milli-secs.
		else if(commandType.equals(CommandType.TIME_PARAMETER_SETTER)) {
			Long arg = (Long)sharedDictionary.get(TestFrameProperties.CP_VALUE);
			if( arg!=null) {
				if( commandName.equalsIgnoreCase("poll")) {
					globals.put(TestFrameProperties.PROPERTY_DEFAULT_POLL_INTERVAL, arg);
					sharedDictionary.put(TestFrameProperties.CP_DEFAULT_POLL_INTERVAL, arg);
				}
				else if( commandName.equalsIgnoreCase("timeout")) {
					globals.put(TestFrameProperties.PROPERTY_DEFAULT_TIMEOUT, arg);
					sharedDictionary.put(TestFrameProperties.CP_DEFAULT_TIMEOUT, arg);
				}
				else {
					log.warnf("%s.execute: Unrecognized time parameter command %s",CLSS,commandName);
				}
			}
		}
		// This is also valid for a tag - resulting in a write.
		else if(commandType.equals(CommandType.VARIABLE_DEFINITION)) {
			String alias = (String)sharedDictionary.get(TestFrameProperties.CP_NAME);
			
			String arg = (String)sharedDictionary.get(TestFrameProperties.CP_VALUE);
			// The argument can itself be an alias.
			Object lookup = registry.valueForAlias(arg);
			if( lookup!=null ) {
				arg = lookup.toString();
			}
			else {
				// The argument can be an expression
				arg = expandVariablesInString(arg,globals);
			}
			int type = registry.getAliasType(alias);
			if( type==AliasRegistry.CONSTANT_TYPE) {
				registry.setVariableAlias(alias, arg);
			}
			else if( type==AliasRegistry.TAG_TYPE) {
				TagWriter writer = ((TestFrameGatewayHook)(context.getModule(TestFrameProperties.MODULE_ID))).getTagWriter();
				if( timestamp==null ) {
					writer.write(registry.pathForAlias(alias),arg,System.currentTimeMillis());
				}
				else {
					writer.write(registry.pathForAlias(alias),arg,timestamp.getTime());
				}
			}
			else {
				recorder.recordError(String.format("Alias %s is neither a constant nor a tag. It cannot be assigned.",alias));
			}
			
		}
		return response;
	}
	// @return True if the first argument is a list and contains the second
	private String computeContains(Object arg1,Object arg2) {
		boolean result = false; 
		if( arg1==null||arg2==null) return "Error: missing argument in contains";
		
		String reason = null;
		if( arg1 instanceof List ) {
			List<?> arglist = (List<?>) arg1;
			if( arglist.contains(arg2) ) {
				result = true;
			}
			else {
				reason = String.format("%s not in list",arg2.toString());
			}
		}
		else {
			reason = String.format("First argument not a list (%s)",arg1.getClass().getName());
			log.warnf("%s.computeContains: %s",CLSS,reason);
		}
			
		if(!result && reason==null) {
			reason = String.format("Assert %s contains %s",arg1.toString(),arg2.toString());
		}
		
		return reason;
	}
	// @return True if the first argument is a list and contains the second
	private String compareListSize(Object arg1,Object arg2,String opr) {
		if( arg1==null||arg2==null) return "Error: missing argument in compare list size";

		String reason = null;
		if( arg1 instanceof List ) {
			List<?> arglist = (List<?>) arg1;
			int count = arglist.size();
			reason = computeComparison(new Integer(count),arg2,opr);
		}
		else {
			reason = String.format("First argument not a list (%s)",arg1.getClass().getName());
			log.warnf("%s.compareListSize: %s",CLSS,reason);
		}
		return reason;
	}
	private String computeComparison(Object arg1,Object arg2,String opr) {
		boolean result = false; 
		if( arg1==null||arg2==null||opr==null) return "Error: missing argument or operator in comparison";
		
		String reason = null;
		if( opr.equalsIgnoreCase("=")) {
			// First try numeric comparison
			try {
				double db1 = Double.parseDouble(arg1.toString());
				double db2 = Double.parseDouble(arg2.toString());
				double tolerance = ((db1<0.?-db1:db1)+(db2<0.?-db2:db2))*TOLERANCE;
				
				if( db1-db2 <= tolerance && db2-db1 <= tolerance ) result = true;
			}
			catch(NumberFormatException nfe) {
				// Not numeric, so do a string comparison (case insensitive)
				if(arg1.toString().equalsIgnoreCase(arg2.toString())) result = true;
			}
		}
		else if( opr.equalsIgnoreCase("!=")) {
			// First try numeric comparison
			try {
				double db1 = Double.parseDouble(arg1.toString());
				double db2 = Double.parseDouble(arg2.toString());
				double tolerance = ((db1<0.?-db1:db1)+(db2<0.?-db2:db2))*TOLERANCE;

				if( db1-db2 > tolerance || db2-db1 > tolerance ) result = true;
			}
			catch(NumberFormatException nfe) {
				log.debugf("%s.computeComparison: Either of %s or %s is not a number",CLSS, arg1.toString(),arg2.toString());
				// Not numeric, so do a string comparison
				if(!arg1.toString().equalsIgnoreCase(arg2.toString())) result = true;
			}
		}
		else {
			// Inequalities require doubles
			try {
				double db1 = Double.parseDouble(arg1.toString());
				double db2 = Double.parseDouble(arg2.toString());
				if( opr.equalsIgnoreCase(">")) {
					if( db1 > db2 ) result = true;
				}
				else if( opr.equalsIgnoreCase(">=")) {
					if( db1 >= db2 ) result = true;
				}
				else if( opr.equalsIgnoreCase("<")) {
					if( db1 < db2 ) result = true;
				}
				else if( opr.equalsIgnoreCase("<=")) {
					if( db1 <= db2 ) result = true;
				}
				else {
					log.warnf("%s.computeComparison: Unknown operator (%s)",CLSS, opr);
					reason = String.format("Unknown operator (%s)",opr);
				}
			}
			catch(NumberFormatException nfe) {
				log.warnf("%s.computeComparison: Either of %s or %s is not a number",CLSS, arg1.toString(),arg2.toString());
				reason = String.format("Either of %s or %s is not a number",arg1.toString(),arg2.toString());
			}
			
		}
		if(!result && reason==null) {
			reason = String.format("%s %s %s is false",arg1.toString(),opr,arg2.toString());
		}
		
		return reason;
	}
	/*
	 * If the argument refers to a built-in variable, return its value,
	 * otherwise return the original value unchanged. This is used for both 
	 * variables and [...] markups. Do a case-insensitive comparison.
	 */
	private String expandBuiltin(String markup,Map<String,Object> globals) {
		String arg = markup;
		if(markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_AVECPU)) {
			try {
				double val = ((Double)globals.get(TestFrameProperties.BUILTIN_AVECPU)).doubleValue();
				arg = String.format("%3.2f",val);
			}
			catch(NumberFormatException nfe) {
				log.warnf("%s.builtin: AVECPU not a double (%s)", CLSS,nfe.getMessage());
			}
		}
		else if( markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_AVEMEM)) {
			try {
				double val = ((Double)globals.get(TestFrameProperties.BUILTIN_AVEMEM)).doubleValue();
				arg = String.format("%5.0f",val/1000000);
			}
			catch(NumberFormatException nfe) {
				log.warnf("%s.builtin: AVEMEM not a double (%s)", CLSS,nfe.getMessage());
			}
		}
		else if( markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_CPU)) {
			try {
				double val = ((Double)globals.get(TestFrameProperties.BUILTIN_CPU)).doubleValue();
				arg = String.format("%3.2f",val);
			}
			catch(NumberFormatException nfe) {
				log.warnf("%s.builtin: CPU not a double (%s)", CLSS,nfe.getMessage());
			}
		}
		else if( markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_DATE)) {  arg = dateFormat.format(new Date()); }
		else if( markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_MEM))  {
			try {
				double val = ((Long)globals.get(TestFrameProperties.BUILTIN_MEM)).longValue();
				arg = String.format("%5.0f",val/1000000);
			}
			catch(NumberFormatException nfe) {
				log.warnf("%s.builtin: MEM not an integer (%s)", CLSS,nfe.getMessage());
			}
		}
		else if(markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_PROJECT)){ arg = (String)registry.valueForAlias(TestFrameProperties.BUILTIN_PROJECT); }
		else if(markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_TIME))  { arg = timeFormat.format(new Date()); }
		else if(markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_TDATE)) { arg = (String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_DATE);}
		else if( markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_TTIME)){ arg = (String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_TIME);}
		else if(markup.equalsIgnoreCase(TestFrameProperties.BUILTIN_USER))  { arg = (String)registry.valueForAlias(TestFrameProperties.BUILTIN_USER); }
		else {
			// Simply return the original argument, including the brackets
			arg = "["+markup+"]";
		}
		return arg.trim();
	}

	
	// The incoming text has, potentially, embedded variables or bracketed markups. 
	// Evaluate them and return the expanded string. The variable name delimiter is 
	// assumed to be a space. Any outer quotes have been removed.
	private String expandMarkupsInString(String intext,Map<String,Object> globals) {
		if( intext==null ) return null;
		// First we handle any bracketed system variables.
		StringBuilder builder = new StringBuilder();
		int pos1 = intext.indexOf("[");
		int pos2 = intext.indexOf("]");
		if( pos1<0 || pos1>pos2 ) builder.append(intext);
		while( pos1>=0 && pos2>pos1 ) {
			if(pos1>0) builder.append(intext.substring(0,pos1));
			String arg = intext.substring(pos1+1,pos2);
			if( arg.length()>0 ) {
				builder.append(expandBuiltin(arg,globals));
			}
			if( pos2<intext.length()-1) {
				intext = intext.substring(pos2+1);
				pos1 = intext.indexOf("[");
				pos2 = intext.indexOf("]");
			}
			else {
				pos1 = -1;
			}
		}
		if( pos2>0 && pos2<intext.length()-1) {
			intext = intext.substring(pos2+1);
			builder.append(intext);
		}
		// Now look for embedded variables
		return expandVariablesInString(builder.toString(),globals);
	}
	
	// The incoming text has, potentially, embedded variables. Evaluate them.
	// The variable name delimiter is assumed to be a space. Here it is OK to 
	// evaluate the AliasRegistry at parse-time.
	private String expandVariablesInString(String intext,Map<String,Object> globals) {
		StringBuilder result = new StringBuilder();
		String[] components = intext.split("[$]");
		int index = 0;
		for( String component:components ) {
			if( index==0 ) result.append(component);
			else {
				String name = component;
				// We want the termination character that yields the shortest string
				int pos = variableLength(name);
				if( pos>0 ) {
					name = name.substring(0, pos);
					component = component.substring(pos);  // Remainder
				}
				else {
					component="";
				}
				Object val = registry.valueForAlias(name);
				if( val!=null ) {
					result.append(val.toString());
				}
				else {
					// the variable may be a system name
					name = expandBuiltin(name,globals);
					result.append(name);
				}
				result.append(component);
			}
			index++;
		}
		return result.toString();
	}
	/**
	 * Check the shared dictionary for errors logged while parsing. If we find a parse
	 * error, log a failure for the current step. Clear the error entries in the dictionary.
	 * @param globals the global dictionary
	 * @param recorder
	 * @return true if we've found (and handled) an error.
	 */
	private boolean handleError(Map<String,Object> globals,long lineno,ProgressRecorder recorder) {
		boolean hasError = false;
		String msg = (String)sharedDictionary.get(TestFrameProperties.EXPR_ERR_MESSAGE);
		if( msg!=null && msg.length()>0 ) {
			hasError = true;
			String file = "script";
			String path = (String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_PATH);
			if( path!=null ) {
				int pos = path.lastIndexOf("/");
				file = path;
				if( pos>0 && pos+1<file.length()) file = path.substring(pos+1);
			}
			msg = String.format("%s, line %d: %s", file,lineno,msg);
			recorder.recordAssertion((String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_DATE), 
					                 (String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_TIME), 
					                 (String)globals.get(TestFrameProperties.PROPERTY_TEST_NAME), 
					                 (String)globals.get(TestFrameProperties.PROPERTY_TEST_STEP), false, msg);
			sharedDictionary.put(TestFrameProperties.EXPR_ERR_MESSAGE,"");  // Clear message
		}
		return hasError;
	}
	/**
	 * Determine whether to continue looping, report a timeout or the normal assertion result. 
	 */
	private ResponseOption handleAssertionResult(Map<String,Object> globals,ProgressRecorder recorder,boolean outcome,String comment,String reason) {
		ResponseOption response = ResponseOption.CONTINUE;
		int count = 0;
		Integer val = (Integer)sharedDictionary.get(TestFrameProperties.CP_COUNT);
		if( val!=null ) count = val.intValue();
		
		log.debugf("%s.handleAssertionResult: %s, count=%d",CLSS,(outcome?"TRUE":"FALSE"),count);
		// If the outcome is positive or we're not looping, report and continue.
		if( outcome || count==0 ) {
			comment = expandMarkupsInString(comment,globals);
			if(!outcome) comment = String.format("%s(%s)",comment,reason);
			recorder.recordAssertion((String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_DATE), 
					(String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_TIME), 
					(String)globals.get(TestFrameProperties.PROPERTY_TEST_NAME), 
					(String)globals.get(TestFrameProperties.PROPERTY_TEST_STEP), outcome, comment);
		}
		// Timeout 
		else if( count==1 ) {
			comment = expandMarkupsInString(comment,globals);
			if(!outcome) comment = String.format("%s(%s)",comment,"TIMEOUT");
			recorder.recordAssertion((String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_DATE), 
					(String)sharedDictionary.get(TestFrameProperties.CP_CURRENT_TIME), 
					(String)globals.get(TestFrameProperties.PROPERTY_TEST_NAME), 
					(String)globals.get(TestFrameProperties.PROPERTY_TEST_STEP), false, comment);
		}
		// Still looping
		else {
			response = ResponseOption.REPEAT;
			sharedDictionary.put(TestFrameProperties.CP_COUNT,new Integer(count-1));
		}
		return response;
	}
	/**
	 * Find the length of the variable name that begins the text
	 * string. Of all the possible terminators, choose the one
	 * that produces the shortest name. None of the terminators
	 * are legal characters in a variable name.
	 * @param text
	 * @return name length or -1 if no terminators were found.
	 */
	private int variableLength(String text) {
		int pos1 = text.indexOf("\'");
		int pos2 = text.indexOf(" ");  // SPACE
		if(pos2>=0 && (pos2<pos1||pos1<0)) pos1 = pos2;;
		pos2 = text.indexOf("	"); // TAB
		if(pos2>=0 && (pos2<pos1||pos1<0)) pos1 = pos2;
		pos2 = text.indexOf(",");
		if(pos2>=0 && (pos2<pos1||pos1<0)) pos1 = pos2;
		pos2 = text.indexOf("}");
		if(pos2>=0 && (pos2<pos1||pos1<0)) pos1 = pos2;
		pos2 = text.indexOf(")");
		if(pos2>=0 && (pos2<pos1||pos1<0)) pos1 = pos2;
		pos2 = text.indexOf("]");
		if(pos2>=0 && (pos2<pos1||pos1<0)) pos1 = pos2;
		
		return pos1;
	}
	@Override
	public String toString() {
		return String.format("CommandPrototype: %s",commandName);
	}
}
