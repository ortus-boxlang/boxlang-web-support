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
import org.jsoup.safety.Safelist;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.util.KeyDictionary;

@BoxBIF
public class HtmlClean extends BIF {

	/**
	 * Constructor
	 */
	public HtmlClean() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, KeyDictionary.html, Set.of( Validator.NON_EMPTY ) ),
		    new Argument( false, Argument.STRING, KeyDictionary.safeList, "relaxed",
		        Set.of( Validator.valueOneOf( "basic", "none", "simpletext", "basicwithimages", "relaxed" ) )
		    ),
		    new Argument( false, Argument.BOOLEAN, KeyDictionary.preserveRelativeLinks, false ),
		    new Argument( false, Argument.STRING, KeyDictionary.baseUri, "" )
		};
	}

	/**
	 * This method cleans the provided HTML string using Jsoup. Here are some details about its functionality:
	 * <h2>Functionality</h2>
	 * <ul>
	 * <li>The safeList argument determines the level of cleaning applied.</li>
	 * <li>It returns a cleaned HTML string or an empty string if the input is null or empty.</li>
	 * <li>By default it will use the "relaxed" safelist, which allows a wide range of HTML tags and attributes.</li>
	 * <li>By default, it does not preserve relative links, but this can be controlled with the <code>preserveRelativeLinks</code> argument.</li>
	 * <li>The <code>baseUri</code> argument is used to resolve relative links into absolute URLs <strong>only when</strong>
	 * <code>preserveRelativeLinks</code> is <code>false</code> (the default behavior). If <code>preserveRelativeLinks</code> is <code>true</code>, then
	 * <code>baseUri</code> is ignored.</li>
	 * </ul>
	 * <h2>Safe List Options</h2>
	 * The valid values for safeList are:
	 * <ul>
	 * <li><code>basic</code>: Basic cleaning, removes all tags except for a few safe ones.</li>
	 * <li><code>none</code>: Maximum cleaning, removes all tags and returns plain text only.</li>
	 * <li><code>simpletext</code>: Similar to <code>none</code>, but allows very limited inline formatting tags like <code>&lt;b&gt;</code>,
	 * <code>&lt;i&gt;</code>, <code>&lt;br&gt;</code>.</li>
	 * <li><code>basicwithimages</code>: Basic cleaning but allows images.</li>
	 * <li><code>relaxed</code>: More lenient cleaning, allows more tags.</li>
	 * </ul>
	 * <h2>Differences from ESAPI</h2>
	 * This BIF uses Jsoup for HTML cleaning, which is different from ESAPI's approach.
	 * Jsoup provides a more flexible and powerful way to clean HTML, allowing for more customization and control over the cleaning process.
	 * It supports various safelists that can be used to define the level of cleaning applied to the HTML content.
	 * <h2>Usage</h2>
	 * This BIF can be used in BoxLang scripts to sanitize user input or any HTML
	 * content that needs to be cleaned before rendering it in a web application.
	 * It is particularly useful for preventing XSS (Cross-Site Scripting) attacks by
	 * ensuring that only safe HTML content is rendered in the browser.
	 * <h2>Example</h2>
	 *
	 * <pre>
	 * // Clean HTML content with basic safelist
	 * var cleanedHtml = HtmlClean( html: "&lt;script&gt;alert('XSS')&lt;/script&gt;&lt;p&gt;Hello World!&lt;/p&gt;" );
	 * // cleanedHtml will be "&lt;p&gt;Hello World!&lt;/p&gt;"
	 * // Clean HTML content with relaxed safelist
	 * var cleanedHtmlRelaxed = HtmlClean( html: "&lt;img src='image.jpg' /&gt;&lt;script&gt;alert('XSS')&lt;/script&gt;&lt;p&gt;Hello World!&lt;/p&gt;", safeList: "relaxed" );
	 * // cleanedHtmlRelaxed will be "&lt;img src='image.jpg' /&gt;&lt;p&gt;Hello World!&lt;/p&gt;"
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.html A HTML string to be cleaned.
	 *
	 * @argument.safeList The level of cleaning to apply, defaults to "relaxed".
	 *
	 * @argument.preserveRelativeLinks If true, relative links will be preserved in the cleaned HTML. Defaults to false.
	 *
	 * @argument.baseUri The base URI to resolve relative links in the cleaned HTML. Defaults to an empty string. Only active if preserveRelativeLinks is
	 *                   false.
	 *
	 * @return A cleaned HTML string or an empty string if the input is null or empty.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		var			target			= arguments.getAsString( KeyDictionary.html );
		Safelist	targetSafeList	= resolveSafelist( arguments.getAsString( KeyDictionary.safeList ) );

		if ( target == null || target.isEmpty() ) {
			return "";
		}

		// Preserve relative links if specified
		targetSafeList = targetSafeList.preserveRelativeLinks( arguments.getAsBoolean( KeyDictionary.preserveRelativeLinks ) );

		// Clean the HTML using Jsoup with the specified safelist and base URI if provided
		return Jsoup.clean( target, arguments.getAsString( KeyDictionary.baseUri ), targetSafeList );

	}

	/**
	 * Resolves the Safelist based on the provided name.
	 *
	 * @param name The name of the safelist to resolve.
	 *
	 * @return The resolved Safelist.
	 */
	private Safelist resolveSafelist( String name ) {
		return switch ( name.toLowerCase() ) {
			case "none" -> Safelist.none();
			case "simpletext" -> Safelist.simpleText();
			case "basic" -> Safelist.basic();
			case "basicwithimages" -> Safelist.basicWithImages();
			case "relaxed" -> Safelist.relaxed();
			default -> throw new BoxRuntimeException( "Unknown HTML Safelist: " + name );
		};
	}

}
