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
import java.util.Enumeration;
import java.util.Locale;

import ortus.boxlang.runtime.types.Array;
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

	/**
	 * Returns the name of the authentication scheme used to protect the request, or <code>null</code> if the request is not
	 * authenticated.
	 *
	 * @return the authentication scheme name, or <code>null</code>
	 */
	public String getAuthType() {
		return getExchange().getRequestAuthType();
	}

	/**
	 * Returns a BoxLang array of all the {@link BoxCookie} objects the client sent with this request.
	 *
	 * @return a BoxLang array of cookies, or an empty array if none were sent
	 */
	public Array getCookies() {
		BoxCookie[] cookies = getExchange().getRequestCookies();
		if ( cookies == null ) {
			return Array.EMPTY;
		}
		return Array.fromArray( cookies );
	}

	/**
	 * Returns the value of the specified request header as a <code>String</code>. If the request did not include a header of
	 * the specified name, this method returns <code>null</code>.
	 *
	 * @param name the name of the header
	 *
	 * @return the header value, or <code>null</code> if not present
	 */
	public String getHeader( String name ) {
		return getExchange().getRequestHeader( name );
	}

	/**
	 * Returns a BoxLang array of all the values of the specified request header. The header name is case insensitive.
	 *
	 * @param name the name of the header
	 *
	 * @return a BoxLang array of header values, or an empty array if not present
	 */
	public Array getHeaders( String name ) {
		String[] values = getExchange().getRequestHeaderMap().get( name );
		if ( values == null ) {
			return Array.EMPTY;
		}
		return Array.fromArray( values );
	}

	/**
	 * Returns a BoxLang array of all the request header names.
	 *
	 * @return a BoxLang array of header names
	 */
	public Array getHeaderNames() {
		return Array.fromSet( getExchange().getRequestHeaderMap().keySet() );
	}

	/**
	 * Returns the name of the HTTP method with which this request was made, for example, GET, POST, or PUT.
	 *
	 * @return the HTTP method name
	 */
	public String getMethod() {
		return getExchange().getRequestMethod();
	}

	/**
	 * Returns any extra path information associated with the URL the client sent when it made this request.
	 *
	 * @return the extra path information, or <code>null</code> if none
	 */
	public String getPathInfo() {
		return getExchange().getRequestPathInfo();
	}

	/**
	 * Returns any extra path information after the servlet name but before the query string, translated to a real path.
	 *
	 * @return the translated path, or <code>null</code> if none
	 */
	public String getPathTranslated() {
		return getExchange().getRequestPathTranslated();
	}

	/**
	 * Returns the portion of the request URI that indicates the context of the request.
	 *
	 * @return the context path, or an empty string for the default (root) context
	 */
	public String getContextPath() {
		return getExchange().getRequestContextPath();
	}

	/**
	 * Returns the query string that is contained in the request URL after the path.
	 *
	 * @return the query string, or <code>null</code> if none
	 */
	public String getQueryString() {
		return getExchange().getRequestQueryString();
	}

	/**
	 * Returns the login of the user making this request, if the user has been authenticated, or <code>null</code> otherwise.
	 *
	 * @return the remote user login, or <code>null</code>
	 */
	public String getRemoteUser() {
		return getExchange().getRequestRemoteUser();
	}

	/**
	 * Returns a <code>Principal</code> object containing the name of the current authenticated user, or <code>null</code> if
	 * not authenticated.
	 *
	 * @return the user name, or <code>null</code>
	 */
	public String getUserPrincipal() {
		Principal principal = getExchange().getRequestUserPrincipal();
		return principal != null ? principal.getName() : null;
	}

	/**
	 * Returns the part of this request's URL from the protocol name up to the query string in the first line of the HTTP
	 * request.
	 *
	 * @return the request URI
	 */
	public String getRequestURI() {
		return getExchange().getRequestURI();
	}

	/**
	 * Reconstructs the URL the client used to make the request. The returned URL contains a protocol, server name, port
	 * number, and server path, but it does not include query string parameters.
	 *
	 * @return the request URL
	 */
	public String getRequestURL() {
		return getExchange().getRequestURL().toString();
	}

	/**
	 * Returns the value of the named request attribute as an <code>Object</code>, or <code>null</code> if no attribute of the
	 * given name exists.
	 *
	 * @param name the name of the attribute
	 *
	 * @return the attribute value, or <code>null</code>
	 */
	public Object getAttribute( String name ) {
		return getExchange().getRequestAttribute( name );
	}

	/**
	 * Returns a BoxLang array of the names of all request attributes.
	 *
	 * @return a BoxLang array of attribute names
	 */
	public Array getAttributeNames() {
		return Array.fromSet( getExchange().getRequestAttributeMap().keySet() );
	}

	/**
	 * Stores an attribute in this request. Attributes are reset between requests.
	 *
	 * @param name the name of the attribute
	 * @param o    the value of the attribute
	 */
	public void setAttribute( String name, Object o ) {
		getExchange().setRequestAttribute( name, o );
	}

	/**
	 * Removes an attribute from this request.
	 *
	 * @param name the name of the attribute to remove
	 */
	public void removeAttribute( String name ) {
		getExchange().removeRequestAttribute( name );
	}

	/**
	 * Returns the name of the character encoding used in the body of this request, or <code>null</code> if none has been
	 * specified.
	 *
	 * @return the request character encoding, or <code>null</code>
	 */
	public String getCharacterEncoding() {
		return getExchange().getRequestCharacterEncoding();
	}

	/**
	 * Returns the length, in bytes, of the request body, or <code>-1</code> if the length is not known.
	 *
	 * @return the request body length in bytes
	 */
	public int getContentLength() {
		return ( int ) getExchange().getRequestContentLength();
	}

	/**
	 * Returns the length, in bytes, of the request body as a <code>long</code>, or <code>-1</code> if the length is not known.
	 *
	 * @return the request body length in bytes
	 */
	public long getContentLengthLong() {
		return getExchange().getRequestContentLength();
	}

	/**
	 * Returns the MIME type of the body of the request, or <code>null</code> if the type is not known.
	 *
	 * @return the request content type, or <code>null</code>
	 */
	public String getContentType() {
		return getExchange().getRequestContentType();
	}

	/**
	 * Returns the name of the scheme used to make this request, for example, <code>http</code>, <code>https</code>, or
	 * <code>ftp</code>.
	 *
	 * @return the request scheme
	 */
	public String getScheme() {
		return getExchange().getRequestScheme();
	}

	/**
	 * Returns the host name of the server to which the request was sent.
	 *
	 * @return the server name
	 */
	public String getServerName() {
		return getExchange().getRequestServerName();
	}

	/**
	 * Returns the port number to which the request was sent.
	 *
	 * @return the server port
	 */
	public int getServerPort() {
		return getExchange().getRequestServerPort();
	}

	/**
	 * Returns the name and version of the protocol the request uses, for example, <code>HTTP/1.1</code>.
	 *
	 * @return the request protocol
	 */
	public String getProtocol() {
		return getExchange().getRequestProtocol();
	}

	/**
	 * Returns the Internet Protocol (IP) address of the remote end of the connection on which the request was received.
	 *
	 * @return the remote address
	 */
	public String getRemoteAddr() {
		return getExchange().getRequestRemoteAddr();
	}

	/**
	 * Returns the fully qualified host name of the remote end of the connection, or the IP address if the host name cannot be
	 * resolved.
	 *
	 * @return the remote host
	 */
	public String getRemoteHost() {
		return getExchange().getRequestRemoteHost();
	}

	/**
	 * Returns the Internet Protocol (IP) source port of the remote end of the connection on which the request was received.
	 *
	 * @return the remote port
	 */
	public int getRemotePort() {
		return getExchange().getRequestRemotePort();
	}

	/**
	 * Returns the Internet Protocol (IP) address representing the interface on which the request was received.
	 *
	 * @return the local address
	 */
	public String getLocalAddr() {
		return getExchange().getRequestLocalAddr();
	}

	/**
	 * Returns the fully qualified host name of the interface on which the request was received, or the IP address if the host
	 * name cannot be resolved.
	 *
	 * @return the local name
	 */
	public String getLocalName() {
		return getExchange().getRequestLocalName();
	}

	/**
	 * Returns the Internet Protocol (IP) port number representing the interface on which the request was received.
	 *
	 * @return the local port
	 */
	public int getLocalPort() {
		return getExchange().getRequestLocalPort();
	}

	/**
	 * Returns a boolean indicating whether this request was made using a secure channel, such as HTTPS.
	 *
	 * @return <code>true</code> if the request is secure, otherwise <code>false</code>
	 */
	public boolean isSecure() {
		return getExchange().isRequestSecure();
	}

	/**
	 * Returns the preferred <code>Locale</code> that the client will accept content in, based on the Accept-Language header.
	 *
	 * @return the preferred locale
	 */
	public Locale getLocale() {
		return getExchange().getRequestLocale();
	}

	/**
	 * Returns an <code>Enumeration</code> of <code>Locale</code> objects indicating, in decreasing order starting with the
	 * preferred locale, the locales that are acceptable to the client.
	 *
	 * @return an enumeration of locales
	 */
	public Enumeration<Locale> getLocales() {
		return getExchange().getRequestLocales();
	}

	/**
	 * Retrieves the body of the request. Will be a string or a byte array depending on the content type.
	 *
	 * @return the request body
	 */
	public Object getRequestBody() {
		return getExchange().getRequestBody();
	}

}
