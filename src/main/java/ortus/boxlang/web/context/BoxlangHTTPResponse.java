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
package ortus.boxlang.web.context;

import java.io.PrintWriter;

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

/**
 * A servlet-compatible response facade that delegates to the underlying {@link IBoxHTTPExchange}.
 * BoxLang does not run in a servlet container, so this class provides the commonly-used
 * <code>HttpServletResponse</code> method surface backed by the exchange.
 */
public class BoxlangHTTPResponse {

	private WebRequestBoxContext context;

	public BoxlangHTTPResponse( WebRequestBoxContext context ) {
		this.context = context;
	}

	/**
	 * Get the underlying exchange
	 *
	 * @return the exchange
	 */
	public IBoxHTTPExchange getExchange() {
		return context.getHTTPExchange();
	}

	/**
	 * Returns the current status code of this response.
	 *
	 * @return the response status code
	 */
	public int getStatus() {
		return getExchange().getResponseStatus();
	}

	/**
	 * Sets the status code for this response. If the response has already been started, this method does nothing.
	 *
	 * @param sc the status code
	 */
	public void setStatus( int sc ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().setResponseStatus( sc );
	}

	/**
	 * Sets the status code and reason phrase for this response. If the response has already been started, this method does
	 * nothing.
	 *
	 * @param sc the status code
	 * @param sm the reason phrase
	 */
	public void setStatus( int sc, String sm ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().setResponseStatus( sc, sm );
	}

	/**
	 * Returns the value of the response header with the given name.
	 *
	 * @param name the name of the header
	 *
	 * @return the header value, or <code>null</code> if not present
	 */
	public String getHeader( String name ) {
		return getExchange().getResponseHeader( name );
	}

	/**
	 * Returns a BoxLang array of all the values of the specified response header.
	 *
	 * @param name the name of the header
	 *
	 * @return a BoxLang array of header values, or an empty array if not present
	 */
	public Array getHeaders( String name ) {
		String[] values = getExchange().getResponseHeaderMap().get( name );
		if ( values == null ) {
			return Array.EMPTY;
		}
		return Array.fromArray( values );
	}

	/**
	 * Returns a BoxLang array of all the response header names.
	 *
	 * @return a BoxLang array of header names
	 */
	public Array getHeaderNames() {
		return Array.fromSet( getExchange().getResponseHeaderMap().keySet() );
	}

	/**
	 * Returns a boolean indicating whether the named response header has already been set.
	 *
	 * @param name the name of the header
	 *
	 * @return <code>true</code> if the header has been set, otherwise <code>false</code>
	 */
	public boolean containsHeader( String name ) {
		return getExchange().getResponseHeader( name ) != null;
	}

	/**
	 * Sets a response header with the given name and value. If the response has already been started, this method does nothing.
	 *
	 * @param name  the name of the header
	 * @param value the value of the header
	 */
	public void setHeader( String name, String value ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().setResponseHeader( name, value );
	}

	/**
	 * Adds a response header with the given name and value. This method allows response headers to have multiple values. If the
	 * response has already been started, this method does nothing.
	 *
	 * @param name  the name of the header
	 * @param value the value of the header
	 */
	public void addHeader( String name, String value ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().addResponseHeader( name, value );
	}

	/**
	 * Sets the response content type header. If the response has already been started, this method does nothing.
	 *
	 * @param value the content type value
	 */
	public void setContentType( String value ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().setResponseHeader( "Content-Type", value );
	}

	/**
	 * Returns the response content type, defaulting to <code>text/html</code> if not explicitly set.
	 *
	 * @return the response content type
	 */
	public String getContentType() {
		String contentType = getExchange().getResponseHeader( "Content-Type" );
		if ( contentType == null ) {
			return "text/html";
		}
		return contentType;
	}

	/**
	 * Returns a boolean indicating if the response has been committed.
	 *
	 * @return <code>true</code> if the response has been committed, otherwise <code>false</code>
	 */
	public boolean isCommitted() {
		return getExchange().isResponseStarted();
	}

	/**
	 * Forces any content in the buffer to be written to the client.
	 */
	public void flushBuffer() {
		getExchange().flushResponseBuffer();
	}

	/**
	 * Clears the content of the underlying buffer in the response without clearing headers or status code.
	 */
	public void resetBuffer() {
		getExchange().resetResponseBuffer();
	}

	/**
	 * Clears all response headers, status code, and buffer.
	 */
	public void reset() {
		getExchange().reset();
		context.clearBuffer();
	}

	/**
	 * Clears the BoxLang response buffer without clearing headers or status code.
	 */
	public void clearBuffer() {
		context.clearBuffer();
	}

	/**
	 * Returns a <code>PrintWriter</code> object that can send character text to the client.
	 *
	 * @return the response writer
	 */
	public PrintWriter getWriter() {
		return getExchange().getResponseWriter();
	}

	/**
	 * Returns a BoxLang array of the cookies that have been added to the response.
	 *
	 * @return a BoxLang array of response cookies, or an empty array if none
	 */
	public Array getCookies() {
		return Array.fromArray( getExchange().getResponseCookies() );
	}

	/**
	 * Adds the specified cookie to the response. This method can be called multiple times to set more than one cookie.
	 *
	 * @param cookie the cookie to add
	 */
	public void addCookie( BoxCookie cookie ) {
		getExchange().addResponseCookie( cookie );
	}

}
