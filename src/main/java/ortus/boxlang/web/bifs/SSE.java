/**
 * [BoxLang]
 *
 * Copyright [2025] [Ortus Solutions, Corp]
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

import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.util.KeyDictionary;
import ortus.boxlang.web.util.SSEEmitter;

@BoxBIF
public class SSE extends BIF {

	/**
	 * Target executor for async execution
	 */
	BoxExecutor targetExecutor;

	/**
	 * Constructor
	 */
	public SSE() {
		super();
		declaredArguments	= new Argument[] {
		    new Argument( true, Argument.FUNCTION, Key.callback ),
		    new Argument( false, Argument.BOOLEAN, KeyDictionary.async, false ),
		    new Argument( false, Argument.NUMERIC, KeyDictionary.retry, 0 ),
		    new Argument( false, Argument.NUMERIC, KeyDictionary.keepAliveInterval, 0 )
		};
		this.targetExecutor	= runtime.getAsyncService().getExecutor( "io-tasks" );
	}

	/**
	 * Enables Server-Sent Events (SSE) streaming to the client.
	 *
	 * This BIF sets up an SSE connection and invokes a callback function that receives an emitter object
	 * for sending events to the client. SSE provides a simple, HTTP-based protocol for server-to-client
	 * real-time communication.
	 *
	 * <h2>Usage Examples</h2>
	 *
	 * <pre>
	 * // Simple counter
	 * sse( emit => {
	 *     for(var i = 1; i <= 10; i++) {
	 *         emit.send(i);
	 *         sleep(1000);
	 *     }
	 *     emit.close();
	 * });
	 *
	 * // Structured events with metadata
	 * sse( emit => {
	 *     emit.send(
	 *         data = { user: "John", action: "login" },
	 *         event = "userEvent",
	 *         id = 1
	 *     );
	 * });
	 *
	 * // Long-lived stream with keep-alive
	 * sse(
	 *     callback =  emit => {
	 *         while(!emit.isClosed()) {
	 *             emit.send(getLatestData());
	 *             sleep(1000);
	 *         }
	 *     },
	 *     keepAliveInterval = 30000,
	 *     async = true
	 * );
	 * </pre>
	 *
	 * <h2>Emitter Methods</h2>
	 * <ul>
	 * <li><strong>send(data, [event], [id])</strong> - Send an SSE event. Complex data is auto-serialized to JSON.</li>
	 * <li><strong>comment(text)</strong> - Send an SSE comment (useful for keep-alive)</li>
	 * <li><strong>close()</strong> - Close the SSE stream</li>
	 * <li><strong>isClosed()</strong> - Check if the client has disconnected</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.callback A closure/lambda that receives an emitter object for sending SSE events.
	 *
	 * @argument.async If true, the callback runs in a background thread (non-blocking). If false (default),
	 *                 the callback blocks the current thread until completion.
	 *
	 * @argument.retry The number of milliseconds the client should wait before attempting to reconnect
	 *                 after a connection drop. Default is 0 (not sent). Only sent on the first message.
	 *
	 * @argument.keepAliveInterval If greater than 0, automatically sends keep-alive comments at this interval
	 *                             (in milliseconds) to prevent connection timeouts. Default is 0 (disabled).
	 *
	 * @throws BoxRuntimeException if the callback is not a valid function
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Function				callback			= arguments.getAsFunction( Key.callback );
		boolean					async				= arguments.getAsBoolean( KeyDictionary.async );
		Integer					retry				= arguments.getAsInteger( KeyDictionary.retry );
		Integer					keepAliveInterval	= arguments.getAsInteger( KeyDictionary.keepAliveInterval );

		// Get the HTTP exchange from the context
		WebRequestBoxContext	requestContext		= context.getParentOfType( WebRequestBoxContext.class );
		IBoxHTTPExchange		exchange			= requestContext.getHTTPExchange();

		// Set SSE response headers
		exchange.setResponseHeader( "Content-Type", "text/event-stream" );
		exchange.setResponseHeader( "Cache-Control", "no-cache" );
		exchange.setResponseHeader( "Connection", "keep-alive" );
		exchange.setResponseHeader( "X-Accel-Buffering", "no" ); // Disable nginx buffering

		// Clear any existing buffer to prevent content corruption
		context.clearBuffer();

		// Create the SSE emitter
		SSEEmitter emitter = new SSEEmitter( exchange, retry, keepAliveInterval, context );

		// Execute the callback
		if ( async ) {
			// Run in background thread with context preservation
			this.targetExecutor.submit(
			    () -> {
				    try {
					    context.invokeFunction( callback, new Object[] { emitter } );
				    } catch ( Exception e ) {
					    emitter.handleError( e );
				    } finally {
					    emitter.cleanup();
				    }
			    }
			);
			// Return immediately for async execution
			return null;
		} else {
			// Execute synchronously (blocking)
			try {
				context.invokeFunction( callback, new Object[] { emitter } );
			} catch ( Exception e ) {
				emitter.handleError( e );
			} finally {
				emitter.cleanup();
			}
		}

		// Throw AbortException to prevent any further output
		throw new AbortException();
	}
}
