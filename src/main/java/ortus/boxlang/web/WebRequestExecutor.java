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
package ortus.boxlang.web;

import java.net.URI;
import java.nio.file.Path;
import java.security.Key;
import java.sql.Struct;
import java.util.Optional;
import java.util.Set;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.util.BoxFQN;
import ortus.boxlang.runtime.util.FRTransService;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.handlers.WebErrorHandler;
import ortus.boxlang.web.scopes.FormScope;
import ortus.boxlang.web.scopes.URLScope;
import ortus.boxlang.web.util.KeyDictionary;

/**
 * I handle running a web request
 */
public class WebRequestExecutor {

	// TODO: make this configurable and move cf extensions to compat
	private static final Set<String>	VALID_REMOTE_REQUEST_EXTENSIONS	= Set.of( "cfc", "bx" );

	public static final String			DEFAULT_CONTENT_TYPE			= "text/html;charset=UTF-8";

	public static final String			CONTENT_TYPE_HEADER				= "Content-Type";

	public static final String			DEFAULT_BINARY_CONTENT_TYPE		= "application/octet-stream";

	/**
	 * Execute a web request
	 *
	 * @param exchange The exchange object to use for the request
	 * @param webRoot  The web root of the application
	 */
	public static void execute( IBoxHTTPExchange exchange, String webRoot, Boolean manageFullReqestLifecycle ) {
		WebRequestBoxContext	context			= null;
		DynamicObject			trans			= null;
		FRTransService			frTransService	= null;
		BaseApplicationListener	appListener		= null;
		Throwable				errorToHandle	= null;
		String					requestString	= "";
		ClassLoader				oldClassLoader	= Thread.currentThread().getContextClassLoader();

		try {
			// Debug tracking
			frTransService	= FRTransService.getInstance( manageFullReqestLifecycle );
			requestString	= exchange.getRequestURI();

			// Target Detection Setup
			String				ext					= "";
			Path				requestPath			= Path.of( requestString );
			InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();

			// Load up the runtime, context and app listener
			context = new WebRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext(), exchange, webRoot );
			RequestBoxContext.setCurrent( context );

			context.loadApplicationDescriptor( new URI( requestString ) );
			appListener = context.getApplicationListener();

			// Allow interceptors to modify the request before we do anything with it.
			// This allows for modules with front controllers to execute inbound requests.
			// We perform this prior to any validation or processing, giving interceptors full control.
			if ( interceptorService.hasState( KeyDictionary.onWebExecutorRequest ) ) {
				IStruct interceptData = Struct.of(
				    KeyDictionary.updatedRequest, Struct.of( KeyDictionary.requestString, null ),
				    Key.context, context,
				    Key.of( "appListener" ), appListener,
				    KeyDictionary.requestString, requestString,
				    KeyDictionary.exchange, exchange
				);

				interceptorService.announce(
				    KeyDictionary.onWebExecutorRequest,
				    interceptData
				);

				// Check if an interceptor provided a new request string
				String updatedRequestString = interceptData.getAsStruct( KeyDictionary.updatedRequest ).getAsString( KeyDictionary.requestString );
				if ( updatedRequestString != null && !updatedRequestString.equals( requestString ) ) {
					requestString	= updatedRequestString;
					requestPath		= Path.of( requestString );
				}
			}

			// Start the transaction after any potential URI rewriting
			trans = frTransService.startTransaction( "Web Request", requestString );

			// Final target detection based on potentially updated requestString
			String fileName = requestPath.getFileName().toString().toLowerCase();
			if ( fileName.contains( "." ) ) {
				ext = fileName.substring( fileName.lastIndexOf( "." ) + 1 );
			}

			// Validate request URI for security issues
			validateRequestURI( requestString, fileName );

			// Pass through to the Application.bx onRequestStart method
			boolean result = appListener.onRequestStart( context, new Object[] { requestString } );

			// If we have a result, then we can continue
			if ( result ) {
				if ( VALID_REMOTE_REQUEST_EXTENSIONS.contains( ext ) ) {
					handleClassRemoteMethod( context, appListener, requestPath, exchange );
				} else {
					ensureContentType( exchange, DEFAULT_CONTENT_TYPE );
					appListener.onRequest( context, new Object[] { requestString } );
				}
			}

			// Any unhandled exceptions in the request, will skip onRequestEnd
			// This includes aborts, custom exceptions, and missing file includes
			appListener.onRequestEnd( context, new Object[] { requestString } );

			// Finally flush the buffer
			context.flushBuffer( false );
		}
		/**
		 * --------------------------------------------------------------------------------
		 * DEAL WITH BX ABORTS
		 * --------------------------------------------------------------------------------
		 */
		catch ( AbortException e ) {
			// We'll handle it below
			errorToHandle = e;
		}
		/**
		 * --------------------------------------------------------------------------------
		 * MISSING INCLUDES HANDLING
		 * --------------------------------------------------------------------------------
		 */
		catch ( MissingIncludeException e ) {
			ensureContentType( exchange, DEFAULT_CONTENT_TYPE );
			try {
				// A return of true means the error has been "handled". False means the default
				// error handling should be used
				if ( appListener == null
				    || !appListener.onMissingTemplate( context, new Object[] { e.getMissingFileName() } ) ) {
					// If the Application listener didn't "handle" it, then let the default handling
					// kick in below
					errorToHandle = e;
				}
			} catch ( Throwable t ) {
				// Opps, an error while handling the missing template error
				errorToHandle = t;
			}

			if ( context != null ) {
				context.flushBuffer( false );
			}
		}
		/**
		 * --------------------------------------------------------------------------------
		 * ALL OTHER ERRORS
		 * --------------------------------------------------------------------------------
		 */
		catch ( Throwable e ) {
			errorToHandle = e;
		}
		/**
		 * --------------------------------------------------------------------------------
		 * DEAL WITH ALL THE ERRORS HERE
		 * --------------------------------------------------------------------------------
		 */
		finally {
			ensureContentType( exchange, DEFAULT_CONTENT_TYPE );

			if ( context != null ) {
				context.flushBuffer( false );
			}

			// Was there an error produced above
			if ( errorToHandle != null ) {

				// If the error to handle is an abort, then take care of it
				if ( errorToHandle instanceof AbortException e ) {

					ensureContentType( exchange, DEFAULT_CONTENT_TYPE );

					if ( appListener != null ) {
						try {
							appListener.onAbort( context, new Object[] { requestString } );
						} catch ( AbortException aae ) {
							if ( aae.getCause() != null ) {
								errorToHandle = aae.getCause();
							}
						} catch ( Throwable ae ) {
							// Opps, an error while handling onAbort
							errorToHandle = ae;
						}
					}

					if ( context != null ) {
						context.flushBuffer( true );
					}

					if ( e.getCause() != null ) {
						errorToHandle = e.getCause();
					}
				}

				// This could still run EVEN IF the error above WAS an abort, as the onAbort could have thrown an error or the abort could have specified a
				// custom error to throw in its cause.
				if ( ! ( errorToHandle instanceof AbortException ) ) {
					// Log it to the exception logs no matter what
					BoxRuntime.getInstance()
					    .getLoggingService()
					    .getLogger( "exception" )
					    .error( errorToHandle.getMessage(), errorToHandle );

					try {
						// A return of true means the error has been "handled". False means the default
						// error handling should be used
						if ( appListener == null || !appListener.onError( context, new Object[] { errorToHandle, "" } ) ) {
							WebErrorHandler.handleError( errorToHandle, exchange, context, frTransService, trans );
						}
						// This is a failsafe in case the onError blows up.
					} catch ( AbortException ae ) {
						// If we abort during our onError, it's prolly too late to output a custom exception, so we'll ignore that logic in this path.
					} catch ( Throwable t ) {
						WebErrorHandler.handleError( t, exchange, context, frTransService, trans );
					}

				}

			}

			if ( context != null ) {
				context.flushBuffer( true );
			} else {
				exchange.flushResponseBuffer();
			}

			if ( frTransService != null ) {
				frTransService.endTransaction( trans );
			}
			context.shutdown();
			RequestBoxContext.removeCurrent();
			Thread.currentThread().setContextClassLoader( oldClassLoader );
		}
	}

	/**
	 * Ensure the content type is set if it is not already
	 *
	 * @param exchange           The exchange object to use for the request
	 * @param defaultContentType The default content type to use if none is set
	 */
	private static void ensureContentType( IBoxHTTPExchange exchange, String defaultContentType ) {
		var contentType = exchange.getResponseHeader( "Content-Type" );
		if ( contentType == null || contentType.isEmpty() ) {
			exchange.setResponseHeader( "Content-Type", defaultContentType );
		}
	}

	/**
	 * Validates the request URI for security issues including path traversal and application file access
	 *
	 * @param requestURI The request URI to validate
	 * @param fileName   The file name extracted from the request URI and lower cased.
	 *
	 * @throws BoxRuntimeException if the request URI contains security violations
	 */
	private static void validateRequestURI( String requestURI, String fileName ) {
		// Check for path traversal attempts
		if ( requestURI.equals( ".." ) ||
		    requestURI.contains( "../" ) ||
		    requestURI.contains( "..\\" ) ||
		    requestURI.contains( ";.." ) ||
		    requestURI.contains( "..;" ) ) {
			throw new BoxRuntimeException( "Invalid request URI: [" + requestURI + "]. Path traversal detected." );
		}

		// Block access to Application files
		if ( fileName.equals( "application.bx" ) ||
		    fileName.equals( "application.bxm" ) ||
		    fileName.equals( "application.bxs" ) ||
		    fileName.equals( "application.cfc" ) ||
		    fileName.equals( "application.cfm" ) ||
		    fileName.equals( "application.cfs" ) ) {
			throw new BoxRuntimeException( "Invalid request URI: [" + requestURI + "]. Access to Application file is forbidden." );
		}
	}

	/**
	 * Handles remote method invocation requests for valid extensions (BX classes, CFCs)
	 *
	 * @param context     The web request context
	 * @param appListener The application listener
	 * @param requestPath The request path as a Path object
	 * @param exchange    The HTTP exchange object
	 */
	private static void handleClassRemoteMethod(
	    WebRequestBoxContext context,
	    BaseApplicationListener appListener,
	    Path requestPath,
	    IBoxHTTPExchange exchange ) {

		// Build arguments from form and URL scopes
		Struct args = new Struct();
		// URL vars override form vars
		args.addAll( context.getScope( FormScope.name ) );
		args.addAll( context.getScope( URLScope.name ) );

		if ( args.containsKey( Key.argumentCollection ) ) {
			try {
				IStruct argCollection = StructCaster.cast( JSONUtil.fromJSON( StringCaster.cast( args.get( Key.argumentCollection ) ), true ) );
				args.addAll( argCollection );
				args.remove( Key.argumentCollection );
			} catch ( Exception e ) {
				throw new BoxRuntimeException( "Remote method invocation failed. Unable to parse argumentCollection JSON: " + e.getMessage(), e );
			}
		}

		// Remove framework-specific parameters
		if ( args.containsKey( Key.method ) ) {
			args.remove( Key.method );
		}
		if ( args.containsKey( Key.returnFormat ) ) {
			args.remove( Key.returnFormat );
		}

		// Invoke the remote method
		appListener.onClassRequest( context,
		    new Object[] { new BoxFQN( requestPath ).toString(), args } );

		// Determine return format from context attachment or default to plain
		String returnFormat = Optional.ofNullable( context.getRequestContext().getAttachment( Key.returnFormat ) )
		    .map( Object::toString )
		    .orElse( "plain" );

		// Set appropriate content type based on return format
		// If the content type is already set, the user has control and we don't override it
		ensureContentType( exchange, switch ( returnFormat.toLowerCase() ) {
			case "json" -> "application/json;charset=UTF-8";
			case "xml", "wddx" -> "application/xml;charset=UTF-8";
			case "plain" -> "text/html;charset=UTF-8";
			case null, default -> "text/html;charset=UTF-8";
		} );
	}

}