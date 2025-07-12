/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.web.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;

/**
 * BoxDocument is a custom document class that extends Jsoup's Document.
 * It can provide fluent methods for working with BoxLang documents.
 * This class is designed to handle the specific needs of BoxLang web documents.
 */
public class BoxDocument extends Document {

	/**
	 * An empty BoxDocument instance.
	 */
	public static final BoxDocument EMPTY_DOCUMENT = new BoxDocument( "" );

	/**
	 * Constructs a new BoxDocument with the specified base URI.
	 *
	 * @param baseUri The base URI for the document.
	 */
	public BoxDocument( String baseUri ) {
		super( baseUri );
	}

	/**
	 * ----------------------------------------------------------------
	 * Static Builders
	 * ----------------------------------------------------------------
	 */

	/**
	 * Creates a BoxDocument from an existing Jsoup Document.
	 *
	 * @param document The existing Jsoup Document to convert.
	 *
	 * @return A new BoxDocument with the same content.
	 */
	public static BoxDocument fromDocument( Document document ) {
		BoxDocument boxDoc = new BoxDocument( document.baseUri() );

		// Copy all child nodes (including DOCTYPE, comments, etc.)
		for ( Node node : document.childNodes() ) {
			boxDoc.appendChild( node.clone() );
		}

		// Copy document settings and properties
		boxDoc.outputSettings( document.outputSettings().clone() );
		boxDoc.charset( document.charset() );

		return boxDoc;
	}

	/**
	 * ----------------------------------------------------------------
	 * Enhanced Methods
	 * ----------------------------------------------------------------
	 */

	/**
	 * Returns an XML representation of the document.
	 *
	 * @return An XML string representation of the document.
	 */
	public String toXML() {
		return toXML( false, 0 );
	}

	/**
	 * Returns an XML representation of the document.
	 *
	 * @param prettyPrint  If true, the XML will be formatted with indentation for readability.
	 *                     If false, the XML will be compact.
	 * @param indentFactor The number of spaces to use for indentation.
	 *
	 * @return An XML string representation of the document.
	 */
	public String toXML( Boolean prettyPrint, int indentFactor ) {
		if ( prettyPrint ) {
			this.outputSettings().prettyPrint( true ).indentAmount( indentFactor );
		}
		return this.outerHtml();
	}

	/**
	 * Returns a JSON representation of the document.
	 */
	public String toJSON() {
		return toJSON( false );
	}

	/**
	 * Returns a JSON representation of the document.
	 *
	 * @param prettyPrint If true, the JSON will be formatted with indentation for readability.
	 *                    If false, the JSON will be compact.
	 *
	 * @return A JSON string representation of the document.
	 *
	 * @throws RuntimeException if the conversion to JSON fails.
	 */
	public String toJSON( Boolean prettyPrint ) {
		try {
			Map<String, Object> tree = elementToMap( this );
			return JSONUtil.getJSONBuilder().asString( tree );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to convert BoxDocument to JSON", e );
		}
	}

	/**
	 * ----------------------------------------------------------------
	 * Private Methods
	 * ----------------------------------------------------------------
	 */

	/**
	 * Converts a Jsoup Element to a Map representation.
	 * This method recursively processes the element and its children,
	 * converting it into a structured Map format suitable for JSON serialization.
	 *
	 * @param element The Jsoup Element to convert.
	 *
	 * @return A Map representation of the Element.
	 */
	private Map<String, Object> elementToMap( Element element ) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put( "tag", element.tagName() );

		// Add attributes
		if ( !element.attributes().isEmpty() ) {
			Map<String, String> attrs = new LinkedHashMap<>();
			element.attributes().forEach( attr -> attrs.put( attr.getKey(), attr.getValue() ) );
			result.put( "attributes", attrs );
		}

		// Add children
		List<Object> children = new ArrayList<>();
		for ( Node child : element.childNodes() ) {
			if ( child instanceof Element childEl ) {
				children.add( elementToMap( childEl ) );
			} else if ( child instanceof TextNode textNode ) {
				String text = textNode.text().trim();
				if ( !text.isEmpty() ) {
					Map<String, String> textNodeMap = new LinkedHashMap<>();
					textNodeMap.put( "text", text );
					children.add( textNodeMap );
				}
			}
		}
		if ( !children.isEmpty() ) {
			result.put( "children", children );
		}

		return result;
	}

}
