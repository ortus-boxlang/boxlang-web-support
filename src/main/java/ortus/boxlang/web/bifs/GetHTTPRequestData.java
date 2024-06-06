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
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

@BoxBIF
public class GetHTTPRequestData extends BIF {

	/**
	 * Constructor
	 */
	public GetHTTPRequestData() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "boolean", Key.includeBody, true ),
		};
	}

	/**
	 *
	 * Uppercase a string
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to uppercase
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );
		IBoxHTTPExchange		exchange		= requestContext.getHTTPExchange();

		IStruct					headers			= new Struct();
		exchange.getRequestHeaderMap().forEach( ( key, values ) -> {
			headers.put( key, values[ 0 ] );
		} );

		IStruct result = Struct.of(
		    Key.headers, headers,
		    Key.method, exchange.getRequestMethod(),
		    Key.protocol, exchange.getRequestProtocol()
		);

		if ( arguments.getAsBoolean( Key.includeBody ) ) {
			result.put( Key.content, requestContext.getRequestBody() );
		}

		return result;
	}

}
