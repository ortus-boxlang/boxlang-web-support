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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

public class URLScopeTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				urlScope;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

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
		urlScope	= context.getScopeNearby( URLScope.name );
	}

	@DisplayName( "It skips paraming a scope with values" )
	@Test
	public void testParamScopes() {
		instance.executeSource(
		    """
		    url.result = "foo";
		     	param url = {};
		     """,
		    context );
		assertThat( urlScope.getAsString( result ) ).isEqualTo( "foo" );
	}

}
