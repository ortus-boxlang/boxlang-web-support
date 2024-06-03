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
package ortus.boxlang.web.exchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * I represent a web request and response
 */
public interface BoxHTTPExchange {

	/*
	 * Do a server-side foward to a new URI
	 */
	public void forward( String URI );

	/*****************************************
	 * REQUEST METHODS
	 ****************************************/

	/**
	 * Returns the name of the authentication scheme used to protect the servlet. All servlet containers support basic, form
	 * and client certificate authentication, and may additionally support digest authentication. If the servlet is not
	 * authenticated <code>null</code> is returned.
	 */
	public String getRequestAuthType();

	/**
	 * Returns an array containing all of the <code>Cookie</code> objects the client sent with this request. This method
	 * returns <code>null</code> if no cookies were sent.
	 */
	public BoxCookie[] getRequestCookies();

	/**
	 * Returns the value of the specified request header as a <code>String</code>. If the request did not include a header
	 * of the specified name, this method returns <code>null</code>. If there are multiple headers with the same name, this
	 * method returns the first head in the request. The header name is case insensitive. You can use this method with any
	 * request header.
	 */
	public String getRequestHeader( String name );

	/**
	 * Returns all the values of the specified request header as an <code>Enumeration</code> of <code>String</code> objects.
	 */
	public Enumeration<String> getRequestHeaders( String name );

	/**
	 * Returns an enumeration of all the header names this request contains. If the request has no headers, this method
	 * returns an empty enumeration.
	 */
	public Enumeration<String> getRequestHeaderNames();

	/**
	 * Returns the name of the HTTP method with which this request was made, for example, GET, POST, or PUT.
	 */
	public String getRequestMethod();

	/**
	 * Returns any extra path information associated with the URL the client sent when it made this request. The extra path
	 * information follows the servlet path but precedes the query string and will start with a "/" character.
	 */
	public String getRequestPathInfo();

	/**
	 * Returns any extra path information after the servlet name but before the query string, and translates it to a real
	 * path.
	 */
	public String getRequestPathTranslated();

	/**
	 * Returns the portion of the request URI that indicates the context of the request. The context path always comes first
	 * in a request URI. The path starts with a "/" character but does not end with a "/" character. For servlets in the
	 * default (root) context, this method returns "". The container does not decode this string.
	 */
	public String getRequestContextPath();

	/**
	 * Returns the query string that is contained in the request URL after the path. This method returns <code>null</code>
	 * if the URL does not have a query string.
	 */
	public String getRequestQueryString();

	/**
	 * Returns the login of the user making this request, if the user has been authenticated, or <code>null</code> if the
	 * user has not been authenticated. Whether the user name is sent with each subsequent request depends on the browser
	 * and type of authentication.
	 */
	public String getRequestRemoteUser();

	/**
	 * Returns a <code>java.security.Principal</code> object containing the name of the current authenticated user. If the
	 * user has not been authenticated, the method returns <code>null</code>.
	 */
	public java.security.Principal getRequestUserPrincipal();

	/**
	 * Returns the part of this request's URL from the protocol name up to the query string in the first line of the HTTP
	 * request. The web container does not decode this String.
	 */
	public String getRequestURI();

	/**
	 * Reconstructs the URL the client used to make the request. The returned URL contains a protocol, server name, port
	 * number, and server path, but it does not include query string parameters.
	 */
	public StringBuffer getRequestURL();

	/**
	 * Returns the value of the named attribute as an <code>Object</code>, or <code>null</code> if no attribute of the given
	 * name exists.
	 */
	public Object getRequestAttribute( String name );

	/**
	 * Returns an <code>Enumeration</code> containing the names of the attributes available to this request. This method
	 * returns an empty <code>Enumeration</code> if the request has no attributes available to it.
	 */
	public Enumeration<String> getRequestAttributeNames();

	/**
	 * Returns the name of the character encoding used in the body of this request. This method returns <code>null</code> if
	 * no request encoding character encoding has been specified.
	 */
	public String getRequestCharacterEncoding();

	/**
	 * Overrides the name of the character encoding used in the body of this request. This method must be called prior to
	 * reading request parameters or reading input using getReader(). Otherwise, it has no effect.
	 */
	public void setRequestCharacterEncoding( String env ) throws UnsupportedEncodingException;

	/**
	 * Returns the length, in bytes, of the request body and made available by the input stream, or -1 if the length is not
	 * known.
	 */
	public long getRequestContentLengthLong();

	/**
	 * Returns the MIME type of the body of the request, or <code>null</code> if the type is not known.
	 *
	 * @return a <code>String</code> containing the name of the MIME type of the request, or null if the type is not known
	 */
	public String getRequestContentType();

	/**
	 * Returns the value of a request parameter as a <code>String</code>, or <code>null</code> if the parameter does not
	 * exist. Request parameters are extra information sent with the request. For HTTP servlets, parameters are contained in
	 * the query string or posted form data.
	 */
	public String getRequestParameter( String name );

	/**
	 *
	 * Returns an <code>Enumeration</code> of <code>String</code> objects containing the names of the parameters contained
	 * in this request. If the request has no parameters, the method returns an empty <code>Enumeration</code>.
	 */
	public Enumeration<String> getRequestParameterNames();

	/**
	 * Returns an array of <code>String</code> objects containing all of the values the given request parameter has, or
	 * <code>null</code> if the parameter does not exist.
	 */
	public String[] getRequestParameterValues( String name );

	/**
	 * Returns a java.util.Map of the parameters of this request.
	 */
	public Map<String, String[]> getRequestParameterMap();

	/**
	 * Returns the name and version of the protocol the request uses in the form <i>protocol/majorVersion.minorVersion</i>,
	 * for example, HTTP/1.1.
	 */
	public String getRequestProtocol();

	/**
	 * Returns the name of the scheme used to make this request, for example, <code>http</code>, <code>https</code>, or
	 * <code>ftp</code>. Different schemes have different rules for constructing URLs, as noted in RFC 1738.
	 */
	public String getRequestScheme();

	/**
	 * Returns the host name of the server to which the request was sent. It may be derived from a protocol specific
	 * mechanism, such as the <code>Host</code> header, or the HTTP/2 authority, or
	 * <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>, otherwise the resolved server name or the server IP
	 * address.
	 *
	 * @return a <code>String</code> containing the name of the server
	 */
	public String getRequestServerName();

	/**
	 * Returns the port number to which the request was sent. It may be derived from a protocol specific mechanism, such as
	 * the <code>Host</code> header, or HTTP authority, or <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>,
	 * otherwise the server port where the client connection was accepted on.
	 */
	public int getRequestServerPort();

	/**
	 * Retrieves the body of the request as character data using a <code>BufferedReader</code>. The reader translates the
	 * character data according to the character encoding used on the body. Either this method or {@link #getInputStream}
	 * may be called to read the body, not both.
	 */
	public BufferedReader getRequestReader() throws IOException;

	/**
	 * Returns the Internet Protocol (IP) of the remote end of the connection on which the request was received. By default
	 * this is either the address of the client or last proxy that sent the request. In some cases a protocol specific
	 * mechanism, such as <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>, may be used to obtain an address
	 * different to that of the actual TCP/IP connection.
	 */
	public String getRequestRemoteAddr();

	/**
	 * Returns the fully qualified name of the address returned by {@link #getRemoteAddr()}. If the engine cannot or chooses
	 * not to resolve the hostname (to improve performance), this method returns the IP address.
	 */
	public String getRequestRemoteHost();

	/**
	 * Stores an attribute in this request. Attributes are reset between requests.
	 *
	 */
	public void setRequestAttribute( String name, Object o );

	/**
	 *
	 * Removes an attribute from this request. This method is not generally needed as attributes only persist as long as the
	 * request is being handled.
	 */
	public void removeRequestAttribute( String name );

	/**
	 * Returns the preferred <code>Locale</code> that the client will accept content in, based on the Accept-Language
	 * header. If the client request doesn't provide an Accept-Language header, this method returns the default locale for
	 * the server.
	 */
	public Locale getRequestLocale();

	/**
	 * Returns an <code>Enumeration</code> of <code>Locale</code> objects indicating, in decreasing order starting with the
	 * preferred locale, the locales that are acceptable to the client based on the Accept-Language header. If the client
	 * request doesn't provide an Accept-Language header, this method returns an <code>Enumeration</code> containing one
	 * <code>Locale</code>, the default locale for the server.
	 */
	public Enumeration<Locale> getRequestLocales();

	/**
	 *
	 * Returns a boolean indicating whether this request was made using a secure channel, such as HTTPS.
	 */
	public boolean isRequestSecure();

	/**
	 * Returns the Internet Protocol (IP) source port the remote end of the connection on which the request was received. By
	 * default this is either the port of the client or last proxy that sent the request. In some cases, protocol specific
	 * mechanisms such as <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a> may be used to obtain a port different
	 * to that of the actual TCP/IP connection.
	 */
	public int getRequestRemotePort();

	/**
	 * Returns the fully qualified name of the address returned by {@link #getLocalAddr()}. If the engine cannot or chooses
	 * not to resolve the hostname (to improve performance), this method returns the IP address.
	 */
	public String getRequestLocalName();

	/**
	 * Returns the Internet Protocol (IP) address representing the interface on which the request was received. In some
	 * cases a protocol specific mechanism, such as <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>, may be used
	 * to obtain an address different to that of the actual TCP/IP connection.
	 */
	public String getRequestLocalAddr();

	/**
	 * Returns the Internet Protocol (IP) port number representing the interface on which the request was received. In some
	 * cases, a protocol specific mechanism such as <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a> may be used
	 * to obtain an address different to that of the actual TCP/IP connection.
	 */
	public int getRequestLocalPort();

	/*****************************************
	 * RESONSE METHODS
	 ****************************************/

	/**
	 * Adds the specified cookie to the response. This method can be called multiple times to set more than one cookie.
	 *
	 */
	public void addResponseCookie( BoxCookie cookie );

	/**
	 * Returns a boolean indicating whether the named response header has already been set.
	 */
	public boolean containsResponseHeader( String name );

	/**
	 *
	 * Sets a response header with the given name and value. If the header had already been set, the new value overwrites
	 * the previous one. The <code>containsHeader</code> method can be used to test for the presence of a header before
	 * setting its value.
	 */
	public void setResponseHeader( String name, String value );

	/**
	 * Adds a response header with the given name and value. This method allows response headers to have multiple values.
	 */
	public void addResponseHeader( String name, String value );

	/**
	 * Sets the status code for this response.
	 */
	public void setResponseStatus( int sc );

	/**
	 * Gets the current status code of this response.
	 */
	public int getResponseStatus();

	/**
	 * Gets the value of the response header with the given name.
	 */
	public String getResponseHeader( String name );

	/**
	 * Gets the values of the response header with the given name.
	 */
	public Collection<String> getResponseHeaders( String name );

	/**
	 * Gets the names of the headers of this response.
	 */
	public Collection<String> getResponseHeaderNames();

	/**
	 * Returns the name of the character encoding (MIME charset) used for the body sent in this response.
	 */
	public String getResponseCharacterEncoding();

	/**
	 * Returns the content type used for the MIME body sent in this response.
	 */
	public String getResponseContentType();

	/**
	 * Returns a <code>PrintWriter</code> object that can send character text to the client. The <code>PrintWriter</code>
	 * uses the character encoding returned by {@link #getCharacterEncoding}. If the response's character encoding has not
	 * been specified as described in <code>getCharacterEncoding</code> (i.e., the method just returns the default value
	 * <code>ISO-8859-1</code>), <code>getWriter</code> updates it to <code>ISO-8859-1</code>.
	 */
	public PrintWriter getResponseWriter() throws IOException;

	/**
	 * Sets the character encoding (MIME charset) of the response being sent to the client, for example, to UTF-8.
	 */
	public void setResponseCharacterEncoding( String charset );

	/**
	 * Sets the content type of the response being sent to the client, if the response has not been committed yet. The given
	 * content type may include a character encoding specification, for example, <code>text/html;charset=UTF-8</code>.
	 */
	public void setResponseContentType( String type );

	/**
	 * Forces any content in the buffer to be written to the client. A call to this method automatically commits the
	 * response, meaning the status code and headers will be written.
	 *
	 */
	public void flushResponseBuffer() throws IOException;

	/**
	 * Clears the content of the underlying buffer in the response without clearing headers or status code. If the response
	 * has been committed, this method throws an <code>IllegalStateException</code>.
	 */
	public void resetResponseBuffer();

	/**
	 * Sets the locale of the response, if the response has not been committed yet.
	 */
	public void setResponseLocale( Locale loc );

	/**
	 * Returns the locale specified for this response using the {@link #setLocale} method. Calls made to
	 * <code>setLocale</code> after the response is committed have no effect. If no locale has been specified, the
	 * container's default locale is returned.
	 */
	public Locale getResponseLocale();

}
