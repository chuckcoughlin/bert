/**
 *   (c) 2014-2015  ILS Automation. All rights reserved.
 */
package com.ils.tf.gateway.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ils.tf.common.TestFrameProperties;
import com.ils.tf.gateway.expression.ScriptSyntaxBaseVisitor;
import com.ils.tf.gateway.expression.ScriptSyntaxParser;
import com.ils.tf.gateway.python.InlineScript;
import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;


/**
 *  This translator handles the individual command expressions as known
 *  to the ANTLR parser. There is a separate instance of the translator
 *  for each line of script.
 */
public class CommandTranslator extends ScriptSyntaxBaseVisitor<Object>  {
	private static final String TAG = "CommandTranslator";
	private LoggerEx log;
	private final AliasRegistry registry = AliasRegistry.getInstance();
	private final CommandPrototype prototype;
	private final HashMap<String,Object> sharedDictionary;
	private long tempCounter = 0;
	
	/**
	 * Constructor.
	 * @param prototype the CommandPrototype that we are constructing
	 * @param shared is the parameter dictionary known to the prototype
	 */
	public CommandTranslator(CommandPrototype proto,HashMap<String,Object> shared) {
		log = LogUtil.getLogger(getClass().getPackage().getName());
		this.prototype = proto;
		this.sharedDictionary = shared;
		// Set by the REQUIRE command to denote that we need to abort on an asseertion failure
		shared.remove(TestFrameProperties.CP_REQUIRE_MARKER); 
		// Each command must replace this if there is a timestamp
		shared.remove(TestFrameProperties.CP_TIMESTAMP_MARKER);
	}
	
	// ================================= Overridden Methods =====================================
	// These do the actual translations
	@Override 
	public Object visitAssertionExpression(ScriptSyntaxParser.AssertionExpressionContext ctx) { 
		prototype.setCommandName("Assertion");
		if( ctx.timeexpr()!=null ) visit(ctx.timeexpr());
		visit(ctx.assertion());
		return null;
	}
	@Override 
	public Object visitBooleanExpression(ScriptSyntaxParser.BooleanExpressionContext ctx) { 
		sharedDictionary.put(TestFrameProperties.CP_VALUE, ctx.BOOL().getText());
		return null;
	}

	@Override 
	public Object visitDateTimeExpression(ScriptSyntaxParser.DateTimeExpressionContext ctx) { 
		String date = ctx.DATE().getText();   // yyyy/MM/dd
		sharedDictionary.put(TestFrameProperties.CP_CURRENT_DATE, date);
		String time = ctx.TIME().getText();   // HH:mm:ss
		sharedDictionary.put(TestFrameProperties.CP_CURRENT_TIME, scrubTime(time));
		sharedDictionary.put(TestFrameProperties.CP_TIMESTAMP_MARKER, "true");
		return null;
	}
	@Override 
	public Object visitHaltCommand(ScriptSyntaxParser.HaltCommandContext ctx) { 
		prototype.setCommandName("HaltCommand");
		prototype.setCommandType(CommandType.HALT);
		return null; 
	}
	@Override 
	public Object visitLogComponentMarkup(ScriptSyntaxParser.LogComponentMarkupContext ctx) { 
		@SuppressWarnings("unchecked")
		List<String> arglist = (List<String>) sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
		String markup = ctx.Markup().getText();
		arglist.add(markup);
		return null; 
	}
	@Override 
	public Object visitLogComponentText(ScriptSyntaxParser.LogComponentTextContext ctx) { 
		if( ctx.TEXT()!=null ) {
			@SuppressWarnings("unchecked")
			List<String> arglist = (List<String>) sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
			arglist.add(ctx.TEXT().getText());
		}
		return null; 
	}
	// This is one of the few cases where it is valid to evaluate the AliasRegistry at parse time.
	@Override 
	public Object visitLogComponentVariable(ScriptSyntaxParser.LogComponentVariableContext ctx) { 
		String name = ctx.VARNAME().getText().substring(1);  // Strip $
		Object val = registry.valueForAlias(name);
		if( val!=null ) {
			@SuppressWarnings("unchecked")
			List<String> arglist = (List<String>) sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
			arglist.add(val.toString());
		}
		else {
			recordError(ctx.VARNAME().getText()," is undefined","");
		}
		return null;
	}
	@Override 
	public Object visitLogComponentString(ScriptSyntaxParser.LogComponentStringContext ctx) { 
		@SuppressWarnings("unchecked")
		List<String> arglist = (List<String>) sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
		String logtext = stripQuotes(ctx.STRING().getText());
		arglist.add(logtext);
		return null; 
	}
	@Override 
	public Object visitLogComponentWord(ScriptSyntaxParser.LogComponentWordContext ctx) { 
		@SuppressWarnings("unchecked")
		List<String> arglist = (List<String>) sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
		arglist.add(ctx.word().getText());
		return null; 
	}
	@Override 
	public Object visitContainerAssertion(ScriptSyntaxParser.ContainerAssertionContext ctx) { 
		prototype.setCommandType(CommandType.LIST_ASSERTION);
		visit(ctx.expr(0));   // Result is CP_VALUE - move it
		sharedDictionary.put(TestFrameProperties.CP_VALUE1, sharedDictionary.get(TestFrameProperties.CP_VALUE));
		if( ctx.Not()!=null ) {
			sharedDictionary.put(TestFrameProperties.CP_NOT, Boolean.TRUE);
		}
		if( ctx.STRING()!=null ) {
			sharedDictionary.put(TestFrameProperties.CP_COMMENT, stripQuotes(ctx.STRING().getText()));
		}
		else {
			sharedDictionary.put(TestFrameProperties.CP_COMMENT, "");
		}
		
		visit(ctx.expr(1));   // Result is CP_VALUE - move it
		sharedDictionary.put(TestFrameProperties.CP_VALUE2, sharedDictionary.get(TestFrameProperties.CP_VALUE));
		return null; 
	}
	@Override 
	public Object visitContainerSizeAssertion(ScriptSyntaxParser.ContainerSizeAssertionContext ctx) { 
		prototype.setCommandType(CommandType.LIST_SIZE_ASSERTION);
		visit(ctx.expr(0));   // Result is CP_VALUE1 - move it
		sharedDictionary.put(TestFrameProperties.CP_VALUE1, sharedDictionary.get(TestFrameProperties.CP_VALUE));
		if( ctx.Not()!=null ) {
			sharedDictionary.put(TestFrameProperties.CP_NOT, Boolean.TRUE);
		}
		if( ctx.STRING()!=null ) {
			sharedDictionary.put(TestFrameProperties.CP_COMMENT, stripQuotes(ctx.STRING().getText()));
		}
		else {
			sharedDictionary.put(TestFrameProperties.CP_COMMENT, "");
		}
		if( ctx.ROPR()!=null ) {
			String op = ctx.ROPR().getText();
			sharedDictionary.put(TestFrameProperties.CP_OPERATOR, op);
		}
		visit(ctx.expr(1));   // Result is CP_VALUE - move it
		sharedDictionary.put(TestFrameProperties.CP_VALUE2, sharedDictionary.get(TestFrameProperties.CP_VALUE));
		return null; 
	}
	// A new line to be logged
	@Override 
	public Object visitLogger(ScriptSyntaxParser.LoggerContext ctx) { 
		prototype.setCommandType(CommandType.LOG_MESSAGE);
		prototype.setCommandName("Log");
		if( ctx.timeexpr()!=null ) visit(ctx.timeexpr());
		List<String> arglist = new ArrayList<String>();
		sharedDictionary.put(TestFrameProperties.CP_ARGLIST,arglist);
		int argcount = ctx.logcomponent().size();
		//log.tracef("%s.visitLogger: %d components",TAG,argcount);
		int index = 0;
		while( index<argcount) {
			visit(ctx.logcomponent(index));
			index++;
		}
		return null; 
	}
	// Store a mixed path list in CP_ARGLIST. The parent command must clear the list.
	// Elements are names, strings or variables. We evaluate the variables here.
	@Override 
	public Object visitMixedPathExpression(ScriptSyntaxParser.MixedPathExpressionContext ctx) { 
		@SuppressWarnings("unchecked")
		List<String>list = (List<String>)sharedDictionary.get(TestFrameProperties.CP_ARGLIST); 
		if( ctx.NAME()!=null )      list.add(ctx.NAME().getText());
		else if(ctx.STRING()!=null )   list.add(stripQuotes(ctx.STRING().getText()));
		else if(ctx.VARNAME()!=null ) {
			String name = ctx.VARNAME().getText().substring(1);  // Strip $
			Object val = registry.valueForAlias(name);
			if( val!=null ) {
				list.add(val.toString());
			}
			else {
				list.add("NO_VALUE("+name+")");
				recordError(ctx.VARNAME().getText()," is undefined","");
			}
		}
		//log.infof("%s.visitMixedPathExpression: got %s",TAG,ctx.NAME().getText());
		return null;
	}
	// Append a mixed path list in CP_ARGLIST. The parent command must clear the list.
	// Elements are names, strings or variables. We evaluate the variables here.
	@Override 
	public Object visitMixedPathRecursive(ScriptSyntaxParser.MixedPathRecursiveContext ctx) { 
		@SuppressWarnings("unchecked")
		List<String>list = (List<String>)sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
		if(ctx.mixedpath()!=null) visit(ctx.mixedpath());
		if( ctx.NAME()!=null )      list.add(ctx.NAME().getText());
		else if(ctx.STRING()!=null ) list.add(stripQuotes(ctx.STRING().getText()));
		else if(ctx.VARNAME()!=null ) {
			String name = ctx.VARNAME().getText().substring(1);  // Strip $
			Object val = registry.valueForAlias(name);
			if( val!=null ) {
				list.add(val.toString());
			}
			else {
				list.add("NO_VALUE("+name+")");
				recordError(ctx.VARNAME().getText()," is undefined","");
			}
		}
		//log.infof("%s.visitMixedPathRecursive: got %s",TAG,ctx.NAME().getText());
		return null;
	}
	@Override 
	public Object visitNameExpression(ScriptSyntaxParser.NameExpressionContext ctx) { 
		sharedDictionary.put(TestFrameProperties.CP_VALUE, ctx.NAME().getText());
		return null;
	}
	// Store a variable name-value list in CP_ARGMAP
	@SuppressWarnings("unchecked")
	@Override 
	public Object visitNameValueExpression(ScriptSyntaxParser.NameValueExpressionContext ctx) { 
		Map<String,String>map = (Map<String,String>)sharedDictionary.get(TestFrameProperties.CP_ARGMAP); 
		if( !ctx.ROPR().getText().equals("=")) {
			recordError(ctx.ROPR().getText()," equality is the only allowed operator","");
		}
		else {
			String name = "";
			String value = "";
			name = ctx.NAME().getText().toLowerCase();
			if(ctx.word()!=null) {
				value = ctx.word().getText();
			}
			else if(ctx.STRING()!=null) {
				value = stripQuotes(ctx.STRING().getText());
			}
			map.put(name, value);
		}
		return null;
	}
	// Store a variable argument list in CP_ARGMAP. It is OK to evaluate variable
	// substitutions here. The arg name will always be lower case.
	@SuppressWarnings("unchecked")
	@Override 
	public Object visitNameValueRecursive(ScriptSyntaxParser.NameValueRecursiveContext ctx) { 
		Map<String,String>map = (Map<String,String>)sharedDictionary.get(TestFrameProperties.CP_ARGMAP); 
		if( !ctx.ROPR().getText().equals("=")) {
			recordError(ctx.ROPR().getText()," equality is the only allowed operator","");
		}
		else {
			String name = "";
			String value = "";
			name = ctx.NAME().getText().toLowerCase();
			if(ctx.word()!=null) {
				value = ctx.word().getText();
			}
			else if(ctx.STRING()!=null) {
				value = stripQuotes(ctx.STRING().getText());
			}
			map.put(name, value);
		}
		return null;
	}
	@Override 
	public Object visitNotificationDefinition(ScriptSyntaxParser.NotificationDefinitionContext ctx) { 
		prototype.setCommandType(CommandType.NOTIFICATION_DEFINITION);
		sharedDictionary.put(TestFrameProperties.CP_NAME, ctx.NAME().getText());
		visit(ctx.expr());  // Sets CP_VALUE
		return null;
	}
	@Override 
	public Object visitNumericExpression(ScriptSyntaxParser.NumericExpressionContext ctx) { 
		sharedDictionary.put(TestFrameProperties.CP_VALUE, ctx.numval().getText());
		return null;
	}
	@Override 
	public Object visitNumericPropertySetter(ScriptSyntaxParser.NumericPropertySetterContext ctx) { 
		prototype.setCommandType(CommandType.NUMERIC_PARAMETER_SETTER);
		prototype.setCommandName(toTitleCase(ctx.NumSetter().getText()));
		if( ctx.numval()!=null ) {
			// Note: The parser should guarantee a valid number. Do some checks here for valid range.
			double val = Double.parseDouble(ctx.numval().getText());
			if( val> 0.0 ) {
				sharedDictionary.put(TestFrameProperties.CP_NUMPARAM, new Double(val));
			}
			else {
				recordError(ctx.NumSetter().getText()," must be positive","");
			}
		}
		else if( ctx.expr()!=null ) {
			visit(ctx.expr());
			String text = (String)sharedDictionary.get(TestFrameProperties.CP_VALUE);
			// Evaluate variable immediately
			String str = registry.pathForAlias(text);
			if( str!=null ) text = str;
			double val = Double.parseDouble(text);
			if( val> 0.0 ) {
				sharedDictionary.put(TestFrameProperties.CP_NUMPARAM, new Double(val));
			}
			else {
				recordError(ctx.NumSetter().getText()," must be positive","");
			}
		}
		return null; 
	}
	@Override 
	public Object visitRangeAssertion(ScriptSyntaxParser.RangeAssertionContext ctx) { 
		prototype.setCommandType(CommandType.RANGE_ASSERTION);
		visit(ctx.expr());   // Result is CP_VALUE
		if( ctx.ROPR()!=null && ctx.ROPR().size()==2) {
			String op1 = ctx.ROPR(0).getText();
			String op2 = ctx.ROPR(1).getText();
			sharedDictionary.put(TestFrameProperties.CP_OPERATOR1, op1);
			sharedDictionary.put(TestFrameProperties.CP_OPERATOR2, op2);
		}
		if( ctx.Not()!=null ) {
			sharedDictionary.put(TestFrameProperties.CP_NOT, Boolean.TRUE);
		}
		if( ctx.STRING()!=null ) {
			sharedDictionary.put(TestFrameProperties.CP_COMMENT, stripQuotes(ctx.STRING().getText()));
		}
		else {
			sharedDictionary.put(TestFrameProperties.CP_COMMENT, "");
		}
		if( ctx.numval()!=null && ctx.numval().size()==2) {
			double val1 = Double.parseDouble(ctx.numval(0).getText());
			double val2 = Double.parseDouble(ctx.numval(1).getText());
			sharedDictionary.put(TestFrameProperties.CP_VALUE1, new Double(val1));
			sharedDictionary.put(TestFrameProperties.CP_VALUE2, new Double(val2));
		}
		return null; 
	}
	@Override 
	public Object visitRelationalAssertion(ScriptSyntaxParser.RelationalAssertionContext ctx) { 
		prototype.setCommandType(CommandType.RELATIONAL_ASSERTION);
		if( ctx.expr().size()==2 )  {
			visit(ctx.expr(0));
			// Result is in VALUE - move it
			sharedDictionary.put(TestFrameProperties.CP_VALUE1, sharedDictionary.get(TestFrameProperties.CP_VALUE));
			
			if( ctx.ROPR()!=null ) {
				String op = ctx.ROPR().getText();
				sharedDictionary.put(TestFrameProperties.CP_OPERATOR, op);
			}
			if( ctx.Not()!=null ) {
				sharedDictionary.put(TestFrameProperties.CP_NOT, Boolean.TRUE);
			}
	
			visit(ctx.expr(1));
			// Result is in VALUE - move it
			sharedDictionary.put(TestFrameProperties.CP_VALUE2, sharedDictionary.get(TestFrameProperties.CP_VALUE));

			if( ctx.STRING()!=null) {
				sharedDictionary.put(TestFrameProperties.CP_COMMENT, stripQuotes(ctx.STRING().getText()));
			}
			else {
				sharedDictionary.put(TestFrameProperties.CP_COMMENT, "");
			}
		}
		return null; 
	}
	@Override 
	public Object visitRequireExpression(ScriptSyntaxParser.RequireExpressionContext ctx) { 
		prototype.setCommandName("RequireCommand");
		prototype.setCommandType(CommandType.REQUIRE);  // Gets replaced by the assertion types
		if( ctx.timeexpr()!=null ) visit(ctx.timeexpr());
		visit(ctx.assertion());
		sharedDictionary.put(TestFrameProperties.CP_REQUIRE_MARKER, "require");
		return null; 
	}
	@Override 
	public Object visitRunCommand(ScriptSyntaxParser.RunCommandContext ctx) { 
		prototype.setCommandName("RunCommand");
		prototype.setCommandType(CommandType.RUN);
		if( ctx.NAME()!=null) {
			sharedDictionary.put(TestFrameProperties.CP_PATH,ctx.NAME().getText());
		}
		else if(ctx.STRING()!=null) {
			sharedDictionary.put(TestFrameProperties.CP_PATH,stripQuotes(ctx.STRING().getText()));
		}
		
		return null; 
	}
	@Override 
	public Object visitScriptDefinition(ScriptSyntaxParser.ScriptDefinitionContext ctx) { 
		prototype.setCommandType(CommandType.SCRIPT_DEFINITION);
		sharedDictionary.put(TestFrameProperties.CP_NAME, ctx.NAME(0).getText());
		sharedDictionary.put(TestFrameProperties.CP_PATH, ctx.NAME(1).getText());
		sharedDictionary.put(TestFrameProperties.CP_ARGLIST,new ArrayList<String>());  // Clear arglist
		if(ctx.vararg()!=null) visit(ctx.vararg());
		return null;
	}
	
	@Override 
	public Object visitScriptCommand(ScriptSyntaxParser.ScriptCommandContext ctx) { 
		prototype.setCommandType(CommandType.SCRIPT);
		prototype.setCommandName("ScriptCommand");
		visit(ctx.timeexpr());
		String command = ctx.NAME().getText();  
		sharedDictionary.put(TestFrameProperties.CP_NAME, command);
		sharedDictionary.put(TestFrameProperties.CP_ARGLIST,new ArrayList<String>());  // Clear arglist
		visit(ctx.vararg());
		return null;
	}
	// Compile the script and place result as CP_VALUE. We wait to execute until any timer
	// expires.
	@Override 
	public Object visitScriptExpression(ScriptSyntaxParser.ScriptExpressionContext ctx) { 
		String name = ctx.NAME().getText(); 
		// If the script is an existing alias, get the full module path
		String modulePath = registry.pathForAlias(name);
		if( modulePath == null ) modulePath = name;
		sharedDictionary.put(TestFrameProperties.CP_ARGLIST,new ArrayList<String>());
		if(ctx.vararg()!=null) visit(ctx.vararg());
		@SuppressWarnings("unchecked")
		List<String> args = (List<String>)sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
		// Convert into a comma-delimited string
		StringBuffer argnames = new StringBuffer();
		int index = 0;
		while( index<args.size() ) {
			if( argnames.length()>0) argnames.append(",");
			argnames.append("arg"+String.valueOf(index));
			index++;
		}
		
		InlineScript runner = new InlineScript(modulePath,argnames.toString(),args);
		String alias = nextTemporaryName();
		registry.setInlineScriptAlias(alias, runner);
		sharedDictionary.put(TestFrameProperties.CP_VALUE,alias);
		return null;
	}
	@Override 
	public Object visitSelectCommand(ScriptSyntaxParser.SelectCommandContext ctx) { 
		prototype.setCommandName("SelectCommand");
		prototype.setCommandType(CommandType.SELECT);
		String option = ctx.NAME().getText();  // Option
		if( !option.equalsIgnoreCase(TestFrameProperties.COMMAND_BUTTON) &&
			!option.equalsIgnoreCase(TestFrameProperties.COMMAND_MENU)	       )
				recordError(option,"is not a valid select command option","");
		sharedDictionary.put(TestFrameProperties.CP_NAME, option);
		visit(ctx.expr());  // Result goes in CP_VALUE, move it to CP_PATH.
		sharedDictionary.put(TestFrameProperties.CP_PATH,sharedDictionary.get(TestFrameProperties.CP_VALUE));
		return null; 
	}
	// Note: This same command is used for "animate" as well as show. The argument is
	//       the concatenation of the first two arguments
	@Override 
	public Object visitShowCommand(ScriptSyntaxParser.ShowCommandContext ctx) { 
		prototype.setCommandName("ShowCommand");
		prototype.setCommandType(CommandType.SHOW);
		String command = ctx.Show().getText().toLowerCase();
		String option = command+":"+ctx.NAME().getText().toLowerCase();  // Option
		if( !option.equalsIgnoreCase(TestFrameProperties.ANIMATE_CHART) &&
			!option.equalsIgnoreCase(TestFrameProperties.SHOW_BROWSER)  &&
			!option.equalsIgnoreCase(TestFrameProperties.SHOW_CHART)  &&
			!option.equalsIgnoreCase(TestFrameProperties.SHOW_DIAGRAM)  
			) {
			recordError(option,"is not a valid animate/show command combination","");
		}
				
		sharedDictionary.put(TestFrameProperties.CP_NAME, option);
		visit(ctx.expr());  // Result goes in CP_VALUE, move it to CP_PATH.
		sharedDictionary.put(TestFrameProperties.CP_PATH,sharedDictionary.get(TestFrameProperties.CP_VALUE));
		return null; 
	}
	@Override 
	public Object visitStartExpression(ScriptSyntaxParser.StartExpressionContext ctx) { 
		if( ctx.timeexpr()!=null) visit(ctx.timeexpr());
		prototype.setCommandType(CommandType.START);
		prototype.setCommandName("start");
		return null;
	}
	@Override 
	public Object visitStringExpression(ScriptSyntaxParser.StringExpressionContext ctx) { 
		sharedDictionary.put(TestFrameProperties.CP_VALUE, stripQuotes(ctx.STRING().getText()));
		return null;
	}
	@Override 
	public Object visitStopExpression(ScriptSyntaxParser.StopExpressionContext ctx) { 
		if( ctx.timeexpr()!=null) visit(ctx.timeexpr());
		prototype.setCommandType(CommandType.STOP);
		prototype.setCommandName("stop");
		return null;
	}
	// This is another place where it is OK to evaluate the AliasRegistry at parse-time.
	@SuppressWarnings("unchecked")
	@Override 
	public Object visitTagDefinition(ScriptSyntaxParser.TagDefinitionContext ctx) { 
		prototype.setCommandType(CommandType.TAG_DEFINITION);
		sharedDictionary.put(TestFrameProperties.CP_NAME, ctx.NAME().getText());
		// Clear tag path components
		List<String> pathComponents = new ArrayList<>();
		sharedDictionary.put(TestFrameProperties.CP_ARGLIST, pathComponents);
		visit(ctx.mixedpath());
		pathComponents = (List<String>) sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
		StringBuilder arg = new StringBuilder();
		for(String component:pathComponents) {
			if( arg.length()>0) arg.append("/");
			arg.append(component);
		}
		
		sharedDictionary.put(TestFrameProperties.CP_VALUE, arg.toString());
		return null;
	}
	// Same as previous, except that the tag path is a quoted string, presumeably including
	// a specified provider.
	@SuppressWarnings("unchecked")
	@Override 
	public Object visitTagStringDefinition(ScriptSyntaxParser.TagStringDefinitionContext ctx) { 
		prototype.setCommandType(CommandType.TAG_DEFINITION);
		sharedDictionary.put(TestFrameProperties.CP_NAME, stripQuotes(ctx.STRING().getText()));
		// Clear tag path components
		List<String> pathComponents = new ArrayList<>();
		sharedDictionary.put(TestFrameProperties.CP_ARGLIST, pathComponents);
		visit(ctx.mixedpath());
		pathComponents = (List<String>) sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
		StringBuilder arg = new StringBuilder();
		for(String component:pathComponents) {
			if( arg.length()>0) arg.append("/");
			arg.append(component);
		}

		sharedDictionary.put(TestFrameProperties.CP_VALUE, arg.toString());
		return null;
	}
	@Override 
	public Object visitTagProviderSetter(ScriptSyntaxParser.TagProviderSetterContext ctx) { 
		prototype.setCommandType(CommandType.TAG_PROVIDER_SETTER);
		prototype.setCommandName(toTitleCase(ctx.TagProviderSetter().getText()));
		if( ctx.STRING()!=null ) {
			sharedDictionary.put(TestFrameProperties.CP_TEXTPARAM, stripQuotes(ctx.STRING().getText()));
		}
		else if( ctx.expr()!=null ) {
			visit(ctx.expr());  // Expression parsers place result in CP_VALUE
			sharedDictionary.put(TestFrameProperties.CP_TEXTPARAM, sharedDictionary.get(TestFrameProperties.CP_VALUE));
		}
		Map<String,String>map = new HashMap<>();
		sharedDictionary.put(TestFrameProperties.CP_ARGMAP,map);
		// Get the arguments, if any
		if( ctx.namevalue()!=null) {
			visit(ctx.namevalue());
		}
		return null; 
	}
	@Override 
	public Object visitTagSet(ScriptSyntaxParser.TagSetContext ctx) { 
		prototype.setCommandName("TagSet");
		String name = ctx.NAME().getText();
		if( name!=null ) {
			sharedDictionary.put(TestFrameProperties.CP_NAME,name);
			sharedDictionary.put(TestFrameProperties.CP_ARGLIST,new ArrayList<String>());
			if( ctx.vararg()!=null) {
				prototype.setCommandType(CommandType.TAG_SET_DEFINITION);
				visit(ctx.vararg());
				sharedDictionary.put(TestFrameProperties.CP_TAGLIST,sharedDictionary.get(TestFrameProperties.CP_ARGLIST));
			}
			else {
				prototype.setCommandType(CommandType.TAG_SET_SELECTION);   // Select previously defined tag set
			}
		}
		return null; 
	}
	// A new line of data
	@Override 
	public Object visitTagValues(ScriptSyntaxParser.TagValuesContext ctx) { 
		prototype.setCommandType(CommandType.TAG_DATA);
		prototype.setCommandName("TagData");
		visit(ctx.timeexpr());
		List<String> arglist = new ArrayList<String>();
		sharedDictionary.put(TestFrameProperties.CP_ARGLIST,arglist);
		int argcount = ctx.value().size();
		int index = 0;
		while( index<argcount) {
			String val = ctx.value(index).getText();
			// The value may have a single leading comma, may be a quoted string
			if(val.startsWith(",")) val = val.substring(1);
			//log.infof("%s.visitTagValues: got %s",TAG,val);
			arglist.add(stripQuotes(val));
			index++;
		}
		return null; 
	}
	@Override 
	public Object visitTextPropertySetter(ScriptSyntaxParser.TextPropertySetterContext ctx) { 
		prototype.setCommandType(CommandType.TEXT_PARAMETER_SETTER);
		prototype.setCommandName(toTitleCase(ctx.StringSetter().getText()));
		if( ctx.STRING()!=null ) {
			String text = stripQuotes(ctx.STRING().getText());
			sharedDictionary.put(TestFrameProperties.CP_TEXTPARAM, text);
		}
		else if( ctx.expr()!=null ) {
			visit(ctx.expr());  // Expression parsers place result in CP_VALUE
			sharedDictionary.put(TestFrameProperties.CP_TEXTPARAM, sharedDictionary.get(TestFrameProperties.CP_VALUE));
		}
		else {
			StringBuilder text = new StringBuilder();
			if( ctx.word()!=null) {
				int count = ctx.word().size();
				int index = 0;
				while( index<count ) {
					if( index>0 ) text.append(" ");
					text.append(ctx.word(index).getText());
					index++;
				}
			}
		
			if( ctx.TEXT()!=null) {
				if( text.length()>0 )text.append(" ");
				text.append(ctx.TEXT().getText());
			}
			sharedDictionary.put(TestFrameProperties.CP_TEXTPARAM, text.toString()); 
		}
		return null; 
	}
	@Override 
	public Object visitTimeUnitExpression(ScriptSyntaxParser.TimeUnitExpressionContext ctx) { 
		String name = ctx.TimeSetter().getText();
		sharedDictionary.put(TestFrameProperties.CP_NAME, name);
		if( ctx.numval()!=null ) {
			// Note: The parser should guarantee a valid number. Do some checks here for valid range.
			long val = convertTime(ctx.numval().getText(),ctx.TIMEUNIT().getText());
			if( val>0) {
				sharedDictionary.put(TestFrameProperties.CP_VALUE, new Long(val));
			}
			else {
				recordError(ctx.TimeSetter().getText()," must be positive","");
			}
		}
		return null; 
	}
	@Override 
	public Object visitTimedVariableDefinition(ScriptSyntaxParser.TimedVariableDefinitionContext ctx) {
		visit(ctx.timeexpr());
		prototype.setCommandType(CommandType.VARIABLE_DEFINITION);
		prototype.setCommandName("VariableDefinition");
		String command = ctx.NAME().getText();
		sharedDictionary.put(TestFrameProperties.CP_NAME, command);   // Definition does not prepend $
		visit(ctx.expr());     // Places result in CP_VALUE
		// The only legal operator is =
		String opr = ctx.ROPR().getText();
		if( !opr.equalsIgnoreCase("=")      )
			recordError(opr,"is not a valid assignment operator","");
		return null;
	}
	// Store arg in CP_CURRENT_TIME
	@Override 
	public Object visitTimeOnlyExpression(ScriptSyntaxParser.TimeOnlyExpressionContext ctx) { 
		String time = ctx.TIME().getText();   // hh:mm:ss
		sharedDictionary.put(TestFrameProperties.CP_CURRENT_TIME, scrubTime(time));
		sharedDictionary.put(TestFrameProperties.CP_TIMESTAMP_MARKER, "true");
		return null;
	}
	@Override 
	public Object visitTimePropertySetter(ScriptSyntaxParser.TimePropertySetterContext ctx) { 
		prototype.setCommandType(CommandType.TIME_PARAMETER_SETTER);
		visit(ctx.timeclause());
		prototype.setCommandName(toTitleCase(sharedDictionary.get(TestFrameProperties.CP_NAME).toString()));
		// NOTE: Parsing the timeclase has already set CP_VALUE
		return null; 
	}
	@Override 
	public Object visitUntilCommand(ScriptSyntaxParser.UntilCommandContext ctx) {
		prototype.setCommandName("until");
		Long interval = (Long)sharedDictionary.get(TestFrameProperties.CP_DEFAULT_POLL_INTERVAL);
		Long timeout = (Long)sharedDictionary.get(TestFrameProperties.CP_DEFAULT_TIMEOUT);
		int clauseCount = ctx.timeclause().size();
		int index = 0;
		while( index<clauseCount) {
			visit(ctx.timeclause(index));
			String name = sharedDictionary.get(TestFrameProperties.CP_NAME).toString();
			Long value = (Long)sharedDictionary.get(TestFrameProperties.CP_VALUE);
			if( name.equalsIgnoreCase("poll")) interval = value;
			else if( name.equalsIgnoreCase("timeout")) timeout = value;
			index++;
		}
		if( interval <= 0 ) {
			recordError(String.valueOf(interval)," (poll interval) must be positive","");
		}
		else {
			int count = 1 + (int)(timeout.longValue()/interval.longValue());
			sharedDictionary.put(TestFrameProperties.CP_COUNT,new Integer(count));
			sharedDictionary.put(TestFrameProperties.CP_POLL_INTERVAL,new Long(interval));
			visit(ctx.assertion());
		}
		return null;
	}
	@Override 
	public Object visitUntimedScriptCommand(ScriptSyntaxParser.UntimedScriptCommandContext ctx) { 
		prototype.setCommandType(CommandType.SCRIPT);
		prototype.setCommandName("ScriptCommand(untimed)");
		String command = ctx.NAME().getText();
		sharedDictionary.put(TestFrameProperties.CP_NAME, command);
		sharedDictionary.put(TestFrameProperties.CP_ARGLIST,new ArrayList<String>());  // Clear arglist
		if(ctx.vararg()!=null) visit(ctx.vararg());
		return null;
	}
	// Store a variable argument list in CP_ARGLIST. The parent command must clear the list.
	@Override 
	public Object visitVarArgExpression(ScriptSyntaxParser.VarArgExpressionContext ctx) { 
		@SuppressWarnings("unchecked")
		List<String>list = (List<String>)sharedDictionary.get(TestFrameProperties.CP_ARGLIST); 
		if( ctx.word()!=null )  visit(ctx.word());
		if( ctx.NAME()!=null )      list.add(ctx.NAME().getText());
		else if(ctx.INT()!=null )   list.add(ctx.INT().getText());
		else if(ctx.FLOAT()!=null ) list.add(ctx.FLOAT().getText());
		else if(ctx.word()!=null ) list.add(ctx.word().getText());
		else if(ctx.STRING()!=null ) list.add(stripQuotes(ctx.STRING().getText()));
		else if(ctx.VARNAME()!=null ) {
			String name = ctx.VARNAME().getText().substring(1);  // Strip $
			Object val = registry.valueForAlias(name);
			if( val!=null ) {
				list.add(val.toString());
			}
			else {
				list.add("NO_VALUE("+name+")");
				recordError(ctx.VARNAME().getText()," is undefined","");
			}
		}
		//log.infof("%s.visitVarArgExp: got %s",TAG,ctx.NAME().getText());
		return null;
	}
	// Store a variable argument list in CP_ARGLIST. It is OK to evaluate variable
	// substitutions here.
	@Override 
	public Object visitVarArgRecursive(ScriptSyntaxParser.VarArgRecursiveContext ctx) { 
		@SuppressWarnings("unchecked")
		List<String>list = (List<String>)sharedDictionary.get(TestFrameProperties.CP_ARGLIST);
		if(ctx.vararg()!=null) visit(ctx.vararg());
		if( ctx.word()!=null )  visit(ctx.word());
		if( ctx.NAME()!=null )      list.add(ctx.NAME().getText());
		else if(ctx.INT()!=null )   list.add(ctx.INT().getText());
		else if(ctx.FLOAT()!=null ) list.add(ctx.FLOAT().getText());
		else if(ctx.word()!=null ) list.add(ctx.word().getText());
		else if(ctx.STRING()!=null ) list.add(stripQuotes(ctx.STRING().getText()));
		else if(ctx.VARNAME()!=null ) {
			String name = ctx.VARNAME().getText().substring(1);  // Strip $
			Object val = registry.valueForAlias(name);
			if( val!=null ) {
				list.add(val.toString());
			}
			else {
				list.add("NO_VALUE("+name+")");
				recordError(ctx.VARNAME().getText()," is undefined","");
			}
		}
		//log.infof("%s.visitVarArgRec: got %s",TAG,ctx.NAME().getText());
		return null;
	}
	@Override 
	public Object visitVariableDefinition(ScriptSyntaxParser.VariableDefinitionContext ctx) { 
		prototype.setCommandType(CommandType.VARIABLE_DEFINITION);
		prototype.setCommandName("VariableDefinition");
		String command = ctx.NAME().getText();
		sharedDictionary.put(TestFrameProperties.CP_NAME, command);   // Definition does not prepend $
		visit(ctx.expr());     // Places result in CP_ARG
		// The only legal operator is =
		String opr = ctx.ROPR().getText();
		if( !opr.equalsIgnoreCase("=")      )
			recordError(opr,"is not a valid assignment operator","");
		return null;
	}
	// This is one of the few cases where it is valid to evaluate the AliasRegistry at parse time.
	@Override 
	public Object visitVariableExpression(ScriptSyntaxParser.VariableExpressionContext ctx) { 
		String name = ctx.VARNAME().getText().substring(1);  // Strip $
		Object val = registry.valueForAlias(name);
		if( val!=null ) {
			sharedDictionary.put(TestFrameProperties.CP_VALUE, val.toString());
		}
		else {
			recordError(ctx.VARNAME().getText()," is undefined","");
		}
		return null;
	}
	@Override 
	public Object visitWaitCommand(ScriptSyntaxParser.WaitCommandContext ctx) { 
		prototype.setCommandType(CommandType.WAIT);
		prototype.setCommandName("wait");
		String numericValue = "0";
		if(ctx.numval()!=null) {
			numericValue =  ctx.numval().getText();
		}
		else if(ctx.VARNAME()!=null) {
			String name = ctx.VARNAME().getText().substring(1);  // Strip $
			Object val = registry.valueForAlias(name);
			if( val!=null ) {
				numericValue = val.toString();
			}
			else {
				recordError(ctx.VARNAME().getText()," is undefined","");
			}
		}
		String unit = "seconds";    // Unit is optional
		if(ctx.TIMEUNIT()!=null ) unit = ctx.TIMEUNIT().getText();
		sharedDictionary.put(TestFrameProperties.CP_VALUE,new Long(convertTime(numericValue,unit)));
		return null;
	}

	// ================================ Helper Methods =========================================
	// The only conversions are from minutes. Canonical time is milli-seconds.
	private long convertTime(String value,String unit) {
		long result = 1000;
		try {
			double val = Double.parseDouble(value);
			if(unit.toLowerCase().startsWith("m") ) {
				val = val*60.*1000;
			}
			else {
				val = val*1000;
			}
			result = (long)val;
		}
		catch(NumberFormatException nfe) {
			log.warnf("%s.convertTime: value not a double (%s)", TAG,nfe.getMessage());
		}
		return result;
	}
	
	
	private void recordError(String text,String verb,String context  ) {
        String msg = "";
        if(verb==null) verb = "";
        if(verb.length()>0) verb = " "+verb;
        if( context!=null && context.length()>0 ) {
            msg = String.format("%s%s \'%s\'",text,verb,context);
            sharedDictionary.put(TestFrameProperties.EXPR_ERR_TOKEN, context);
        }
        else {
            msg = String.format("%s%s",text,verb); // In this case verb may have an argument
        }

        log.info(TAG+msg);
        sharedDictionary.put(TestFrameProperties.EXPR_ERR_MESSAGE, msg);
        sharedDictionary.put(TestFrameProperties.EXPR_ERR_LINE, "1");     // Would be nice if we knew the line
        sharedDictionary.put(TestFrameProperties.EXPR_ERR_POSITION,"0");
    }

	private String scrubTime(String arg) {
		String result = arg.trim();
		// Time may not have seconds
		int index1 = arg.indexOf(":");
		if( index1>0) {
			int index2 = arg.lastIndexOf(":");
			if( index2<=index1) result = arg+":00";
		}
		return result;
	}
	private String stripQuotes(String arg) {
		String result = arg.trim();
		if( result.endsWith("\"") )  result = result.substring(0, result.length()-1);
		if( result.startsWith("\"")) result = result.substring(1);
		return result;
	}
	
	public String toTitleCase(String input) {
	    StringBuilder titleCase = new StringBuilder();
	    boolean nextTitleCase = true;

	    for (char c : input.toCharArray()) {
	        if (Character.isSpaceChar(c)) {
	            nextTitleCase = true;
	        } 
	        else if (nextTitleCase) {
	            c = Character.toTitleCase(c);
	            nextTitleCase = false;
	        }
	        titleCase.append(c);
	    }
	    return titleCase.toString();
	}	
	
	/**
	 * If we see this string in the UI, something has gone wrong.
	 * @return a name that is unique for this instance (script line)
	 */
	private String nextTemporaryName() {
		String name = "UNDEFINED"+String.valueOf(tempCounter);
		this.tempCounter++;
		return name;
	}
}
