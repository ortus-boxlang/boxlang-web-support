/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.web.bifs;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

@BoxBIF( description = "Retrieves HTTP response data such as status, headers, content type, and cookies." )
public class GetHTTPResponseData extends BIF {

	private static final String	DEFAULT_CONTENT_TYPE	= "text/html";

	/**
	 * Constructor
	 */
	public GetHTTPResponseData() {
		super();
	}

	/**
	 * Retrieves HTTP response data including status code, headers, content type, and cookies.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return A struct containing the HTTP response data.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );
		IBoxHTTPExchange		exchange		= requestContext.getHTTPExchange();

		IStruct					headers			= new Struct( false );
		exchange.getResponseHeaderMap().forEach( ( key, values ) -> {
			if ( values != null && values.length > 0 ) {
				headers.put( key, values[ 0 ] );
			}
		} );

		IStruct cookies = new Struct( false );
		for ( BoxCookie cookie : exchange.getResponseCookies() ) {
			IStruct cookieStruct = Struct.ofNonConcurrent(
			    Key.of( "name" ), cookie.getName(),
			    Key.of( "value" ), cookie.getValue() != null ? cookie.getValue() : "",
			    Key.of( "path" ), cookie.getPath() != null ? cookie.getPath() : "",
			    Key.of( "domain" ), cookie.getDomain() != null ? cookie.getDomain() : "",
			    Key.of( "maxAge" ), cookie.getMaxAge() != null ? cookie.getMaxAge() : -1,
			    Key.of( "expires" ), cookie.getExpires(),
			    Key.of( "secure" ), cookie.isSecure(),
			    Key.of( "httpOnly" ), cookie.isHttpOnly(),
			    Key.of( "sameSite" ), cookie.isSameSite(),
			    Key.of( "sameSiteMode" ), cookie.getSameSiteMode() != null ? cookie.getSameSiteMode() : ""
			);
			cookies.put( cookie.getName(), cookieStruct );
		}

		String contentType = exchange.getResponseHeader( "Content-Type" );

		return Struct.ofNonConcurrent(
		    Key.of( "status" ), exchange.getResponseStatus(),
		    Key.of( "contentType" ), contentType != null ? contentType : DEFAULT_CONTENT_TYPE,
		    Key.of( "headers" ), headers,
		    Key.of( "cookies" ), cookies
		);
	}

}
