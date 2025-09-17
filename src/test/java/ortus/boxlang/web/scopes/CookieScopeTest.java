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

package ortus.boxlang.web.scopes;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

public class CookieScopeTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				cookieScope;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		// Mock a connection
		IBoxHTTPExchange mockExchange = Mockito.mock( IBoxHTTPExchange.class );
		// Mock some objects which are used in the context
		when( mockExchange.getRequestCookies() ).thenReturn( new BoxCookie[ 0 ] );
		when( mockExchange.getRequestHeaderMap() ).thenReturn( new HashMap<String, String[]>() );
		when( mockExchange.getResponseWriter() ).thenReturn( new PrintWriter( OutputStream.nullOutputStream() ) );
		when( mockExchange.getRequestMethod() ).thenReturn( "GET" );
		context		= new WebRequestBoxContext( instance.getRuntimeContext(), mockExchange, "/" );
		variables	= context.getScopeNearby( VariablesScope.name );
		cookieScope	= context.getScopeNearby( CookieScope.name );
		cookieScope.clear();
	}

	@DisplayName( "It can get a value set earlier in the request" )
	@Test
	public void testSetAndGetInSameRequest() {
		instance.executeSource(
		    """
		          checkA = cookie.keyExists( "foo" );
		          cookie[ "foo" ] = "bar";
		          checkB = cookie.keyExists( "foo" );
		          result = cookie[ "foo" ];
		          cookie[ "bar"] = {
		          	"value": "baz",
		          	"samesite": "Lax",
		       	"secure": true,
		          };
		       checkC = cookie.keyExists( "bar" );
		    checkCResult = cookie[ "bar" ];
		          """,
		    context );
		Object checkA = variables.get( Key.of( "checkA" ) );
		assertThat( ( Boolean ) checkA ).isFalse();
		Object checkB = variables.get( Key.of( "checkB" ) );
		assertThat( ( Boolean ) checkB ).isTrue();
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "bar" );
		Object checkC = variables.get( Key.of( "checkC" ) );
		assertThat( ( Boolean ) checkC ).isTrue();
		assertThat( variables.getAsString( Key.of( "checkCResult" ) ) ).isEqualTo( "baz" );
	}

	@DisplayName( "It can set a cookie expiration value with a date string" )
	@Test
	public void testSetCookieExpirationAsDateStringInComponent() {
		// @formatter:off
		instance.executeSource( """
			cookieStruct = {
				"name": "foo",
				"value": "bar",
				"expires": "Fri, 31 Dec 2038 23:59:59 GMT",
			};
			bx:cookie attributeCollection=cookieStruct;
			check = cookie.keyExists( "foo" );
			checkResult = cookie[ "foo" ];
		""", context );
		// @formatter:on
		Object check = variables.get( Key.of( "check" ) );
		assertThat( ( Boolean ) check ).isTrue();
		assertThat( variables.getAsString( Key.of( "checkResult" ) ) ).isEqualTo( "bar" );
	}

}
