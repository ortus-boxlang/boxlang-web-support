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
package ortus.boxlang.web.bifs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.web.util.BoxDocument;

public class HtmlParseTest extends ortus.boxlang.web.util.BaseWebTest {

	@DisplayName( "It can parse HTML content correctly" )
	@Test
	public void testHtmlParse() {

		// @formatter:off
		runtime.executeSource(
		    """
			    content	= "<html><head><title>My Page</title></head><body><h1>Hello World</h1></body></html>"
		    	result = htmlParse( content )
		    """,
		    context );
		// @formatter:on

		BoxDocument doc = ( BoxDocument ) variables.get( result );
		assertEquals( "My Page", doc.title() );
		assertEquals( "<h1>Hello World</h1>", doc.body().html() );
		assertEquals( "Hello World", doc.body().text() );
		assertEquals( "UTF-8", doc.charset().name() );
	}

	@DisplayName( "BoxDocument can call the toXML() method" )
	@Test
	public void testBoxDocumentToXML() {
		BoxDocument	doc	= BoxDocument.fromDocument( BoxDocument.EMPTY_DOCUMENT );
		String		xml	= doc.toXML();
		assertThat( xml ).contains( "<html>" );
		assertThat( xml ).contains( "<head>" );
	}

	@DisplayName( "BoxDocument can call the toJson() method" )
	@Test
	public void testBoxDocumentToJson() {
		BoxDocument	doc		= BoxDocument.fromDocument( BoxDocument.EMPTY_DOCUMENT );
		String		json	= doc.toJSON();
		assertThat( json ).contains( "\"html\"" );
		assertThat( json ).contains( "\"head\"" );
	}

}
