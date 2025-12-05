/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.web.bifs;

import java.util.Map;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

@BoxBIF( description = "Gets the current page context." )
public class GetPageContext extends BIF {

	/**
	 * Constructor
	 */
	public GetPageContext() {
		super();
	}

	/**
	 * Gets the current server PageContext object that provides access to page attributes and configuration, request and response objects.
	 * If not running in a servlet, this will be a fake class attempting to provide most of the common methods.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The current PageContext object.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return new PageContext( context );
	}

	/**
	 * Fake PageContext class to provide common methods since we are NOT in a servlet container
	 */
	public class PageContext {

		private IBoxHTTPExchange		exchange;
		private WebRequestBoxContext	context;

		public PageContext( IBoxContext context ) {
			this.context	= context.getParentOfType( WebRequestBoxContext.class );
			this.exchange	= this.context.getHTTPExchange();
		}

		/**
		 * Returns the current context object. This method is provided for backward compatibility
		 *
		 * @return
		 */
		public PageContext getResponse() {
			return this;
		}

		/**
		 * Returns the current request object. This method is provided for backward compatibility
		 *
		 * @return
		 */
		public PageContext getRequest() {
			return this;
		}

		/**
		 * Sets a response header. If the response has already been started, this method does nothing.
		 *
		 * @param name
		 * @param value
		 */
		public void setHeader( String name, String value ) {
			if ( exchange.isResponseStarted() ) {
				return;
			}
			exchange.setResponseHeader( name, value );
		}

		/**
		 * Sets the response content type header. If the response has already been started, this method does nothing.
		 *
		 * @param value
		 */
		public void setContentType( String value ) {
			if ( exchange.isResponseStarted() ) {
				return;
			}
			setHeader( "Content-Type", value );
		}

		// Shim until we fix auto java casting
		public void setStatus( Double code, String text ) {
			setStatus( code.intValue(), text );
		}

		// Shim until we fix auto java casting
		public void setStatus( Double code ) {
			setStatus( code.intValue() );
		}

		/**
		 * Sets the response status code and message. If the response has already
		 * been started, this method does nothing.
		 *
		 * @param code
		 * @param text
		 */
		public void setStatus( int code, String text ) {
			if ( exchange.isResponseStarted() ) {
				return;
			}
			exchange.setResponseStatus( code, text );
		}

		/**
		 * Sets the response status code.
		 *
		 * @param code
		 */
		public void setStatus( int code ) {
			setStatus( code, "" );
		}

		/**
		 * Returns the current response status code. This method is provided for backward compatibility
		 *
		 * @return
		 */
		public int getStatus() {
			return exchange.getResponseStatus();
		}

		/**
		 * Clears the response buffer, if any, and resets the response status code and headers.
		 *
		 */
		public void reset() {
			exchange.reset();
			context.clearBuffer();
		}

		/**
		 * Clears the response buffer, if any, and resets the response status code and headers.
		 * This method is provided for backward compatibility
		 */
		public void clearBuffer() {
			context.clearBuffer();
		}

		/**
		 * Clears the response buffer, if any, and resets the response status code and headers.
		 */
		public void resetHTMLHead() {
			context.clearBuffer();
		}

		/**
		 * Returns the current PageContext object. This method is provided for backward compatibility
		 *
		 * @return
		 */
		public PageContext getOut() {
			return this;
		}

		public Boolean hasFamily() {
			return false;
		}

		/**
		 * Returns the request URL as a StringBuffer. This is the full URL
		 *
		 * @return
		 */
		public StringBuffer getRequestURL() {
			return new StringBuffer( exchange.getRequestURL() );
		}

		// Getting response content type. If we need to get request content type, we'll need to spoof actual request and response objects
		public String getContentType() {
			String contentType = exchange.getResponseHeader( "Content-Type" );
			if ( contentType == null ) {
				return "text/html";
			} else {
				return contentType;
			}
		}

		/**
		 * Adds a response header to the response. If the response has already
		 * been committed, this method does nothing.
		 *
		 * @param name
		 * @param value
		 */
		public void addHeader( String name, String value ) {
			setHeader( name, value );
		}

		/**
		 * Returns a map of all the response headers.
		 *
		 * @return
		 */
		public Map<String, String[]> getResponseHeaderMap() {
			return exchange.getResponseHeaderMap();
		}

		/**
		 * Returns the value of the specified response header as a String.
		 *
		 * @param name
		 *
		 * @return
		 */
		public String getResponseHeader( String name ) {
			return exchange.getResponseHeader( name );
		}

		/**
		 * Returns a map of all the request headers.
		 *
		 * @return
		 */
		public Map<String, String[]> getRequestHeaderMap() {
			return exchange.getRequestHeaderMap();
		}

		/**
		 * Returns the value of the specified request header as a String.
		 *
		 * @param name
		 *
		 * @return
		 */
		public String getRequestHeader( String name ) {
			return exchange.getRequestHeader( name );
		}

		/**
		 * Alias for getRequestHeader - for compat
		 *
		 * @param name
		 *
		 * @return
		 */
		public String getHeader( String name ) {
			return exchange.getRequestHeader( name );
		}

		/**
		 * Returns a boolean indicating if the response has been
		 * committed. A committed response has already had its status
		 * code and headers written.
		 *
		 * @return a boolean indicating if the response has been committed
		 *
		 */
		public boolean isCommitted() {
			return exchange.isResponseStarted();
		}

		/**
		 * Returns the name of the scheme used to make this request, for example, http, https, or ftp. Different schemes have different rules for constructing
		 * URLs, as noted in RFC 1738.
		 *
		 * @return a String containing the name of the scheme used to make this request
		 */
		public String getScheme() {
			return exchange.getRequestScheme();
		}

	}

}
