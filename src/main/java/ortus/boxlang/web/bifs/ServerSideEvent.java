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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

@BoxBIF( alias = "sse", description = "Enables Server-Sent Events (SSE) streaming to the client." )
public class ServerSideEvent extends BIF {

	/**
	 * Target executor for async execution
	 */
	BoxExecutor targetExecutor;

	/**
	 * Constructor
	 */
	public ServerSideEvent() {
		super();
		declaredArguments	= new Argument[] {
		    new Argument( true, Argument.FUNCTION, Key.callback ),
		    new Argument( false, Argument.BOOLEAN, KeyDictionary.async, false ),
		    new Argument( false, Argument.NUMERIC, KeyDictionary.retry, 0 ),
		    new Argument( false, Argument.NUMERIC, KeyDictionary.keepAliveInterval, 0 ),
		    new Argument( false, Argument.NUMERIC, KeyDictionary.timeout, 0 ),
		    new Argument( false, Argument.STRING, KeyDictionary.cors, "" )
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
	 *
	 * // With timeout to prevent runaway connections
	 * sse(
	 *     callback = emit => {
	 *         for(var i = 1; i <= 100; i++) {
	 *             emit.send({ count: i });
	 *             sleep(1000);
	 *         }
	 *     },
	 *     async = true,
	 *     timeout = 30000  // Close after 30 seconds max
	 * );
	 *
	 * // With CORS enabled for cross-origin requests
	 * sse(
	 *     callback = emit => {
	 *         emit.send({ message: "Hello from API" });
	 *     },
	 *     cors = "*"  // or specific origin like "https://app.example.com"
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
	 * @argument.timeout Maximum time in milliseconds to wait for async execution to complete. If the timeout
	 *                   is exceeded, the connection will be gracefully closed. Default is 0 (no timeout).
	 *                   Only applies when async=true.
	 *
	 * @argument.cors Optional CORS origin to allow cross-origin requests. Use "*" for all origins, a specific
	 *                origin like "https://app.example.com", or leave empty (default) for no CORS headers.
	 *                When set, adds Access-Control-Allow-Origin and Access-Control-Allow-Credentials headers.
	 *
	 * @throws BoxRuntimeException if the callback is not a valid function
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Function				callback			= arguments.getAsFunction( Key.callback );
		boolean					async				= arguments.getAsBoolean( KeyDictionary.async );
		Integer					retry				= arguments.getAsInteger( KeyDictionary.retry );
		Integer					keepAliveInterval	= arguments.getAsInteger( KeyDictionary.keepAliveInterval );
		Integer					timeout				= arguments.getAsInteger( KeyDictionary.timeout );
		String					cors				= arguments.getAsString( KeyDictionary.cors );

		// Get the HTTP exchange from the context
		WebRequestBoxContext	requestContext		= context.getParentOfType( WebRequestBoxContext.class );
		IBoxHTTPExchange		exchange			= requestContext.getHTTPExchange();

		// Set CORS headers if specified
		if ( cors != null && !cors.isEmpty() ) {
			exchange.setResponseHeader( "Access-Control-Allow-Origin", cors );
			exchange.setResponseHeader( "Access-Control-Allow-Credentials", "true" );
		}

		// Set SSE response headers
		exchange.setResponseHeader( "Content-Type", "text/event-stream; charset=utf-8" );
		exchange.setResponseHeader( "Cache-Control", "no-cache, no-transform" );
		exchange.setResponseHeader( "Connection", "keep-alive" );
		// Disable nginx buffering
		exchange.setResponseHeader( "X-Accel-Buffering", "no" );
		// avoid gzip buffering in some stacks
		exchange.setResponseHeader( "Content-Encoding", "identity" );

		// Clear any existing buffer to prevent content corruption
		context.clearBuffer();

		// Create the SSE emitter
		SSEEmitter emitter = new SSEEmitter( retry, keepAliveInterval, requestContext );

		// Execute the callback
		if ( async ) {
			// Use CountDownLatch to block the request thread until async task completes
			// This prevents WebRequestExecutor from closing the connection prematurely
			CountDownLatch latch = new CountDownLatch( 1 );

			// Run in background thread with context preservation
			this.targetExecutor.submit(
			    () -> {
				    try {
					    context.invokeFunction( callback, new Object[] { emitter } );
				    } catch ( Exception e ) {
					    emitter.handleError( e );
				    } finally {
					    emitter.cleanup();
					    latch.countDown(); // Signal completion
				    }
			    }
			);

			// Block the request thread until the async task completes or timeout is reached
			try {
				boolean completed;
				if ( timeout > 0 ) {
					// Wait with timeout
					completed = latch.await( timeout, TimeUnit.MILLISECONDS );
					if ( !completed ) {
						// Timeout reached - gracefully close the connection
						runtime.getLoggingService().APPLICATION_LOGGER
						    .warn( "SSE async execution timed out after " + timeout + "ms. Closing connection gracefully." );
						emitter.close();
						// Still wait a bit for cleanup to finish
						latch.await( 1, TimeUnit.SECONDS );
					}
				} else {
					// Wait indefinitely
					latch.await();
				}
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
				emitter.close();
				throw new BoxRuntimeException( "SSE async execution was interrupted", e );
			}
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
