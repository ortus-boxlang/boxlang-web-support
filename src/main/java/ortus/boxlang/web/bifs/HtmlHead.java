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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class HtmlHead extends BIF {

	/**
	 * Constructor
	 */
	public HtmlHead() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.text, Set.of( Validator.NON_EMPTY ) ),
		};
	}

	/**
	 *
	 * Writes text to the head section of a generated HTML page. It is
	 * useful for embedding JavaScript code, or putting other HTML tags, such as meta, link,
	 * title, or base in an HTML page header.
	 *
	 * If there is no head section in the HTML page, the function will create one.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.text Text to add to the head area of an HTML page.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the text to add to the head area of an HTML page
		var	text	= arguments.getAsString( Key.text ).trim();

		// Use JSOUP to parse the HTML page in the buffer
		var	doc		= context.getBuffer().toString();

		// Find the head section in the HTML page
		var	head	= doc.indexOf( "<head>" );

		// If there is no head section in the HTML page, create one with the `text` content in the middle
		if ( head == -1 ) {
			doc = doc.replaceFirst( "<html>", "<html><head>" + text + "</head>" );
		} else {
			// If there is a head section in the HTML page, add the `text` content to the end of the head section
			doc = doc.substring( 0, head + 6 ) + text + doc.substring( head + 6 );
		}

		// Set the modified HTML page back to the buffer
		context.clearBuffer();
		context.writeToBuffer( doc );

		return true;
	}
}
