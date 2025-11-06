/**
 * [BoxLang]
 *
 * Copyright [2024] [Ortus Solutions, Corp]
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

@BoxBIF( description = "Sets the character encoding (character set) of Form and URL scope variable values." )
public class SetEncoding extends BIF {

	public static Key scope_name = Key.of( "scope_name" );

	/**
	 * Constructor
	 */
	public SetEncoding() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, scope_name ),
		    new Argument( true, Argument.STRING, Key.charset )
		};
	}

	/**
	 *
	 * Sets the character encoding (character set) of Form and URL scope variable values; used when the character encoding of the input to a form, or the
	 * character encoding of a URL, is not in UTF-8 encoding.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.scope_name The name of the scope to set the character encoding for.
	 *
	 * @argument.charset The character encoding to set.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// TODO: Implement SetEncoding BIF
		return null;
	}

}
