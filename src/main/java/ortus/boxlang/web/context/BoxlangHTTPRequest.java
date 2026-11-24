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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

/**
 * A servlet-compatible request facade that delegates to the underlying {@link IBoxHTTPExchange}.
 * BoxLang does not run in a servlet container, so this class provides the commonly-used
 * <code>HttpServletRequest</code> method surface backed by the exchange.
 */
public class BoxlangHTTPRequest {

	private WebRequestBoxContext context;

	public BoxlangHTTPRequest( WebRequestBoxContext context ) {
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

	public String getAuthType() {
		return getExchange().getRequestAuthType();
	}

	public BoxCookie[] getCookies() {
		return getExchange().getRequestCookies();
	}

	public String getHeader( String name ) {
		return getExchange().getRequestHeader( name );
	}

	public Enumeration<String> getHeaders( String name ) {
		String[] values = getExchange().getRequestHeaderMap().get( name );
		if ( values == null ) {
			return Collections.emptyEnumeration();
		}
		return Collections.enumeration( Arrays.asList( values ) );
	}

	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration( getExchange().getRequestHeaderMap().keySet() );
	}

	public String getMethod() {
		return getExchange().getRequestMethod();
	}

	public String getPathInfo() {
		return getExchange().getRequestPathInfo();
	}

	public String getPathTranslated() {
		return getExchange().getRequestPathTranslated();
	}

	public String getContextPath() {
		return getExchange().getRequestContextPath();
	}

	public String getQueryString() {
		return getExchange().getRequestQueryString();
	}

	public String getRemoteUser() {
		return getExchange().getRequestRemoteUser();
	}

	public Principal getUserPrincipal() {
		return getExchange().getRequestUserPrincipal();
	}

	public String getRequestURI() {
		return getExchange().getRequestURI();
	}

	public StringBuffer getRequestURL() {
		return getExchange().getRequestURL();
	}

	public Object getAttribute( String name ) {
		return getExchange().getRequestAttribute( name );
	}

	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration( getExchange().getRequestAttributeMap().keySet() );
	}

	public void setAttribute( String name, Object o ) {
		getExchange().setRequestAttribute( name, o );
	}

	public void removeAttribute( String name ) {
		getExchange().removeRequestAttribute( name );
	}

	public String getCharacterEncoding() {
		return getExchange().getRequestCharacterEncoding();
	}

	public int getContentLength() {
		return ( int ) getExchange().getRequestContentLength();
	}

	public long getContentLengthLong() {
		return getExchange().getRequestContentLength();
	}

	public String getContentType() {
		return getExchange().getRequestContentType();
	}

	public String getScheme() {
		return getExchange().getRequestScheme();
	}

	public String getServerName() {
		return getExchange().getRequestServerName();
	}

	public int getServerPort() {
		return getExchange().getRequestServerPort();
	}

	public String getProtocol() {
		return getExchange().getRequestProtocol();
	}

	public String getRemoteAddr() {
		return getExchange().getRequestRemoteAddr();
	}

	public String getRemoteHost() {
		return getExchange().getRequestRemoteHost();
	}

	public int getRemotePort() {
		return getExchange().getRequestRemotePort();
	}

	public String getLocalAddr() {
		return getExchange().getRequestLocalAddr();
	}

	public String getLocalName() {
		return getExchange().getRequestLocalName();
	}

	public int getLocalPort() {
		return getExchange().getRequestLocalPort();
	}

	public boolean isSecure() {
		return getExchange().isRequestSecure();
	}

	public Locale getLocale() {
		return getExchange().getRequestLocale();
	}

	public Enumeration<Locale> getLocales() {
		return getExchange().getRequestLocales();
	}

	public Object getRequestBody() {
		return getExchange().getRequestBody();
	}

}
