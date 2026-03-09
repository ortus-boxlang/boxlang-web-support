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

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.web.util.KeyDictionary;

@BoxComponent( allowsBody = true, description = "Writes text to the body section of a generated HTML page. When used with a body block, the rendered body content is buffered and prepended to the HTML body element at the end of the request. Supports append, write, read, reset and flush actions." )
public class HtmlBody extends Component {

	public HtmlBody() {
		super();
		declaredAttributes = new Attribute[] {
		    /**
		     * append (default): append text to the HTML body buffer.
		     * read: return the text currently in the HTML body buffer.
		     * reset: reset/remove text already in the HTML body buffer.
		     * write: write text to HTML body, overwriting any existing buffer content.
		     * flush: write the buffer to the current output stream and clear the buffer.
		     */
		    new Attribute( Key.action, "string", "append" ),
		    // The text to add to the 'body' area of an HTML page. When a component body is used, the rendered body is combined with this value.
		    new Attribute( Key.text, "string" ),
		    // ID of the snippet. When specified, prevents the same snippet from being added more than once per request.
		    new Attribute( Key.id, "string" ),
		    // Name of the variable to store the result in. Used with the read action. Defaults to "htmlbody".
		    new Attribute( Key.variable, "string" ),
		    // If true, the component works even within a silent block.
		    new Attribute( KeyDictionary.silent, "boolean" )
		};
	}

	/**
	 * Writes text to the body section of a generated HTML page.
	 *
	 * When a component body is present (i.e. used as a tag with body content), the rendered output of that body
	 * is captured and used as (or appended to) the {@code text} attribute value before being passed to the action.
	 * This mirrors Lucee's cfhtmlbody behavior where the tag body content is treated as the text to buffer.
	 *
	 * The buffered content is injected at the beginning of the HTML {@code <body>} element at the end of
	 * the request via an {@code onRequestEnd} interceptor, using Jsoup for HTML manipulation.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.action The action to perform on the HTML body buffer (append, write, read, reset, flush). Default: append.
	 *
	 * @attribute.text Text to add to the body area of an HTML page. Combined with body content when a body block is used.
	 *
	 * @attribute.id Optional unique ID. When specified, prevents the same snippet from being added more than once per request.
	 *
	 * @attribute.variable Name of the variable to receive the body buffer content. Used with the read action. Defaults to "htmlbody".
	 *
	 * @attribute.silent If true, the component works even within a silent block.
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String	action		= attributes.getAsString( Key.action );
		String	text		= attributes.getAsString( Key.text );
		String	id			= attributes.getAsString( Key.id );
		String	variable	= attributes.getAsString( Key.variable );

		if ( action == null ) {
			action = "append";
		}

		// Capture the rendered body content, if any, and combine it with the text attribute.
		// This mirrors Lucee's doAfterBody() behavior: tag body is treated as the text argument.
		if ( body != null ) {
			StringBuffer	contextBuffer	= context.getBuffer();
			int				beforeLength	= contextBuffer.length();
			BodyResult		bodyResult		= processBody( context, body );

			// Extract the rendered body text from the output buffer
			String			bodyText		= contextBuffer.substring( beforeLength );

			// Remove the body output from the normal output stream — it goes into the htmlBody buffer instead
			contextBuffer.setLength( beforeLength );

			// Concatenate: text attribute first, then tag body content
			if ( text != null && !text.isEmpty() ) {
				text = text + bodyText;
			} else {
				text = bodyText;
			}

			if ( bodyResult.isEarlyExit() ) {
				return bodyResult;
			}
		}

		// Delegate to the BIF's static dispatcher
		ortus.boxlang.web.bifs.HtmlBody.addToBody( context, action, text, variable, id );

		return DEFAULT_RETURN;
	}
}
