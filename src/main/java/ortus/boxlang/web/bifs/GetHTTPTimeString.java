
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

import java.time.ZoneId;
import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF

public class GetHTTPTimeString extends BIF {

	/**
	 * Constructor
	 */
	public GetHTTPTimeString() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date, Set.of( Validator.typeOneOf( "datetime", "string" ) ) )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		DateTime dateRef = null;
		try {
			dateRef = arguments.getAsDateTime( Key.date );
		} catch ( java.lang.ClassCastException e ) {
			throw new BoxRuntimeException(
			    String.format( "The provided date value [%s] could not be cast to a DateTime value", StringCaster.cast( arguments.get( Key.date ) ) ) );
		}
		DateTime converted = dateRef.convertToZone( ZoneId.of( "UTC" ) );
		converted.setFormat( "EEE, dd MMM yyyy HH:mm:ss z" );

		return converted.toString();

	}

}
