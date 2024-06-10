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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.util.KeyDictionary;

@BoxBIF
public class HtmlFooter extends BIF {

	/**
	 * Constructor
	 */
	public HtmlFooter() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.text, Set.of( Validator.NON_EMPTY ) ),
		};
	}

	/**
	 *
	 * Writes text to the footer section of a generated HTML page. It is
	 * useful for embedding JavaScript code, or putting other HTML tags or deferred scripts.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.text Text to add to the footer area of an HTML page.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the text to add to the footer area of an HTML page
		var text = arguments.getAsString( Key.text ).trim();

		// invoke add to footer
		addToFooter( context, text );

		return true;
	}

	/**
	 * Adds the `text` content to the footer section of an HTML page.
	 *
	 * @param context The context in which the BIF is being invoked.
	 * @param text    Text to add to the footer area of an HTML page.
	 */
	public static void addToFooter( IBoxContext context, String text ) {
		// Init it if it doesn't exist to an array
		if ( !context.hasAttachment( KeyDictionary.htmlFooter ) ) {
			// Init the html footer array
			context.putAttachment( KeyDictionary.htmlFooter, new Array() );

			// Init a new interceptor for the application
			ApplicationBoxContext appContext = context.getParentOfType( ApplicationBoxContext.class );
			appContext.getApplication()
			    .getInterceptorPool()
			    .register( data -> {
				    IBoxContext dataContext = ( IBoxContext ) data.get( Key.context );
				    // Only if the attachment exists use it
				    if ( dataContext.hasAttachment( KeyDictionary.htmlHead ) ) {
					    Array		headArray	= dataContext.getAttachment( KeyDictionary.htmlHead );
					    StringBuffer buffer		= dataContext.getBuffer();
					    Document	doc			= Jsoup.parse( buffer.toString() );

					    // Get the body element
					    Element		body		= doc.body();
					    // Add to the end of the body
					    for ( Object item : headArray ) {
						    body.append( item.toString() );
					    }

					    // Clear the buffer
					    dataContext.clearBuffer();

					    // Set the new buffer
					    dataContext.writeToBuffer( doc.toString() );
				    }
				    return false;
			    }, KeyDictionary.onRequestEnd );
		}

		// Append the text to the footer array
		Array footer = context.getAttachment( KeyDictionary.htmlFooter );
		footer.append( text );
	}
}
