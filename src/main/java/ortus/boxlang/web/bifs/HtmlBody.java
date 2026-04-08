/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.web.bifs;

import java.util.Set;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.util.KeyDictionary;

@BoxBIF( description = "Writes text to the body section of a generated HTML page. It supports appending, writing, reading, resetting, or flushing the HTML body buffer." )
public class HtmlBody extends BIF {

	/**
	 * Constructor
	 */
	public HtmlBody() {
		super();
		declaredArguments = new Argument[] {
		    /**
		     * append (default): append text to the HTML body
		     * read: return the text already set to HTML body
		     * reset: reset/remove text already set to HTML body
		     * write: write text to HTML body, overwrite already existing text in HTML body
		     * flush: writes the buffer in the HTML body to the response stream
		     */
		    new Argument(
		        true,
		        Argument.STRING,
		        Key.action,
		        "append",
		        Set.of( Validator.valueOneOf( "append", "read", "reset", "write", "flush" ) )
		    ),
		    // Name of variable to contain the text for HTML body (used by the read action).
		    new Argument( false, Argument.STRING, Key.variable ),
		    // ID of the snippet that is added, used to ensure that the same snippet will not be added more than once.
		    new Argument( false, Argument.STRING, Key.id ),
		    // The text to add to the 'body' area of an HTML page. Alternatively if a closing tag is used then the body between the tags is used.
		    new Argument( false, Argument.STRING, Key.text ),
		    // If set to true, it works even within a silent block.
		    new Argument( false, Argument.BOOLEAN, KeyDictionary.silent )
		};
	}

	/**
	 * Invokes the BIF to perform the specified action on the HTML body based on the provided arguments.
	 * <ul>
	 * <li><code>append</code> (default): Append text to the HTML body buffer.</li>
	 * <li><code>read</code>: Return the text currently in the HTML body buffer and optionally store it in a variable.</li>
	 * <li><code>reset</code>: Reset/remove text already set in the HTML body buffer.</li>
	 * <li><code>write</code>: Write text to the HTML body, overwriting any existing text in the buffer.</li>
	 * <li><code>flush</code>: Write the HTML body buffer content to the current output stream and clear the buffer.</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.action The action to perform on the HTML body (append, read, reset, write, flush). Default: append.
	 *
	 * @argument.variable (Optional) Name of the variable to store the HTML body text in. Used with the read action. Defaults to "htmlbody".
	 *
	 * @argument.id (Optional) ID of the snippet. When specified, ensures that the same snippet is not added more than once per request.
	 *
	 * @argument.text (Optional) The text to add to the body area of an HTML page. When a component body is used, the rendered body content is used
	 *                instead (or appended to the text attribute).
	 *
	 * @argument.silent (Optional) If true, the BIF works even within a silent block.
	 *
	 * @return For the read action, returns the current HTML body buffer content. For all other actions, returns true.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	action		= arguments.getAsString( Key.action );
		String	text		= arguments.getAsString( Key.text );
		String	variable	= arguments.getAsString( Key.variable );
		String	id			= arguments.getAsString( Key.id );

		return addToBody( context, action, text, variable, id );
	}

	/**
	 * Unified static delegate that dispatches to the appropriate action handler.
	 * This method can be called from the companion HtmlBody component.
	 *
	 * @param context  The context in which the BIF is being invoked.
	 * @param action   The action to perform (append, write, read, reset, flush).
	 * @param text     The text to add (used by append and write actions).
	 * @param variable The variable name to store the read result in (used by read action).
	 * @param id       Optional unique ID to prevent duplicate snippets.
	 *
	 * @return For the read action, returns the current HTML body buffer content. For all other actions, returns true.
	 */
	public static Object addToBody( IBoxContext context, String action, String text, String variable, String id ) {
		if ( action == null ) {
			action = "append";
		}
		switch ( action.toLowerCase().trim() ) {
			case "append" :
				return appendToBody( context, text, id );
			case "write" :
				resetIdMap( context );
				resetBody( context );
				return appendToBody( context, text, id );
			case "read" :
				return readBody( context, variable );
			case "reset" :
				resetIdMap( context );
				return resetBody( context );
			case "flush" :
				return flushBody( context );
			default :
				return true;
		}
	}

	/**
	 * Appends text to the HTML body buffer. On the first call, this method registers an onRequestEnd interceptor
	 * that injects the accumulated buffer into the rendered HTML body element using Jsoup.
	 *
	 * If an {@code id} is provided, the same snippet (identified by that id) will not be appended more than once per
	 * request.
	 *
	 * @param context The context in which the BIF is being invoked.
	 * @param text    The text to append to the HTML body buffer.
	 * @param id      Optional unique ID. When specified, prevents the same snippet from being added twice.
	 *
	 * @return true if text was appended, false if it was skipped (e.g. because the id was already registered).
	 */
	public static boolean appendToBody( IBoxContext context, String text, String id ) {
		if ( text == null ) {
			return false;
		}

		RequestBoxContext requestContext = context.getParentOfType( RequestBoxContext.class );

		// Initialize the buffer and register the end-of-request interceptor on first use
		if ( !requestContext.hasAttachment( KeyDictionary.htmlBody ) ) {
			requestContext.putAttachment( KeyDictionary.htmlBody, new StringBuffer() );

			requestContext
			    .getApplicationListener()
			    .getInterceptorPool()
			    .register( data -> {
				    IBoxContext dataContext = ( IBoxContext ) data.get( Key.context );
				    if ( requestContext.hasAttachment( KeyDictionary.htmlBody ) ) {
					    StringBuffer bodyBuffer	= requestContext.getAttachment( KeyDictionary.htmlBody );
					    String		bodyContent	= bodyBuffer.toString();
					    if ( !bodyContent.isEmpty() ) {
						    StringBuffer buffer	= dataContext.getBuffer();
						    Document	doc		= Jsoup.parse( buffer.toString() );
						    Element		body	= doc.body();
						    // Prepend so that cfhtmlbody content appears at the top of the body
						    body.prepend( bodyContent );
						    dataContext.clearBuffer();
						    dataContext.writeToBuffer( doc.toString() );
					    }
				    }
				    return false;
			    }, KeyDictionary.onRequestEnd );
		}

		// Check ID uniqueness — skip if this id has already been registered this request
		if ( id != null && !id.isEmpty() ) {
			TreeMap<String, Boolean> idMap = getOrCreateIdMap( requestContext );
			if ( idMap.containsKey( id ) ) {
				return false;
			}
			idMap.put( id, Boolean.TRUE );
		}

		( ( StringBuffer ) requestContext.getAttachment( KeyDictionary.htmlBody ) )
		    .append( text );
		return true;
	}

	/**
	 * Reads the current HTML body buffer content and optionally stores it in a named variable in the variables scope.
	 *
	 * @param context  The current box context.
	 * @param variable Optional variable name to assign the result to. Defaults to "htmlbody".
	 *
	 * @return The current HTML body buffer content as a string.
	 */
	public static String readBody( IBoxContext context, String variable ) {
		RequestBoxContext	requestContext	= context.getParentOfType( RequestBoxContext.class );
		String				content			= "";
		if ( requestContext.hasAttachment( KeyDictionary.htmlBody ) ) {
			content = requestContext.getAttachment( KeyDictionary.htmlBody ).toString();
		}

		// Store the result in the specified variable in the variables scope, if provided
		String varName = ( variable != null && !variable.isEmpty() ) ? variable : "htmlbody";
		context.getScopeNearby( VariablesScope.name ).assign( context, Key.of( varName ), content );

		// Set the result variable before returning
		ExpressionInterpreter.setVariable(
		    context,
		    varName,
		    content
		);

		return content;
	}

	/**
	 * Clears the HTML body buffer without writing it to the output stream.
	 *
	 * @param context The current box context.
	 *
	 * @return true always.
	 */
	public static boolean resetBody( IBoxContext context ) {
		RequestBoxContext requestContext = context.getParentOfType( RequestBoxContext.class );
		if ( requestContext.hasAttachment( KeyDictionary.htmlBody ) ) {
			( ( StringBuffer ) requestContext.getAttachment( KeyDictionary.htmlBody ) )
			    .setLength( 0 );
		}
		return true;
	}

	/**
	 * Writes the HTML body buffer content to the current output stream and then clears the buffer.
	 *
	 * @param context The current box context.
	 *
	 * @return true always.
	 */
	public static boolean flushBody( IBoxContext context ) {
		RequestBoxContext requestContext = context.getParentOfType( RequestBoxContext.class );
		if ( requestContext.hasAttachment( KeyDictionary.htmlBody ) ) {
			StringBuffer	bodyBuffer	= requestContext.getAttachment( KeyDictionary.htmlBody );
			String			content		= bodyBuffer.toString();
			if ( !content.isEmpty() ) {
				context.writeToBuffer( content );
				bodyBuffer.setLength( 0 );
			}
		}
		return true;
	}

	/**
	 * Resets the ID de-duplication map, allowing previously registered IDs to be re-registered.
	 * Called internally by the write and reset actions.
	 *
	 * @param context The current box context.
	 */
	private static void resetIdMap( IBoxContext context ) {
		RequestBoxContext requestContext = context.getParentOfType( RequestBoxContext.class );
		requestContext.putAttachment( KeyDictionary.htmlBodyIdMap, new TreeMap<>( String.CASE_INSENSITIVE_ORDER ) );
	}

	/**
	 * Returns the case-insensitive ID map for the current request, creating it if it does not yet exist.
	 *
	 * @param requestContext The request context.
	 *
	 * @return The ID map for the current request.
	 */
	@SuppressWarnings( "unchecked" )
	private static TreeMap<String, Boolean> getOrCreateIdMap( RequestBoxContext requestContext ) {
		if ( !requestContext.hasAttachment( KeyDictionary.htmlBodyIdMap ) ) {
			requestContext.putAttachment( KeyDictionary.htmlBodyIdMap, new TreeMap<>( String.CASE_INSENSITIVE_ORDER ) );
		}
		return ( TreeMap<String, Boolean> ) requestContext.getAttachment( KeyDictionary.htmlBodyIdMap );
	}
}
