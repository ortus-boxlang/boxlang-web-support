
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.util.BaseWebTest;

public class SessionInvalidateTest extends BaseWebTest {

	IScope		variables;
	static Key	result	= new Key( "result" );

	@AfterAll
	public static void teardown() {
	}

	@BeforeEach
	public void setVariables() {
		variables = context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF SessionInvalidate" )
	@Test
	public void testBif() {
		// @formatter:off
		runtime.executeSource(
		    """
				bx:application name="unit-test-sm" sessionmanagement="true";
				session.foo = "bar";
				initialSession = duplicate( session );
		    	sleep( 1000 );
				SessionInvalidate();
				result = session;

				println( result.asString() )
			""",
		    context );
		// @formatter:on

		RequestBoxContext	requestContext	= context.getParentOfType( RequestBoxContext.class );
		IStruct				initialSession	= variables.getAsStruct( Key.of( "initialSession" ) );
		assertFalse( variables.getAsStruct( result ).containsKey( Key.of( "foo" ) ) );
		assertNotEquals( initialSession.getAsString( Key.of( "jsessionID" ) ), variables.getAsStruct( result ).getAsString( Key.of( "jsessionID" ) ) );
		assertFalse(
		    initialSession.getAsDateTime( Key.of( "timeCreated" ) ).equals( variables.getAsStruct( result ).getAsDateTime( Key.of( "timeCreated" ) ) ) );
		assertNotNull( variables.getAsStruct( result ).getAsString( Key.of( "jsessionID" ) ) );
		assertNotEquals( initialSession.getAsString( Key.of( "sessionid" ) ), variables.getAsStruct( result ).getAsString( Key.of( "sessionid" ) ) );

		// Test our session cookies in the response
		if ( requestContext instanceof WebRequestBoxContext webRequestContext ) {
			if ( webRequestContext.getHTTPExchange() instanceof ortus.boxlang.web.util.MockHTTPExchange mockExchange ) {
				// Debug output
				// System.out.println( "Request Cookies: " + Stream.of( mockExchange.getRequestCookies() ).map( c -> c.toString() ).toList() );
				// System.out
				// .println( "Response Cookies: " + Stream.of( mockExchange.getResponseCookies() ).map( c -> c.toString() ).toList() );
				BoxCookie[] responseCookies = mockExchange.getResponseCookies();
				assertThat( responseCookies.length ).isEqualTo( 2 );
				assertThat( responseCookies[ 0 ].getValue() ).isEqualTo( initialSession.getAsString( Key.of( "jsessionID" ) ) );
				assertThat( responseCookies[ 0 ].getMaxAge() ).isEqualTo( 0 );
				assertThat( responseCookies[ 1 ].getValue() ).isEqualTo( variables.getAsStruct( result ).getAsString( Key.of( "jsessionID" ) ) );
			}
		}
	}

}
