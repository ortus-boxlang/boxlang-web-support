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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.web.scopes.FormScope;
import ortus.boxlang.web.scopes.URLScope;
import ortus.boxlang.web.util.BaseWebTest;

/**
 * Verifies that request parameters (URL and FORM) may not shadow real scopes such as
 * session and application. See BL-2802.
 */
public class ScopeShadowingTest extends BaseWebTest {

	private static final Key	SESSION			= Key.of( "session" );
	private static final Key	APPLICATION		= Key.of( "application" );
	private static final Key	PROBE_KEY		= Key.of( "probeKey" );
	private static final Key	IS_STRUCT		= Key.of( "isStructResult" );
	private static final Key	PROBE_VALUE		= Key.of( "probeValue" );
	private static final String	ATTACKER_VALUE	= "ATTACKER";

	private IScope				urlScope;
	private IScope				formScope;

	@BeforeEach
	public void setScopes() {
		urlScope	= context.getScope( URLScope.name );
		formScope	= context.getScope( FormScope.name );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It does not allow a URL or FORM parameter to shadow the session scope" )
	@Test
	public void testURLParamCannotShadowSessionScope() {
		urlScope.put( SESSION, ATTACKER_VALUE );
		formScope.put( SESSION, ATTACKER_VALUE );

		// @formatter:off
		runtime.executeSource(
		    """
				bx:application name="unit-test-shadow-session" sessionmanagement="true";
				session.probeKey = "REAL_SESSION";
				isStructResult = isStruct( session );
				probeValue = session.probeKey;
				hasProbeKey = structKeyExists( session, "probeKey" );
				urlValue = url.session;
				formValue = form.session;
		    """,
		    context );
		// @formatter:on

		assertThat( variables.getAsBoolean( IS_STRUCT ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasProbeKey" ) ) ).isTrue();
		assertThat( variables.getAsString( PROBE_VALUE ) ).isEqualTo( "REAL_SESSION" );
		// The actual URL and FORM scopes still contain the params
		assertThat( variables.getAsString( Key.of( "urlValue" ) ) ).isEqualTo( ATTACKER_VALUE );
		assertThat( variables.getAsString( Key.of( "formValue" ) ) ).isEqualTo( ATTACKER_VALUE );
		// The session scope itself is untouched
		assertThat( urlScope.get( SESSION ) ).isEqualTo( ATTACKER_VALUE );
		assertThat( context.getScope( SESSION ).getAsString( PROBE_KEY ) ).isEqualTo( "REAL_SESSION" );
	}

	@DisplayName( "It does not allow a URL or FORM parameter to shadow the application scope" )
	@Test
	public void testURLParamCannotShadowApplicationScope() {
		urlScope.put( APPLICATION, ATTACKER_VALUE );
		formScope.put( APPLICATION, ATTACKER_VALUE );

		// @formatter:off
		runtime.executeSource(
		    """
				bx:application name="unit-test-shadow-application";
				application.probeKey = "REAL_APPLICATION";
				isStructResult = isStruct( application );
				probeValue = application.probeKey;
				hasProbeKey = structKeyExists( application, "probeKey" );
				urlValue = url.application;
				formValue = form.application;
		    """,
		    context );
		// @formatter:on

		assertThat( variables.getAsBoolean( IS_STRUCT ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasProbeKey" ) ) ).isTrue();
		assertThat( variables.getAsString( PROBE_VALUE ) ).isEqualTo( "REAL_APPLICATION" );
		assertThat( variables.getAsString( Key.of( "urlValue" ) ) ).isEqualTo( ATTACKER_VALUE );
		assertThat( variables.getAsString( Key.of( "formValue" ) ) ).isEqualTo( ATTACKER_VALUE );
		assertThat( context.getScope( APPLICATION ).getAsString( PROBE_KEY ) ).isEqualTo( "REAL_APPLICATION" );
	}

	@DisplayName( "It resolves scope names to their scope objects when request params share their name" )
	@Test
	public void testScopeFindReturnsScopeObjectsForReservedKeys() {
		urlScope.put( SESSION, ATTACKER_VALUE );
		urlScope.put( APPLICATION, ATTACKER_VALUE );
		formScope.put( SESSION, ATTACKER_VALUE );
		formScope.put( APPLICATION, ATTACKER_VALUE );

		runtime.executeSource(
		    """
		    bx:application name="unit-test-shadow-lookup" sessionmanagement="true";
		      """,
		    context );

		// Unscoped "nearby" lookup, as used when reading an unscoped variable
		ScopeSearchResult sessionResult = context.scopeFindNearby( SESSION, null, false, false );
		assertThat( sessionResult.isScope() ).isTrue();
		assertThat( sessionResult.value() ).isEqualTo( context.getScope( SESSION ) );

		// Transcendent lookup
		ScopeSearchResult applicationResult = context.scopeFind( APPLICATION, null, false );
		assertThat( applicationResult.isScope() ).isTrue();
		assertThat( applicationResult.value() ).isEqualTo( context.getScope( APPLICATION ) );
	}

	@DisplayName( "It still resolves unscoped variables from the URL and FORM scopes" )
	@Test
	public void testNonScopeParamsStillResolveUnscoped() {
		urlScope.put( Key.of( "fromUrl" ), "url-value" );
		formScope.put( Key.of( "fromForm" ), "form-value" );

		// @formatter:off
		runtime.executeSource(
		    """
				urlResult = fromUrl;
				formResult = fromForm;
		    """,
		    context );
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "urlResult" ) ) ).isEqualTo( "url-value" );
		assertThat( variables.getAsString( Key.of( "formResult" ) ) ).isEqualTo( "form-value" );
	}

}
