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
package ortus.boxlang.web.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.bifs.global.decision.IsSimpleValue;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

/**
 * Helper class for Server-Sent Events (SSE) streaming.
 *
 * This class wraps the HTTP response writer and provides methods for sending SSE-formatted
 * events to the client. It handles automatic keep-alive comments, error handling, and
 * proper cleanup of resources.
 *
 * Implements AutoCloseable for try-with-resources support:
 * 
 * <pre>
 * try (SSEEmitter emitter = new SSEEmitter(...)) {
 *     emitter.send("data");
 * } // Automatically calls close()
 * </pre>
 *
 * SSE Message Format:
 * - data: <message>\n\n
 * - event: <eventName>\ndata: <message>\nid: <id>\n\n
 * - :<comment>\n\n
 */
public class SSEEmitter implements AutoCloseable {

	/**
	 * --------------------------------------------------------------------------
	 * Static Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The BoxRuntime instance for logging and utilities
	 */
	private static final BoxRuntime		runtime				= BoxRuntime.getInstance();

	/**
	 * The target Scheduled Executor for scheduled tasks
	 */
	private static final BoxExecutor	scheduledExecutor	= runtime.getAsyncService().getExecutor( "scheduled-tasks" );

	/**
	 * The application logger
	 */
	private static final BoxLangLogger	appLogger			= runtime.getLoggingService().APPLICATION_LOGGER;

	/**
	 * --------------------------------------------------------------------------
	 * Emitter Properties
	 * --------------------------------------------------------------------------
	 */

	private final IBoxHTTPExchange		exchange;
	private final PrintWriter			writer;
	private final AtomicBoolean			closed				= new AtomicBoolean( false );
	private final AtomicBoolean			firstMessage;
	private final Integer				retry;
	private ScheduledFuture<?>			keepAliveTask;
	private final IBoxContext			context;

	/**
	 * Creates a new SSE emitter.
	 *
	 * @param exchange          The HTTP exchange for this request
	 * @param retry             The retry interval in milliseconds (0 = not sent)
	 * @param keepAliveInterval The interval for keep-alive comments in milliseconds (0 = disabled)
	 * @param context           The BoxLang context for logging
	 */
	public SSEEmitter( IBoxHTTPExchange exchange, Integer retry, Integer keepAliveInterval, IBoxContext context ) {
		Objects.requireNonNull( retry, "Retry interval cannot be null. Use 0 to disable." );
		Objects.requireNonNull( keepAliveInterval, "Keep-alive interval cannot be null. Use 0 to disable." );

		this.exchange		= exchange;
		this.writer			= exchange.getResponseWriter();
		this.retry			= retry;
		this.firstMessage	= new AtomicBoolean( true );
		this.context		= context;

		appLogger.debug( "SSE Emitter created - retry: " + retry + "ms, keepAlive: " + keepAliveInterval + "ms" );

		// Start keep-alive task if enabled
		if ( keepAliveInterval > 0 ) {
			startKeepAlive( keepAliveInterval );
		}
	}

	/**
	 * Send an SSE event to the client.
	 *
	 * Complex data types (structs, arrays) are automatically serialized to JSON.
	 * Simple values are sent as-is.
	 *
	 * @param data  The data to send
	 * @param event Optional event name
	 * @param id    Optional event ID
	 */
	public void send( Object data, String event, Object id ) {
		if ( closed.get() ) {
			appLogger.debug( "SSE send ignored - stream is closed" );
			return;
		}

		try {
			synchronized ( writer ) {
				// Send retry on first message only
				if ( firstMessage.getAndSet( false ) && retry > 0 ) {
					appLogger.debug( "SSE sending retry header: " + retry + "ms" );
					writer.write( "retry: " + retry + "\n" );
				}

				// Send event name if provided
				if ( event != null && !event.isEmpty() ) {
					appLogger.debug( "SSE sending event: " + event );
					writer.write( "event: " + event + "\n" );
				}

				// Send ID if provided
				if ( id != null ) {
					appLogger.debug( "SSE sending id: " + id );
					writer.write( "id: " + id + "\n" );
				}

				// Serialize and send data
				String dataString;
				if ( data instanceof String ) {
					dataString = ( String ) data;
				} else if ( IsSimpleValue.isSimpleValue( data ) ) {
					dataString = StringCaster.cast( data );
				} else {
					// Complex types -> JSON
					appLogger.debug( "SSE serializing complex data to JSON" );
					dataString = JSONUtil.getJSONBuilder().asString( data );
				}

				appLogger.debug( "SSE sending data: " + ( dataString.length() > 100 ? dataString.substring( 0, 100 ) + "..." : dataString ) );

				// Handle multi-line data (each line must be prefixed with "data: ")
				String[] lines = dataString.split( "\n" );
				for ( String line : lines ) {
					writer.write( "data: " + line + "\n" );
				}

				// End of message
				writer.write( "\n" );
				writer.flush();
				if ( writer.checkError() ) {
					appLogger.debug( "SSE client disconnected (writer error)" );
					close();
				}
			}
		} catch ( IOException e ) {
			appLogger.debug( "SSE client disconnected during send: " + e.getMessage() );
			// Client disconnected
			close();
		}
	}

	/**
	 * Send an SSE event with only data.
	 *
	 * @param data The data to send
	 */
	public void send( Object data ) {
		send( data, null, null );
	}

	/**
	 * Send an SSE comment (useful for keep-alive).
	 *
	 * Comments are lines starting with ':' and are ignored by the client.
	 *
	 * @param text The comment text
	 */
	public void comment( String text ) {
		// Ignore comments if closed
		if ( closed.get() ) {
			return;
		}

		try {
			synchronized ( writer ) {
				appLogger.debug( "SSE sending comment: " + text );
				writer.write( ":" + text + "\n\n" );
				writer.flush();
				if ( writer.checkError() ) {
					appLogger.debug( "SSE client disconnected (writer error)" );
					close();
				}
			}
		} catch ( Exception e ) {
			appLogger.error( "Failed to send SSE comment: " + e.getMessage(), e );
			// Client disconnected
			close();
		}
	}

	/**
	 * Close the SSE stream.
	 */
	public void close() {
		if ( this.closed.compareAndSet( false, true ) ) {
			appLogger.debug( "SSE stream closing" );
		}
		cleanup();
	}

	/**
	 * Check if the stream is closed (client disconnected or explicitly closed).
	 *
	 * @return true if closed, false otherwise
	 */
	public boolean isClosed() {
		return this.closed.get();
	}

	/**
	 * Handle errors that occur during streaming.
	 * Logs the error and sends an error event to the client.
	 *
	 * @param error The exception that occurred
	 */
	public void handleError( Exception error ) {
		// Log to application logger
		appLogger.error( "SSE Error: " + error.getMessage(), error );
		// Send to console as well
		error.printStackTrace();

		// Try to send error event to client
		if ( !this.closed.get() ) {
			try {
				var errorStruct = new java.util.LinkedHashMap<String, Object>();
				errorStruct.put( "error", error.getMessage().replace( "\"", "\\\"" ) );
				send(
				    errorStruct,
				    "error",
				    null
				);
			} catch ( Exception e ) {
				// If we can't send the error, just close
			}
		}

		close();
	}

	/**
	 * Cleanup resources (cancel keep-alive task, etc.).
	 */
	public void cleanup() {
		if ( this.keepAliveTask != null && !this.keepAliveTask.isCancelled() ) {
			appLogger.debug( "SSE cancelling keep-alive task" );
			keepAliveTask.cancel( true );
			this.keepAliveTask = null;
		}
		appLogger.debug( "SSE cleanup complete" );
	}

	/**
	 * Start the keep-alive task that sends periodic comments.
	 *
	 * @param intervalMs The interval in milliseconds
	 */
	private void startKeepAlive( int intervalMs ) {
		appLogger.debug( "SSE starting keep-alive task with interval: " + intervalMs + "ms" );
		this.keepAliveTask = scheduledExecutor.scheduledExecutor().scheduleAtFixedRate(
		    () -> {
			    if ( !closed.get() ) {
				    comment( "keep-alive" );
			    } else {
				    // Self-cancel if closed
				    if ( this.keepAliveTask != null ) {
					    this.keepAliveTask.cancel( false );
				    }
			    }
		    },
		    intervalMs,
		    intervalMs,
		    TimeUnit.MILLISECONDS
		);
	}

}
