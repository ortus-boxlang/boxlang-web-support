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

import java.util.Map;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.context.WebRequestBoxContext;

@BoxComponent( description = "Displays a custom HTML page when an error occurs." )
public class Error extends Component {

	public Error() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.template, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.exception, "string", "any", Set.of( Validator.NON_EMPTY ) )

			/*
			 * Not implemented arguments
			 * - mailto (doesn't make much sense and Lucee doesn't support)
			 * - type (BoxLang doesn't really have a demarcation between "exception", "validation", "request", and "monitor")
			 * monitor is deprecated in ColdFusion. We don't support the server-side validation feature, and all exceptions are part of a request,
			 * so I have no idea what the "request" type even means.
			 */
		};
	}

	/**
	 * Displays a custom HTML page when an error occurs. This lets you maintain a consistent look and feel among an application's functional and error
	 * pages.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @atribute.template The template to use for the error page.
	 * 
	 * @atribute.exception The exception type to handle with this error page.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String					template		= attributes.getAsString( Key.template );
		String					exception		= attributes.getAsString( Key.exception );

		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );
		Map<Key, String>		errorTemplates	= requestContext.getErrorTemplates();

		// Expand relative paths
		errorTemplates.put( Key.of( exception ), FileSystemUtil.expandPath( context, template ).absolutePath().toString() );

		return DEFAULT_RETURN;
	}
}
