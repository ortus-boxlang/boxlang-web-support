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
import java.util.Optional;
import java.util.Set;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.util.BoxFQN;
import ortus.boxlang.runtime.util.FRTransService;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.handlers.WebErrorHandler;
import ortus.boxlang.web.scopes.FormScope;
import ortus.boxlang.web.scopes.URLScope;

/**
 * I handle running a web request
 */
public class WebRequestExecutor {

	// TODO: make this configurable and move cf extensions to compat
	private static final Set<String>	VALID_REMOTE_REQUEST_EXTENSIONS	= Set.of( "cfc", "bx" );

	private static final String			DEFAULT_CONTENT_TYPE			= "text/html;charset=UTF-8";

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
			trans			= frTransService.startTransaction( "Web Request", requestString );

			// Load up the runtime, context and app listener
			context			= new WebRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext(), exchange, webRoot );
			RequestBoxContext.setCurrent( context );
			context.loadApplicationDescriptor( new URI( requestString ) );
			appListener = context.getApplicationListener();

			// Target Detection
			String	ext			= "";
			Path	requestPath	= Path.of( requestString );
			String	fileName	= Path.of( requestString ).getFileName().toString().toLowerCase();
			if ( fileName.contains( "." ) ) {
				ext = fileName.substring( fileName.lastIndexOf( "." ) + 1 );
			}

			// Pass through to the Application.bx onRequestStart method
			boolean result = appListener.onRequestStart( context, new Object[] { requestString } );

			// If we have a result, then we can continue
			if ( result ) {
				if ( VALID_REMOTE_REQUEST_EXTENSIONS.contains( ext ) ) {
					Struct args = new Struct();
					// URL vars override form vars
					args.addAll( context.getScope( FormScope.name ) );
					args.addAll( context.getScope( URLScope.name ) );
					if ( args.containsKey( Key.method ) ) {
						args.remove( Key.method );
					}
					if ( args.containsKey( Key.returnFormat ) ) {
						args.remove( Key.returnFormat );
					}

					// Fire it!
					appListener.onClassRequest( context,
					    new Object[] { new BoxFQN( requestPath ).toString(), args } );

					// This will have been set during the request, either by a URL variable, a param in the code, or a function annotation.
					String returnFormat = Optional.ofNullable( context.getRequestContext().getAttachment( Key.returnFormat ) )
					    .map( Object::toString )
					    .orElse( "plain" );

					// If the content type is set, the user has already set it, so don't override it
					// It's their responsibility to set it correctly
					ensureContentType( exchange, switch ( returnFormat.toLowerCase() ) {
						case "json" -> "application/json;charset=UTF-8";
						case "xml", "wddx" -> "application/xml;charset=UTF-8";
						case "plain" -> "text/html;charset=UTF-8";
						case null, default -> "text/html;charset=UTF-8";
					} );
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
			ensureContentType( exchange, DEFAULT_CONTENT_TYPE );

			if ( appListener != null ) {
				try {
					appListener.onAbort( context, new Object[] { requestString } );
				} catch ( Throwable ae ) {
					// Opps, an error while handling onAbort
					errorToHandle = ae;
				}
			}

			if ( context != null ) {
				context.flushBuffer( true );
			}

			if ( e.getCause() != null ) {
				// This will always be an instance of CustomException
				throw ( RuntimeException ) e.getCause();
			}
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

			if ( errorToHandle != null ) {
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
				} catch ( Throwable t ) {
					WebErrorHandler.handleError( t, exchange, context, frTransService, trans );
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

}
