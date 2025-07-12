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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.XML;

public class HtmlToXmlTest extends ortus.boxlang.web.util.BaseWebTest {

	@DisplayName( "It can parse HTML content correctly to a BoxLang XML object" )
	@Test
	public void testHtmlParse() {

		// @formatter:off
		runtime.executeSource(
		    """
			    content	= "<html><head><title>My Page</title></head><body><h1>Hello World</h1></body></html>"
		    	result = htmlToXml( content )
				println( result )
		    """,
		    context );
		// @formatter:on

		var xml = ( XML ) variables.get( result );
		assertNotNull( xml );
	}

}
