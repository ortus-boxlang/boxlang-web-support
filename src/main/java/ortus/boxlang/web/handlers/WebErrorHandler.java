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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

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

/**
 * I handle default errors for a web request
 * TODO: allow custom error template to be configured
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
	 * Build the error page
	 *
	 * @param e the error
	 *
	 * @return the error page string
	 */
	private static String buildErrorPage( Throwable e ) {
		BoxRuntime	runtime		= BoxRuntime.getInstance();

		// If an HTML template is present in resources, inject dynamic content into placeholders.
		String		template	= loadTemplate();

		if ( template != null ) {
			String	versionInfo		= buildVersionInfoHTML( runtime );
			String	errorContent	= buildErrorContentHTML( e, runtime );

			String	out				= template
			    .replace( "{{VERSION_INFO}}", versionInfo )
			    .replace( "{{ERROR_CONTENT}}", errorContent );

			return out;
		}

		// Fallback: if template not found, return empty string (or could build a minimal error page)
		return "";
	}

	/**
	 * Build the version info HTML string for display in the error header
	 *
	 * @param runtime the BoxRuntime instance
	 *
	 * @return the version HTML string (e.g., "<small>v1.0.0</small>") or empty string if not in debug mode
	 */
	private static String buildVersionInfoHTML( BoxRuntime runtime ) {
		if ( runtime.inDebugMode() ) {
			Object version = BoxRuntime.getInstance().getVersionInfo().get( Key.version );
			return "<small>v" + ( version != null ? version.toString() : "unknown" ) + "</small>";
		}
		return "";
	}

	/**
	 * Build the error content HTML string (exceptions, tag context, stack trace)
	 *
	 * @param e       the exception
	 * @param runtime the BoxRuntime instance
	 *
	 * @return the error content HTML string
	 */
	private static String buildErrorContentHTML( Throwable e, BoxRuntime runtime ) {
		StringBuilder	errorOutput		= new StringBuilder();

		Throwable		thisException	= e;
		// track error count
		var				errCount		= 0;
		while ( thisException != null ) {
			errCount++;
			var cosClass = "bx-err-cos";
			if ( errCount % 2 == 0 ) {
				cosClass = "bx-err-cos-even";
			}

			if ( errCount == 1 ) {
				errorOutput.append( "<div class=\"" )
				    .append( cosClass )
				    .append( "\">" )
				    .append( "<h2>" );
			} else if ( errCount > 1 && thisException.getCause() != null ) {
				errorOutput.append( "<details open class=\"" )
				    .append( cosClass )
				    .append( "\">" )
				    .append( "<summary role=\"button\">" )
				    .append( "Caused By: " );
			} else {
				errorOutput.append( "<div class=\"" )
				    .append( cosClass )
				    .append( "\">" )
				    .append( "<div class=\"bx-err-cos-title\"><strong>" )
				    .append( "Caused By: " );
				;
			}
			// error title text
			if ( thisException instanceof BoxLangException ble ) {
				errorOutput.append( escapeHTML( ble.getType() ) )
				    .append( " Error" );
			} else {
				errorOutput.append( "An Error Occurred" );
			}

			// close error title elements
			if ( errCount == 1 ) {
				errorOutput.append( "</h2>" );
			} else if ( errCount > 1 && thisException.getCause() != null ) {
				errorOutput.append( "</summary>" );
			} else {
				errorOutput.append( "</strong></div>" );
			}

			errorOutput.append( "<div>" );

			// message
			if ( thisException.getMessage() != null && !thisException.getMessage().isEmpty() ) {
				errorOutput.append( "<div class=\"bx-err-msg\">" )
				    // erro icon
				    .append(
				        "<svg xmlns=\"http://www.w3.org/2000/svg\" height=\"24\" viewBox=\"0 -960 960 960\" width=\"34\"><path fill=\"red\" d=\"M480-280q17 0 28.5-11.5T520-320q0-17-11.5-28.5T480-360q-17 0-28.5 11.5T440-320q0 17 11.5 28.5T480-280Zm-40-160h80v-240h-80v240Zm40 360q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z\"/></svg>" )
				    .append( "<div style=\"text-wrap: pretty;\">" )
				    // text
				    .append( preserveWhitespace( escapeHTML( thisException.getMessage() ) ) )
				    .append( "</div></div>" );
			}

			// If not in debug mode, just show the error message
			if ( !runtime.inDebugMode() ) {
				errorOutput.append( "</div>" );
				if ( errCount == 1 ) {
					errorOutput.append( "</div>" );
				} else {
					errorOutput.append( "</details>" );
				}
				break;
			}

			/**
			 * ------------------------------------------------------------------------------
			 * DEBUG MODE DATA
			 * In debug mode, we show more details about the error
			 * ------------------------------------------------------------------------------
			 */

			// error detail
			if ( thisException instanceof BoxLangException ble ) {
				if ( ble.getDetail() != null && !ble.getDetail().isEmpty() ) {
					errorOutput.append( "<p><strong>Detail: </strong>" )
					    .append( preserveWhitespace( escapeHTML( ble.getDetail() ) ) )
					    .append( "</p>" );
				}
			}

			if ( thisException instanceof MissingIncludeException mie ) {
				errorOutput.append( "Missing include: " )
				    .append( escapeHTML( mie.getMissingFileName() ) )
				    .append( "<br>" );
			}

			if ( thisException instanceof BoxRuntimeException bre ) {
				Object				extendedInfo	= bre.getExtendedInfo();
				CastAttempt<String>	castAttempt		= StringCaster.attempt( extendedInfo );
				if ( castAttempt.wasSuccessful() && !castAttempt.get().isEmpty() ) {
					errorOutput.append( "Extended Info: " )
					    .append( preserveWhitespace( escapeHTML( castAttempt.get() ) ) )
					    .append( "<br>" );
				}
			}

			if ( thisException instanceof CustomException ce ) {
				String errorCode = ce.getErrorCode();
				if ( errorCode != null && !errorCode.isEmpty() ) {
					errorOutput.append( "Error Code: " )
					    .append( escapeHTML( errorCode ) )
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
					errorOutput.append( "Native Error Code: " )
					    .append( escapeHTML( nativeErrorCode ) )
					    .append( "<br>" );
				}
				if ( SQLState != null && !SQLState.isEmpty() ) {
					errorOutput.append( "SQL State: " )
					    .append( escapeHTML( SQLState ) )
					    .append( "<br>" );
				}
				if ( SQL != null && !SQL.isEmpty() ) {
					errorOutput.append( "SQL: " )
					    .append( preserveWhitespace( escapeHTML( SQL ) ) )
					    .append( "<br>" );
				}
				if ( queryError != null && !queryError.isEmpty() ) {
					errorOutput.append( "Query Error: " )
					    .append( preserveWhitespace( escapeHTML( queryError ) ) )
					    .append( "<br>" );
				}
				if ( where != null && !where.isEmpty() ) {
					errorOutput.append( "Where: " )
					    .append( preserveWhitespace( escapeHTML( where ) ) )
					    .append( "<br>" );
				}
			}

			if ( thisException instanceof LockException le ) {
				String	lockName		= le.getLockName();
				String	lockOperation	= le.getLockOperation();

				if ( lockName != null && !lockName.isEmpty() ) {
					errorOutput.append( "Lock Name: " )
					    .append( escapeHTML( lockName ) )
					    .append( "<br>" );
				}
				if ( lockOperation != null && !lockOperation.isEmpty() ) {
					errorOutput.append( "Lock Operation: " )
					    .append( escapeHTML( lockOperation ) )
					    .append( "<br>" );
				}
			}
			thisException = thisException.getCause();
		}

		// let's close the error divs
		for ( var i = 0; i < errCount; i++ ) {
			errorOutput.append( "</div>" );
			if ( i == 0 || i == errCount - 1 ) {
				errorOutput.append( "</div>" );
			} else {
				errorOutput.append( "</details>" );
			}
		}

		// Tag Context Panel
		errorOutput.append( "<details open>" )
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
			errorOutput.append( "<tr><td>" );
			if ( lineNo > 0 ) {
				// trigger to toggle code display
				errorOutput.append( "<button type=\"button\" class=\"btn-tgl\"" )
				    .append(
				        " onclick=\"this.getAttribute('aria-expanded')=='true'?this.setAttribute('aria-expanded', false):this.setAttribute('aria-expanded', true);this.toggleAttribute('open');this.parentElement.getElementsByTagName('pre')[0].classList.toggle('d-none')\"" );
				if ( tagCount > 1 ) {
					errorOutput.append( "aria-expanded=\"false\"" );
				} else {
					errorOutput.append( " open aria-expanded=\"true\"" );
				}
				errorOutput.append( "aria-label=\"Toggle code of line " )
				    .append( lineNo.toString() )
				    .append( "\"></button>" );
			}
			errorOutput.append( " <span>" )
			    .append( fileName )
			    .append( "</span>" );
			if ( lineNo > 0 ) {
				errorOutput.append( ":<strong>" )
				    .append( lineNo.toString() )
				    .append( "</strong>" );
				if ( tagCount > 1 ) {
					errorOutput.append( "<pre class=\"d-none\">" );
				} else {
					errorOutput.append( "<pre aria-label=\"code around line " )
					    .append( lineNo.toString() )
					    .append( "\">" );
				}
				errorOutput.append( item.getAsString( Key.codePrintHTML ) )
				    .append( "</pre>" );
			}
			errorOutput.append( "</td>" )
			    .append( "<td>" )
			    .append( escapeHTML( item.getAsString( Key.id ) ) )
			    .append( "</td></tr>" );
		}
		errorOutput.append( "</tbody></table>" )
		    .append( "</div></details>" );

		// Stack Trace Panel
		errorOutput.append( "<details open>" )
		    .append( "<summary role=\"button\">Stack Trace</summary>" )
		    .append( "<div><pre style=\"text-wrap: pretty;\">" );

		errorOutput.append( ExceptionUtil.getStackTraceAsString( e ).replaceAll( "\\((.*)\\)", "<strong class=\"highlight\">($1)</strong>" ) );

		errorOutput.append( "</pre></div>" )
		    .append( "</details>" );

		return errorOutput.toString();
	}

	/**
	 * Load the HTML template from resources
	 * 
	 * @return the template string
	 */
	private static String loadTemplate() {
		try {
			// Get a pipe to the file
			InputStream inputStream = WebErrorHandler.class.getResourceAsStream( "/templates/error.html" );

			// Check if file exists or not
			if ( inputStream == null ) {
				System.err.println( "Error template not found in file." );
				return null;
			}

			// convert byes to characters
			InputStreamReader	reader			= new InputStreamReader( inputStream, StandardCharsets.UTF_8 );

			// Read efficiently with buffer
			BufferedReader		bufferedReader	= new BufferedReader( reader );

			// Read all lines and join them
			String				template		= bufferedReader.lines().collect( Collectors.joining( "\n" ) );
			return template;

		} catch ( Exception e ) {
			System.err.println( "Error loading template: " + e.getMessage() );
			return null;

		}
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

	/**
	 * Convert actual line breaks to <br>
	 * and spaces to &nbsp;
	 * Only call this after escaping HTML
	 */
	private static String preserveWhitespace( String s ) {
		if ( s == null ) {
			return "";
		}
		return s.replace( "\n", "<br>" ).replace( " ", "&nbsp;" );
	}

	public static void main( String[] args ) {
		System.out.println();
		System.out.println("Testing template loading and placeholder replacement.");

		// Test 1: Can we load the template?
		String template = loadTemplate();

		if ( template != null ) {
			System.out.println("Template loaded successfully.");
		} else {
			System.out.println("Template not found.");
			return;
		}

		// Test 2: Can we build version info?
		BoxRuntime	runtime		= BoxRuntime.getInstance();
		String		versionInfo	= buildVersionInfoHTML(runtime);
		System.out.println( "Version Info HTML: " + ( versionInfo.isEmpty() ? "(empty - not in debug mode)" : versionInfo));

		// Test 3: Can we build error content and replace placeholders?
		try {
			throw new Exception( "This is a test error message." );
		} catch ( Exception e ) {
			String	errorContent	= buildErrorContentHTML( e, runtime );
			String	result			= template
			    .replace( "{{VERSION_INFO}}", versionInfo )
			    .replace( "{{ERROR_CONTENT}}", errorContent );

			if ( result.contains( "{{VERSION_INFO}}" ) ) {
				System.out.println( "{{VERSION_INFO}} placeholder not replaced." );
			} else {
				System.out.println( "{{VERSION_INFO}} placeholder replaced successfully." );
			}

			if ( result.contains("{{ERROR_CONTENT}}")) {
				System.out.println("{{ERROR_CONTENT}} placeholder not replaced.");
			} else {
				System.out.println("{{ERROR_CONTENT}} placeholder replaced successfully.");
			}
		}
	}
}
