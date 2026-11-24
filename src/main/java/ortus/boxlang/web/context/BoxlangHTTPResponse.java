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
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

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

	public int getStatus() {
		return getExchange().getResponseStatus();
	}

	public void setStatus( int sc ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().setResponseStatus( sc );
	}

	public void setStatus( int sc, String sm ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().setResponseStatus( sc, sm );
	}

	public String getHeader( String name ) {
		return getExchange().getResponseHeader( name );
	}

	public Enumeration<String> getHeaders( String name ) {
		String[] values = getExchange().getResponseHeaderMap().get( name );
		if ( values == null ) {
			return Collections.emptyEnumeration();
		}
		return Collections.enumeration( Arrays.asList( values ) );
	}

	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration( getExchange().getResponseHeaderMap().keySet() );
	}

	public boolean containsHeader( String name ) {
		return getExchange().getResponseHeader( name ) != null;
	}

	public void setHeader( String name, String value ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().setResponseHeader( name, value );
	}

	public void addHeader( String name, String value ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().addResponseHeader( name, value );
	}

	public void setContentType( String value ) {
		if ( getExchange().isResponseStarted() ) {
			return;
		}
		getExchange().setResponseHeader( "Content-Type", value );
	}

	public String getContentType() {
		String contentType = getExchange().getResponseHeader( "Content-Type" );
		if ( contentType == null ) {
			return "text/html";
		}
		return contentType;
	}

	public boolean isCommitted() {
		return getExchange().isResponseStarted();
	}

	public void flushBuffer() {
		getExchange().flushResponseBuffer();
	}

	public void resetBuffer() {
		getExchange().resetResponseBuffer();
	}

	public void reset() {
		getExchange().reset();
		context.clearBuffer();
	}

	public void clearBuffer() {
		context.clearBuffer();
	}

	public PrintWriter getWriter() {
		return getExchange().getResponseWriter();
	}

	public BoxCookie[] getCookies() {
		return getExchange().getResponseCookies();
	}

	public void addCookie( BoxCookie cookie ) {
		getExchange().addResponseCookie( cookie );
	}

}
