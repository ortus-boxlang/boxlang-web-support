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

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.util.BaseWebTest;

public class GetHTTPResponseDataTest extends BaseWebTest {

	@Test
	@DisplayName( "It returns default values when no response data has been set" )
	public void testDefaultValues() {
		runtime.executeSource( "result = getHTTPResponseData();", context );

		IStruct data = ( IStruct ) variables.get( result );

		assertThat( data ).isNotNull();
		assertThat( data.get( Key.of( "status" ) ) ).isEqualTo( 200 );
		assertThat( data.getAsString( Key.of( "contentType" ) ) ).isEqualTo( "text/html" );
		assertThat( data.get( Key.of( "headers" ) ) ).isInstanceOf( IStruct.class );
		assertThat( data.get( Key.of( "cookies" ) ) ).isInstanceOf( IStruct.class );
	}

	@Test
	@DisplayName( "It returns the current response status code" )
	public void testWithStatus() {
		mockExchange.setResponseStatus( 404 );

		runtime.executeSource( "result = getHTTPResponseData();", context );

		IStruct data = ( IStruct ) variables.get( result );
		assertThat( data.get( Key.of( "status" ) ) ).isEqualTo( 404 );
	}

	@Test
	@DisplayName( "It returns the Content-Type header as contentType" )
	public void testWithContentType() {
		mockExchange.setResponseHeader( "Content-Type", "application/json" );

		runtime.executeSource( "result = getHTTPResponseData();", context );

		IStruct data = ( IStruct ) variables.get( result );
		assertThat( data.getAsString( Key.of( "contentType" ) ) ).isEqualTo( "application/json" );
	}

	@Test
	@DisplayName( "It returns response headers in the headers struct" )
	public void testWithResponseHeaders() {
		mockExchange.setResponseHeader( "X-Custom-Header", "myValue" );
		mockExchange.setResponseHeader( "Cache-Control", "no-cache" );

		runtime.executeSource( "result = getHTTPResponseData();", context );

		IStruct	data	= ( IStruct ) variables.get( result );
		IStruct	headers	= ( IStruct ) data.get( Key.of( "headers" ) );

		assertThat( headers ).isNotNull();
		assertThat( headers.getAsString( Key.of( "X-Custom-Header" ) ) ).isEqualTo( "myValue" );
		assertThat( headers.getAsString( Key.of( "Cache-Control" ) ) ).isEqualTo( "no-cache" );
	}

	@Test
	@DisplayName( "It returns response cookies in the cookies struct" )
	public void testWithResponseCookies() {
		BoxCookie cookie = new BoxCookie( "sessionId", "abc123" );
		cookie.setPath( "/" );
		cookie.setHttpOnly( true );
		cookie.setSecure( true );
		mockExchange.addResponseCookie( cookie );

		runtime.executeSource( "result = getHTTPResponseData();", context );

		IStruct	data	= ( IStruct ) variables.get( result );
		IStruct	cookies	= ( IStruct ) data.get( Key.of( "cookies" ) );

		assertThat( cookies ).isNotNull();
		assertThat( cookies.containsKey( Key.of( "sessionId" ) ) ).isTrue();

		IStruct cookieData = ( IStruct ) cookies.get( Key.of( "sessionId" ) );
		assertThat( cookieData.getAsString( Key.of( "name" ) ) ).isEqualTo( "sessionId" );
		assertThat( cookieData.getAsString( Key.of( "value" ) ) ).isEqualTo( "abc123" );
		assertThat( cookieData.getAsString( Key.of( "path" ) ) ).isEqualTo( "/" );
		assertThat( cookieData.get( Key.of( "httpOnly" ) ) ).isEqualTo( true );
		assertThat( cookieData.get( Key.of( "secure" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "It returns multiple response cookies" )
	public void testWithMultipleCookies() {
		mockExchange.addResponseCookie( new BoxCookie( "cookieA", "valueA" ) );
		mockExchange.addResponseCookie( new BoxCookie( "cookieB", "valueB" ) );

		runtime.executeSource( "result = getHTTPResponseData();", context );

		IStruct	data	= ( IStruct ) variables.get( result );
		IStruct	cookies	= ( IStruct ) data.get( Key.of( "cookies" ) );

		assertThat( cookies.size() ).isEqualTo( 2 );
		assertThat( cookies.containsKey( Key.of( "cookieA" ) ) ).isTrue();
		assertThat( cookies.containsKey( Key.of( "cookieB" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "It returns empty structs by default for headers and cookies" )
	public void testHeadersAndCookiesDefaultToEmptyStructs() {
		runtime.executeSource( "result = getHTTPResponseData();", context );

		IStruct	data	= ( IStruct ) variables.get( result );

		IStruct	headers	= ( IStruct ) data.get( Key.of( "headers" ) );
		IStruct	cookies	= ( IStruct ) data.get( Key.of( "cookies" ) );

		assertThat( headers ).isNotNull();
		assertThat( cookies ).isNotNull();
		assertThat( headers.size() ).isEqualTo( 0 );
		assertThat( cookies.size() ).isEqualTo( 0 );
	}

}
