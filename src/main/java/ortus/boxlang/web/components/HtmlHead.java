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

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( description = "Writes text to the head section of a generated HTML page. It is useful for embedding JavaScript code, or putting other HTML tags, such as meta, link, title, or base in an HTML page header. If there is no head section in the HTML page, the function will create one." )
public class HtmlHead extends Component {

	public HtmlHead() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.text, "string", Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Writes text to the head section of a generated HTML page. It is
	 * useful for embedding JavaScript code, or putting other HTML tags, such as meta, link,
	 * title, or base in an HTML page header.
	 *
	 * If there is no head section in the HTML page, the function will create one.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 *
	 * @atribute.text Text to add to the head area of an HTML page.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {

		// Get the text attribute
		String text = attributes.getAsString( Key.text );
		// Pass it to the BIF
		ortus.boxlang.web.bifs.HtmlHead.addToHead( context, text );

		return DEFAULT_RETURN;
	}
}
