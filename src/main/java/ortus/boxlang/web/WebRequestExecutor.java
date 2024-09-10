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
import ortus.boxlang.runtime.util.FQN;
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
	static private Set<String> validClassExtensions = Set.of( "cfc", "bx" );

	public static void execute( IBoxHTTPExchange exchange, String webRoot, Boolean manageFullReqestLifecycle ) {

		WebRequestBoxContext	context			= null;
		DynamicObject			trans			= null;
		FRTransService			frTransService	= null;
		BaseApplicationListener	appListener		= null;
		Throwable				errorToHandle	= null;
		String					requestString	= "";

		try {
			frTransService	= FRTransService.getInstance( manageFullReqestLifecycle );

			requestString	= exchange.getRequestURI();

			trans			= frTransService.startTransaction( "Web Request", requestString );
			context			= new WebRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext(), exchange, webRoot );
			// Set default content type to text/html
			exchange.setResponseHeader( "Content-Type", "text/html;charset=UTF-8" );
			// exchange.getResponseHeaders().put( new HttpString( "Content-Encoding" ),
			// "UTF-8" );
			context.loadApplicationDescriptor( new URI( requestString ) );
			appListener = context.getApplicationListener();

			String	ext			= "";
			Path	requestPath	= Path.of( requestString );
			String	fileName	= Path.of( requestString ).getFileName().toString().toLowerCase();
			if ( fileName.contains( "." ) ) {
				ext = fileName.substring( fileName.lastIndexOf( "." ) + 1 );
			}

			boolean result = appListener.onRequestStart( context, new Object[] { requestString } );
			if ( result ) {
				if ( validClassExtensions.contains( ext ) ) {
					Struct args = new Struct();
					// URL vars override form vars
					args.addAll( context.getScope( FormScope.name ) );
					args.addAll( context.getScope( URLScope.name ) );
					String	methodName		= null;
					String	returnFormat	= null;
					if ( args.containsKey( Key.method ) ) {
						methodName = args.get( Key.method ).toString();
						args.remove( Key.method );
					}
					if ( args.containsKey( Key.returnFormat ) ) {
						returnFormat = args.get( Key.returnFormat ).toString();
						args.remove( Key.returnFormat );
					}
					appListener.onClassRequest( context, new Object[] { new FQN( requestPath ).toString(), methodName, args, returnFormat } );
					// If return format was passed in the URL, then we know it was chosen. If none in the URL, one may have been set on the remote functions
					// annotations
					if ( returnFormat == null ) {
						returnFormat = Optional.ofNullable( context.getParentOfType( RequestBoxContext.class ).getAttachment( Key.returnFormat ) )
						    .map( Object::toString )
						    .orElse( "plain" );
					}

					// Set the content type based on the return format
					exchange.setResponseHeader( "Content-Type", switch ( returnFormat ) {
						case "json" -> "application/json;charset=UTF-8";
						case "xml", "wddx" -> "application/xml;charset=UTF-8";
						case "plain" -> "text/html;charset=UTF-8";
						case null, default -> "text/html;charset=UTF-8";
					} );

				} else {
					appListener.onRequest( context, new Object[] { requestString } );
				}
			}

			context.flushBuffer( false );
		} catch ( AbortException e ) {
			if ( appListener != null ) {
				try {
					appListener.onAbort( context, new Object[] { requestString } );
				} catch ( Throwable ae ) {
					// Opps, an error while handling onAbort
					errorToHandle = ae;
				}
			}
			if ( context != null )
				context.flushBuffer( true );
			if ( e.getCause() != null ) {
				// This will always be an instance of CustomException
				throw ( RuntimeException ) e.getCause();
			}
		} catch ( MissingIncludeException e ) {
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
			if ( context != null )
				context.flushBuffer( false );
		} catch ( Throwable e ) {
			errorToHandle = e;
		} finally {
			if ( appListener != null ) {
				try {
					appListener.onRequestEnd( context, new Object[] { requestString } );
				} catch ( Throwable e ) {
					// Opps, an error while handling onRequestEnd
					errorToHandle = e;
				}
			}
			if ( context != null )
				context.flushBuffer( false );

			if ( errorToHandle != null ) {
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
				context.flushBuffer( false );
			} else {
				exchange.flushResponseBuffer();
			}

			if ( frTransService != null ) {
				frTransService.endTransaction( trans );
			}
		}
	}

}
