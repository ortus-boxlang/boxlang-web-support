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
package ortus.boxlang.web.context;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.web.util.BaseWebTest;

/**
 * Covers WebRequestBoxContext.isWhitespaceCompressionEnabled().
 *
 * The text/event-stream cases are the important ones: an SSE frame is
 * terminated by a BLANK line, so collapsing consecutive newlines strips every
 * frame terminator and leaves a stream no client can parse.
 */
public class WhitespaceCompressionTest extends BaseWebTest {

	@DisplayName( "It never compresses a text/event-stream response" )
	@Test
	public void testSseIsNeverCompressed() {
		when( mockExchange.getResponseHeader( "Content-Type" ) ).thenReturn( "text/event-stream" );
		assertThat( context.isWhitespaceCompressionEnabled() ).isFalse();
	}

	@DisplayName( "It never compresses text/event-stream carrying a charset parameter" )
	@Test
	public void testSseWithCharsetIsNeverCompressed() {
		when( mockExchange.getResponseHeader( "Content-Type" ) ).thenReturn( "text/event-stream; charset=utf-8" );
		assertThat( context.isWhitespaceCompressionEnabled() ).isFalse();
	}

	@DisplayName( "It still compresses HTML" )
	@Test
	public void testHtmlIsStillCompressed() {
		when( mockExchange.getResponseHeader( "Content-Type" ) ).thenReturn( "text/html; charset=utf-8" );
		assertThat( context.isWhitespaceCompressionEnabled() ).isTrue();
	}

	@DisplayName( "It still compresses JSON" )
	@Test
	public void testJsonIsStillCompressed() {
		when( mockExchange.getResponseHeader( "Content-Type" ) ).thenReturn( "application/json" );
		assertThat( context.isWhitespaceCompressionEnabled() ).isTrue();
	}

	@DisplayName( "It does not compress other content types" )
	@Test
	public void testBinaryIsNotCompressed() {
		when( mockExchange.getResponseHeader( "Content-Type" ) ).thenReturn( "application/octet-stream" );
		assertThat( context.isWhitespaceCompressionEnabled() ).isFalse();
	}

	@DisplayName( "An Application.bx setting can turn compression off for HTML" )
	@Test
	public void testApplicationSettingCanDisableIt() {
		when( mockExchange.getResponseHeader( "Content-Type" ) ).thenReturn( "text/html" );
		assertThat( context.isWhitespaceCompressionEnabled() ).isTrue();

		context.getApplicationListener().getSettings().put( Key.whitespaceCompressionEnabled, false );
		context.clearConfigCache();

		assertThat( context.isWhitespaceCompressionEnabled() ).isFalse();
	}

	@DisplayName( "An Application.bx setting can turn compression on for HTML" )
	@Test
	public void testApplicationSettingCanEnableIt() {
		when( mockExchange.getResponseHeader( "Content-Type" ) ).thenReturn( "text/html" );

		context.getApplicationListener().getSettings().put( Key.whitespaceCompressionEnabled, true );
		context.clearConfigCache();

		assertThat( context.isWhitespaceCompressionEnabled() ).isTrue();
	}
}
