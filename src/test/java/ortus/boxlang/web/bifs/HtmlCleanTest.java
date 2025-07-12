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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HtmlCleanTest extends ortus.boxlang.web.util.BaseWebTest {

	@DisplayName( "It can clean HTML content correctly" )
	@Test
	public void testHtmlClean() {

		// @formatter:off
		runtime.executeSource(
		    """
			    // Invalid Content to clean
			    content = "<div><p>Test <b>bold</b> text</p><script>alert('XSS');</script></div>";
			    // Clean the content
			    result = HtmlClean( html: content );
			    println( result );
				 // Expected output: "<p>Test <b>bold</b> text</p>"
		    """,
		    context );
		// @formatter:on

		String	expected	= "<p>Test <b>bold</b> text</p>";
		String	actual		= ( String ) variables.get( result );
		assertThat( actual ).isEqualTo( expected );
	}

	@DisplayName( "Clean with another safe list" )
	@Test
	public void testHtmlCleanWithAnotherSafeList() {

		// @formatter:off
		runtime.executeSource(
		    """
			    // Invalid Content to clean
			    content = "<div><p>Test <b>bold</b> text</p><script>alert('XSS');</script></div>";
			    // Clean the content
			    result = HtmlClean( html: content, safeList: "relaxed" );
			    println( result );
				 // Expected output: "<p>Test <b>bold</b> text</p>"
		    """,
		    context );
		// @formatter:on

		String actual = ( String ) variables.get( result );
		assertThat( actual ).contains( "<p>Test <b>bold</b> text</p>" );
		assertThat( actual ).contains( "<div>" );
	}

}
