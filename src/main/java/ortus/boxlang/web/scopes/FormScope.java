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

import java.util.Arrays;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

/**
 * Form scope implementation in BoxLang
 */
public class FormScope extends BaseScope {

	public static Key		fieldNames	= Key.of( "fieldNames" );

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key	name		= Key.of( "form" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public FormScope( IBoxHTTPExchange exchange ) {
		super( FormScope.name );
		exchange.getRequestFormMap().forEach( ( key, value ) -> {
			this.put( Key.of( key ), Arrays.stream( value ).collect( Collectors.joining( "," ) ) );
		} );

		// Only for POST requests
		if ( exchange.getRequestMethod().equalsIgnoreCase( "POST" ) ) {
			// add form.fieldNames from our internal keys
			this.put( fieldNames, Arrays.stream( this.keySet().toArray() ).map( Object::toString ).collect( Collectors.joining( "," ) ) );
		}

	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */
}
