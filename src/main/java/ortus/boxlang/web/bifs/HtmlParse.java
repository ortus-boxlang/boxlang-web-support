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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.util.KeyDictionary;

@BoxBIF
public class HtmlParse extends BIF {

	/**
	 * Constructor
	 */
	public HtmlParse() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, KeyDictionary.html, Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Leverage Jsoup to parse HTML content and return a Document object.
	 * You can find all the information about Jsoup here: <a href="https://jsoup.org/apidocs/org/jsoup/nodes/Document.html">Jsoup Document</a>
	 * Some of the most common methods you can use on the Document object include:
	 * <ul>
	 * <li><code>title()</code> – Get the contents of the &lt;title&gt; tag.</li>
	 * <li><code>select(selector)</code> – Find elements using CSS selectors.</li>
	 * <li><code>text()</code> – Get the combined text of the entire document.</li>
	 * <li><code>outerHtml()</code> – Get the HTML of the entire document.</li>
	 * <li><code>body()</code> – Get the &lt;body&gt; element.</li>
	 * <li><code>head()</code> – Get the &lt;head&gt; element.</li>
	 * <li><code>getElementById(id)</code> – Find an element by its ID attribute.</li>
	 * <li><code>getElementsByTag(tagName)</code> – Get all elements with the given tag.</li>
	 * <li><code>getElementsByClass(className)</code> – Get all elements with the given class.</li>
	 * <li><code>getElementsByAttribute(attrName)</code> – Get elements that have the specified attribute.</li>
	 * <li><code>html()</code> – Get the inner HTML of the document body.</li>
	 * <li><code>createElement(tagName)</code> – Create a new element with the given tag.</li>
	 * <li><code>charset()</code> – Get the document's character set.</li>
	 * <li><code>location()</code> – Get the URL from which the document was loaded.</li>
	 * <li><code>normalise()</code> – Normalize the document structure (e.g., ensures head/body exist).</li>
	 * </ul>
	 *
	 * <h2>Usage</h2>
	 *
	 * <pre>
	 * // Parse a simple HTML string
	 * htmlContent = "<html><head><title>My Page</title></head><body><h1>Hello World</h1></body></html>";
	 * doc = htmlParse( htmlContent );
	 * title = doc.title(); // Returns "My Page"
	 *
	 * // Extract text content from HTML
	 * htmlContent = "<div class='content'><p>First paragraph</p><p>Second paragraph</p></div>";
	 * doc = htmlParse( htmlContent );
	 * text = doc.text(); // Returns "First paragraph Second paragraph"
	 *
	 * // Select elements using CSS selectors
	 * htmlContent = "<ul><li class='item'>Item 1</li><li class='item'>Item 2</li></ul>";
	 * doc = htmlParse( htmlContent );
	 * items = doc.select( ".item" ); // Returns elements with class 'item'
	 *
	 * // Find elements by ID and modify content
	 * htmlContent = "<div id='main'><p>Original content</p></div>";
	 * doc = htmlParse( htmlContent );
	 * mainDiv = doc.getElementById( "main" );
	 * // You can now work with the mainDiv element
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.html A HTML string to be parsed.
	 *
	 * @return A Jsoup Document object representing the parsed HTML content.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		var target = arguments.getAsString( KeyDictionary.html );

		if ( target == null || target.isEmpty() ) {
			return Jsoup.parse( "" );
		}

		return Jsoup.parse( target );
	}

}
