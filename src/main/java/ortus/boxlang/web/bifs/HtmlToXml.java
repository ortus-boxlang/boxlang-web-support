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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.util.KeyDictionary;

@BoxBIF
public class HtmlToXml extends BIF {

	/**
	 * Constructor
	 */
	public HtmlToXml() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, KeyDictionary.html, Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Leverage Jsoup to parse HTML content and return a BoxLang XML object.
	 * This method uses Jsoup to parse the HTML string provided in the arguments.
	 * This will add:
	 * <ul>
	 * <li>Self closing tags for elements that do not have children.</li>
	 * <li>Convert the HTML to a well-formed XML structure.</li>
	 * <li>Ensure that the document is parsed with UTF-8 encoding.</li>
	 * <li>Escape special characters in the HTML content.</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.html A HTML string to be parsed.
	 *
	 * @return A BoxLang XML object representing the parsed HTML content.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		var target = arguments.getAsString( KeyDictionary.html );

		if ( target == null || target.isEmpty() ) {
			return new XML( false );
		}

		Document doc = Jsoup.parse( target );
		doc.outputSettings().escapeMode( org.jsoup.nodes.Entities.EscapeMode.base );
		doc.outputSettings().charset( "UTF-8" );
		doc.outputSettings().syntax( org.jsoup.nodes.Document.OutputSettings.Syntax.xml );

		return new XML( doc.outerHtml() );
	}

}
