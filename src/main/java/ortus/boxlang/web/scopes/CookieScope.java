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

import java.util.Date;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

/**
 * Cookie scope implementation in BoxLang
 */
public class CookieScope extends BaseScope {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key		name		= Key.of( "cookie" );
	private static final Key	maxAgeKey	= Key.of( "maxAge" );
	private static final Key	sameSiteKey	= Key.of( "sameSite" );

	protected IBoxHTTPExchange	exchange;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public CookieScope( IBoxHTTPExchange exchange ) {
		super( CookieScope.name );
		this.exchange = exchange;
		for ( var cookie : exchange.getRequestCookies() ) {
			this.put( Key.of( cookie.getName() ), cookie.getValue() );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Assign a value to a key
	 *
	 * @param key   The key to assign
	 * @param value The value to assign
	 */
	@Override
	public Object assign( IBoxContext context, Key key, Object value ) {
		CastAttempt<IStruct>	structAttempt	= StructCaster.attempt( value );
		String					cookieValue		= "";
		String					path			= "/";
		String					domain			= null;
		Integer					maxAge			= null;
		Date					expires			= null;
		boolean					secure			= false;
		boolean					httpOnly		= false;
		int						version			= 0;
		boolean					sameSite		= false;
		String					sameSiteMode	= null;

		if ( structAttempt.wasSuccessful() ) {
			IStruct cookieData = structAttempt.get();
			if ( cookieData.containsKey( Key.value ) ) {
				cookieValue = StringCaster.cast( cookieData.get( Key.value ) );
			}
			if ( cookieData.containsKey( Key.path ) ) {
				path = StringCaster.cast( cookieData.get( Key.path ) );
			}
			if ( cookieData.containsKey( Key.domain ) ) {
				domain = StringCaster.cast( cookieData.get( Key.domain ) );
			}
			if ( cookieData.containsKey( maxAgeKey ) ) {
				maxAge = IntegerCaster.cast( cookieData.get( maxAgeKey ) );
			}
			if ( cookieData.containsKey( Key.expires ) ) {
				expires = Date.from( DateTimeCaster.cast( cookieData.get( Key.expires ) ).toInstant() );
			}
			if ( cookieData.containsKey( Key.secure ) ) {
				secure = BooleanCaster.cast( cookieData.get( Key.secure ) );
			}
			if ( cookieData.containsKey( Key.httpOnly ) ) {
				httpOnly = BooleanCaster.cast( cookieData.get( Key.httpOnly ) );
			}
			if ( cookieData.containsKey( Key.version ) ) {
				version = IntegerCaster.cast( cookieData.get( Key.version ) );
			}
			if ( cookieData.containsKey( sameSiteKey ) ) {
				sameSiteMode	= StringCaster.cast( cookieData.get( sameSiteKey ) );
				sameSite		= true;
			}

		} else {
			// Anything other than a struct or string will errror ere
			cookieValue = StringCaster.cast( value );
		}

		this.put( key, cookieValue );
		// If the incoming value was just a struct, most of these will just be defaults
		exchange.addResponseCookie(
		    new BoxCookie( key.getName(), cookieValue )
		        .setPath( path )
		        .setDomain( domain )
		        .setMaxAge( maxAge )
		        .setSecure( secure )
		        .setVersion( version )
		        .setHttpOnly( httpOnly )
		        .setExpires( expires )
		        .setSameSite( sameSite )
		        .setSameSiteMode( sameSiteMode )
		);
		return value;
	}
}
