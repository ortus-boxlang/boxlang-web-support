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
package ortus.boxlang.web.handlers;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.exceptions.LockException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.util.FRTransService;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.util.ErrorPageTemplate;

/**
 * I handle default errors for a web request
 * 
 * Error pages are now generated using a template system for better maintainability.
 * The template can be found at: src/main/resources/templates/error-page.html
 * 
 * Template variables:
 * - {{VERSION_INFO}} - BoxLang version info (debug mode only)
 * - {{ERROR_CONTENT}} - Main error messages and exception details
 * - {{DEBUG_CONTENT}} - Tag context and stack trace (debug mode only)
 * 
 * TODO: allow custom error template path to be configured
 */
public class WebErrorHandler {

	/**
	 * Handle an error
	 *
	 * @param e              the error
	 * @param exchange       the exchange
	 * @param context        the context
	 * @param frTransService the FRTrans, if any
	 * @param trans          the transaction, if any
	 */
	public static void handleError( Throwable e, IBoxHTTPExchange exchange, WebRequestBoxContext context, FRTransService frTransService,
	    DynamicObject trans ) {
		try {
			e.printStackTrace();
			// Return 500 status code
			exchange.setResponseStatus( 500 );

			if ( frTransService != null ) {
				if ( e instanceof Exception ee ) {
					frTransService.errorTransaction( trans, ee );
				} else {
					frTransService.errorTransaction( trans, new Exception( e ) );
				}
			}

			if ( context != null ) {
				context.flushBuffer( true );
			}

			String errorOutput = buildErrorPage( e );

			if ( context != null ) {
				context.writeToBuffer( errorOutput, true );
			} else {
				// fail safe in case we errored out before creating the context
				exchange.getResponseWriter().append( errorOutput );
			}

		} catch ( Throwable t ) {
			// Something terrible happened and a blank page will probably be what the user sees.
			t.printStackTrace();
		}
	}

	/**
	 * Build the error page using template system
	 *
	 * @param e the error
	 *
	 * @return the error page string
	 */
	private static String buildErrorPage( Throwable e ) {
		BoxRuntime runtime = BoxRuntime.getInstance();
		
		// Create template and set version info
		ErrorPageTemplate template = ErrorPageTemplate.create();
		
		// Build version info
		String versionInfo = "";
		if ( runtime.inDebugMode() ) {
			versionInfo = "<small>v" + BoxRuntime.getInstance().getVersionInfo().get( Key.version ) + "</small>";
		}
		template.setVariable( "VERSION_INFO", versionInfo );
		
		// Build error content (the main error messages)
		StringBuilder errorContent = buildErrorContent( e, runtime );
		template.setVariable( "ERROR_CONTENT", errorContent.toString() );
		
		// Build debug content (tag context and stack trace) only in debug mode
		String debugContent = "";
		if ( runtime.inDebugMode() ) {
			debugContent = buildDebugContent( e ).toString();
		}
		template.setVariable( "DEBUG_CONTENT", debugContent );
		
		return template.render();
	}
	
	/**
	 * Build the main error content section
	 *
	 * @param e the error
	 * @param runtime the BoxRuntime instance
	 *
	 * @return StringBuilder with error content
	 */
	private static StringBuilder buildErrorContent( Throwable e, BoxRuntime runtime ) {
		StringBuilder errorContent = new StringBuilder();
		Throwable thisException = e;
		var errCount = 0;
		
		while ( thisException != null ) {
			errCount++;
			var cosClass = "bx-err-cos";
			if ( errCount % 2 == 0 ) {
				cosClass = "bx-err-cos-even";
			}

			if ( errCount == 1 ) {
				errorContent.append( "<div class=\"" )
				    .append( cosClass )
				    .append( "\">" )
				    .append( "<h2>" );
			} else if ( errCount > 1 && thisException.getCause() != null ) {
				errorContent.append( "<details open class=\"" )
				    .append( cosClass )
				    .append( "\">" )
				    .append( "<summary role=\"button\">" )
				    .append( "Caused By: " );
			} else {
				errorContent.append( "<div class=\"" )
				    .append( cosClass )
				    .append( "\">" )
				    .append( "<div class=\"bx-err-cos-title\"><strong>" )
				    .append( "Caused By: " );
			}
			
			// error title text
			if ( thisException instanceof BoxLangException ble ) {
				errorContent.append( escapeHTML( ble.getType() ) )
				    .append( " Error" );
			} else {
				errorContent.append( "An Error Occurred" );
			}
			
			// close error title elements
			if ( errCount == 1 ) {
				errorContent.append( "</h2>" );
			} else if ( errCount > 1 && thisException.getCause() != null ) {
				errorContent.append( "</summary>" );
			} else {
				errorContent.append( "</strong></div>" );
			}

			errorContent.append( "<div>" );

			// message
			if ( thisException.getMessage() != null && !thisException.getMessage().isEmpty() ) {
				errorContent.append( "<div class=\"bx-err-msg\">" )
				    // error icon
				    .append(
				        "<svg xmlns=\"http://www.w3.org/2000/svg\" height=\"24\" viewBox=\"0 -960 960 960\" width=\"34\"><path fill=\"red\" d=\"M480-280q17 0 28.5-11.5T520-320q0-17-11.5-28.5T480-360q-17 0-28.5 11.5T440-320q0 17 11.5 28.5T480-280Zm-40-160h80v-240h-80v240Zm40 360q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z\"/></svg>" )
				    .append( "<div style=\"text-wrap: pretty;\">" )
				    // text
				    .append( escapeHTML( thisException.getMessage() ) )
				    .append( "</div></div>" );
			}

			// Add debug-specific exception details if in debug mode
			if ( runtime.inDebugMode() ) {
				appendExceptionDetails( errorContent, thisException );
			}
			
			thisException = thisException.getCause();
		}

		// close the error divs
		for ( var i = 0; i < errCount; i++ ) {
			errorContent.append( "</div>" );
			if ( i == 0 || i == errCount - 1 ) {
				errorContent.append( "</div>" );
			} else {
				errorContent.append( "</details>" );
			}
		}
		
		return errorContent;
	}
	
	/**
	 * Append exception-specific details to the error content
	 *
	 * @param errorContent the StringBuilder to append to
	 * @param thisException the exception to process
	 */
	private static void appendExceptionDetails( StringBuilder errorContent, Throwable thisException ) {
		// error detail
		if ( thisException instanceof BoxLangException ble ) {
			if ( ble.getDetail() != null && !ble.getDetail().isEmpty() ) {
				errorContent.append( "<p><strong>Detail: </strong>" )
				    .append( ble.getDetail() )
				    .append( "</p>" );
			}
		}

		if ( thisException instanceof MissingIncludeException mie ) {
			errorContent.append( "Missing include: " )
			    .append( mie.getMissingFileName() )
			    .append( "<br>" );
		}

		if ( thisException instanceof BoxRuntimeException bre ) {
			Object				extendedInfo	= bre.getExtendedInfo();
			CastAttempt<String>	castAttempt		= StringCaster.attempt( extendedInfo );
			if ( castAttempt.wasSuccessful() && !castAttempt.get().isEmpty() ) {
				errorContent.append( "Extended Info: " )
				    .append( castAttempt.get() )
				    .append( "<br>" );
			}
		}

		if ( thisException instanceof CustomException ce ) {
			String errorCode = ce.getErrorCode();
			if ( errorCode != null && !errorCode.isEmpty() ) {
				errorContent.append( "Error Code: " )
				    .append( errorCode )
				    .append( "<br>" );
			}
		}

		if ( thisException instanceof DatabaseException dbe ) {
			String	nativeErrorCode	= dbe.getNativeErrorCode();
			String	SQLState		= dbe.getSQLState();
			String	SQL				= dbe.getSQL();
			String	queryError		= dbe.getQueryError();
			String	where			= dbe.getWhere();

			if ( nativeErrorCode != null && !nativeErrorCode.isEmpty() ) {
				errorContent.append( "Native Error Code: " )
				    .append( nativeErrorCode )
				    .append( "<br>" );
			}
			if ( SQLState != null && !SQLState.isEmpty() ) {
				errorContent.append( "SQL State: " )
				    .append( SQLState )
				    .append( "<br>" );
			}
			if ( SQL != null && !SQL.isEmpty() ) {
				errorContent.append( "SQL: " )
				    .append( SQL )
				    .append( "<br>" );
			}
			if ( queryError != null && !queryError.isEmpty() ) {
				errorContent.append( "Query Error: " )
				    .append( queryError )
				    .append( "<br>" );
			}
			if ( where != null && !where.isEmpty() ) {
				errorContent.append( "Where: " )
				    .append( where )
				    .append( "<br>" );
			}
		}

		if ( thisException instanceof LockException le ) {
			String	lockName		= le.getLockName();
			String	lockOperation	= le.getLockOperation();

			if ( lockName != null && !lockName.isEmpty() ) {
				errorContent.append( "Lock Name: " )
				    .append( lockName )
				    .append( "<br>" );
			}
			if ( lockOperation != null && !lockOperation.isEmpty() ) {
				errorContent.append( "Lock Operation: " )
				    .append( lockOperation )
				    .append( "<br>" );
			}
		}
	}
	
	/**
	 * Build the debug content section (tag context and stack trace)
	 *
	 * @param e the error
	 *
	 * @return StringBuilder with debug content
	 */
	private static StringBuilder buildDebugContent( Throwable e ) {
		StringBuilder debugContent = new StringBuilder();
		
		// Tag Context Panel
		debugContent.append( "<details open>" )
		    .append( "<summary role=\"button\">Tag Context</summary>" )
		    .append( "<div>" )
		    .append( "<table><thead>" )
		    .append( "<tr><th>File</th><th>Method</th></tr></thead><tbody>" );

		Array	tagContext	= ExceptionUtil.buildTagContext( e );
		var		tagCount	= 0;
		for ( var t : tagContext ) {
			tagCount++;
			IStruct	item		= ( IStruct ) t;
			Integer	lineNo		= item.getAsInteger( Key.line );
			String	fileName	= item.getAsString( Key.template );
			debugContent.append( "<tr><td>" );
			if ( lineNo > 0 ) {
				// trigger to toggle code display
				debugContent.append( "<button type=\"button\" class=\"btn-tgl\"" )
				    .append(
				        " onclick=\"toggleCode(this)\"" );
				if ( tagCount > 1 ) {
					debugContent.append( " aria-expanded=\"false\"" );
				} else {
					debugContent.append( " open aria-expanded=\"true\"" );
				}
				debugContent.append( " aria-label=\"Toggle code of line " )
				    .append( lineNo.toString() )
				    .append( "\"></button>" );
			}
			debugContent.append( " <span>" )
			    .append( escapeHTML( fileName ) )
			    .append( "</span>" );
			if ( lineNo > 0 ) {
				debugContent.append( ":<strong>" )
				    .append( lineNo.toString() )
				    .append( "</strong>" );
				if ( tagCount > 1 ) {
					debugContent.append( "<pre class=\"d-none\">" );
				} else {
					debugContent.append( "<pre aria-label=\"code around line " )
					    .append( lineNo.toString() )
					    .append( "\">" );
				}
				debugContent.append( item.getAsString( Key.codePrintHTML ) )
				    .append( "</pre>" );
			}
			debugContent.append( "</td>" )
			    .append( "<td>" )
			    .append( escapeHTML( item.getAsString( Key.id ) ) )
			    .append( "</td></tr>" );
		}
		debugContent.append( "</tbody></table>" )
		    .append( "</div></details>" );

		// Stack Trace Panel
		debugContent.append( "<details open>" )
		    .append( "<summary role=\"button\">Stack Trace</summary>" )
		    .append( "<div><pre style=\"text-wrap: pretty;\">" );

		debugContent.append( ExceptionUtil.getStackTraceAsString( e ).replaceAll( "\\((.*)\\)", "<strong class=\"highlight\">($1)</strong>" ) );

		debugContent.append( "</pre></div>" )
		    .append( "</details>" );
		
		return debugContent;
	}

	/**
	 * Escape HTML
	 *
	 * @param s the string to escape
	 *
	 * @return the escaped string
	 */
	private static String escapeHTML( String s ) {
		if ( s == null ) {
			return "";
		}
		return s.replace( "<", "&lt;" ).replace( ">", "&gt;" );
	}

}
