/**
 * [BoxLang]
 *
 * Copyright [2024] [Ortus Solutions, Corp]
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
package ortus.boxlang.web.components;

import java.time.Duration;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCasterStrict;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.util.KeyDictionary;

@BoxComponent( description = "Defines web browser cookie variables, including expiration and security options." )
public class Cookie extends Component {

	public Cookie() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.value, "string", "" ),
		    new Attribute( Key.secure, "boolean" ),
		    new Attribute( Key.httpOnly, "boolean" ),
		    new Attribute( Key.expires, "any" ),
		    new Attribute( Key.samesite, "string" ),
		    new Attribute( Key.path, "string", "/" ),
		    new Attribute( Key.domain, "string" ),
		    new Attribute( KeyDictionary.encodevalue, "boolean", true )
		};
	}

	/**
	 * Defines web browser cookie variables, including expiration and security options.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 *
	 * @atribute.name Name of cookie variable. Converts cookie names
	 *                to all-uppercase. Cookie names set using this tag can
	 *                include any printable ASCII characters except commas,
	 *                semicolons or white space characters.
	 *
	 * @atribute.value Value to assign to cookie variable. Must be a string or
	 *                 variable that can be stored as a string.
	 *
	 * @atribute.secure If browser does not support Secure Sockets Layer (SSL)
	 *                  security, the cookie is not sent. To use the cookie, the
	 *                  page must be accessed using the https protocol.
	 *
	 * @atribute.httpOnly Specify whether cookie is http cookie or not
	 *
	 * @atribute.expires Expiration of cookie variable.
	 *
	 *                   - The default: the cookie expires when the user closes the
	 *                   browser, that is, the cookie is "session only".
	 *                   - A date or date/time object (for example, 10/09/97)
	 *                   - A number of days (for example, 10, or 100)
	 *                   - now: deletes cookie from client cookie.txt file
	 *                   (but does not delete the corresponding variable the
	 *                   Cookie scope of the active page).
	 *                   - never: The cookie expires in 30 years from the time it
	 *                   was created (effectively never in web years).
	 *
	 * @atribute.samesite Tells browsers when and how to fire cookies in first- or third-party situations. SameSite is used to identify whether or not to
	 *                    allow a cookie to be accessed.
	 *                    Values:
	 *                    - strict
	 *                    - lax
	 *                    - none
	 *
	 * @atribute.path URL, within a domain, to which the cookie applies;
	 *                typically a directory. Only pages in this path can use the
	 *                cookie. By default, all pages on the server that set the
	 *                cookie can access the cookie.
	 *
	 * @atribute.domain Domain in which cookie is valid and to which cookie content
	 *                  can be sent from the user's system. By default, the cookie
	 *                  is only available to the server that set it. Use this
	 *                  attribute to make the cookie available to other servers.
	 *
	 *                  Must start with a period. If the value is a subdomain, the
	 *                  valid domain is all domain names that end with this string.
	 *                  This attribute sets the available subdomains on the site
	 *                  upon which the cookie can be used.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String					name			= attributes.getAsString( Key._NAME );
		String					value			= attributes.getAsString( Key.value );
		Boolean					secure			= attributes.getAsBoolean( Key.secure );
		Boolean					httpOnly		= attributes.getAsBoolean( Key.httpOnly );
		Object					expires			= attributes.get( Key.expires );
		String					samesite		= attributes.getAsString( Key.samesite );
		String					path			= attributes.getAsString( Key.path );
		String					domain			= attributes.getAsString( Key.domain );
		Boolean					encodeValue		= attributes.getAsBoolean( KeyDictionary.encodevalue );

		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );

		IBoxHTTPExchange		exchange		= requestContext.getHTTPExchange();

		BoxCookie				cookieInstance	= new BoxCookie( name, value, encodeValue );

		if ( secure != null ) {
			cookieInstance.setSecure( secure );
		}

		if ( httpOnly != null ) {
			cookieInstance.setHttpOnly( httpOnly );
		}

		if ( expires != null ) {

			// try number first
			CastAttempt<Number> numberAttempt = NumberCaster.attempt( expires, false );
			if ( numberAttempt.wasSuccessful() ) {
				// convert days to seconds
				cookieInstance.setMaxAge( ( int ) ( numberAttempt.get().doubleValue() * 24 * 60 * 60 ) );
			} else if ( expires instanceof Duration expiresDuration ) {
				cookieInstance.setMaxAge( ( int ) expiresDuration.getSeconds() );
			} else {
				// Now try string
				Boolean				maxAgeSet		= false;
				CastAttempt<String>	stringAttempt	= StringCasterStrict.attempt( expires );
				if ( stringAttempt.wasSuccessful() ) {
					String stringValue = stringAttempt.get();
					if ( stringValue.equalsIgnoreCase( "now" ) ) {
						cookieInstance.setMaxAge( 0 );
						maxAgeSet = true;
					} else if ( stringValue.equalsIgnoreCase( "never" ) ) {
						cookieInstance.setMaxAge( 60 * 60 * 24 * 365 * 30 ); // 30 years
						maxAgeSet = true;
					}
				}
				if ( !maxAgeSet ) {
					// finally try date
					CastAttempt<DateTime> dateAttempt = DateTimeCaster.attempt( expires );
					if ( dateAttempt.wasSuccessful() ) {
						cookieInstance.setExpires( dateAttempt.get().toDate() );
					} else {
						throw new BoxValidationException( "Invalid cookie expiration type: " + expires.getClass().getName() );
					}
				}
			}
		}

		if ( samesite != null ) {
			cookieInstance.setSameSiteMode( samesite );
		}

		if ( path != null ) {
			cookieInstance.setPath( path );
		}

		if ( domain != null ) {
			cookieInstance.setDomain( domain );
		}

		// Add to the actual HTTP reponse
		exchange.addResponseCookie( cookieInstance );

		// Keep the cookie scope in sync
		requestContext.getCookieScope().put( name, value );

		return DEFAULT_RETURN;
	}
}
