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

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.web.WebRequestExecutor;
import ortus.boxlang.web.context.WebRequestBoxContext;

/**
 * I represent a web request and response
 */
public interface IBoxHTTPExchange {

	/*
	 * Do a server-side foward to a new URI
	 */
	public void forward( String URI );

	/**
	 * Set the BoxLang context for this request
	 *
	 * @param context The BoxLang context
	 */
	public void setWebContext( WebRequestBoxContext context );

	/**
	 * Get the BoxLang context for this request
	 *
	 * @return The BoxLang context
	 */
	public WebRequestBoxContext getWebContext();

	/*****************************************
	 * REQUEST METHODS
	 ****************************************/

	default boolean isTextBasedContentType() {
		String contentType = this.getRequestContentType();
		if ( contentType == null )
			return false;

		// Remove parameters (e.g., "; charset=utf-8") and normalize
		int semicolon = contentType.indexOf( ';' );
		if ( semicolon != -1 ) {
			contentType = contentType.substring( 0, semicolon );
		}
		contentType = contentType.trim().toLowerCase( Locale.ROOT );

		return contentType.startsWith( "text/" )
		    || contentType.equals( "application/json" )
		    || contentType.equals( "application/ld+json" )
		    || contentType.equals( "application/vnd.api+json" )
		    || contentType.equals( "application/hal+json" )
		    || contentType.equals( "application/problem+json" )
		    || contentType.equals( "application/problem+xml" )
		    || contentType.equals( "application/xml" )
		    || contentType.equals( "application/xhtml+xml" )
		    || contentType.equals( "application/rss+xml" )
		    || contentType.equals( "application/atom+xml" )
		    || contentType.equals( "application/x-www-form-urlencoded" )
		    || contentType.equals( "application/javascript" )
		    || contentType.equals( "application/graphql" )
		    || contentType.equals( "application/yaml" )
		    || contentType.equals( "application/x-yaml" )
		    || contentType.equals( "application/x-ndjson" )
		    || contentType.equals( "application/csv" )
		    || contentType.equals( "application/sql" )
		    || contentType.equals( "application/rtf" );
	}

	/**
	 * Default the response content type to text/html if not set.
	 */
	default void ensureResponseContentType() {
		var contentType = getResponseHeader( WebRequestExecutor.CONTENT_TYPE_HEADER );
		if ( contentType == null || contentType.isEmpty() ) {
			addResponseHeader( WebRequestExecutor.CONTENT_TYPE_HEADER, WebRequestExecutor.DEFAULT_CONTENT_TYPE );
		}

	}

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
	 * Get a request cookie by name
	 *
	 * @param name the name of the cookie
	 *
	 * @return the cookie or null if not found
	 */
	public BoxCookie getRequestCookie( String name );

	/**
	 * Returns a map of HTTP request headers where the keys are header names and the values are arrays of header values.
	 */
	public Map<String, String[]> getRequestHeaderMap();

	/**
	 * Returns the value of the specified request header as a <code>String</code>. If the request did not include a header
	 * of the specified name, this method returns <code>null</code>. If there are multiple headers with the same name, this
	 * method returns the first head in the request. The header name is case insensitive. You can use this method with any
	 * request header.
	 */
	public String getRequestHeader( String name );

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
	 * Returns a Map containing the attributes available to this request.
	 */
	public Map<String, Object> getRequestAttributeMap();

	/**
	 * Returns the name of the character encoding used in the body of this request. This method returns <code>null</code> if
	 * no request encoding character encoding has been specified.
	 */
	public String getRequestCharacterEncoding();

	/**
	 * Returns the length, in bytes, of the request body and made available by the input stream, or -1 if the length is not
	 * known.
	 */
	public long getRequestContentLength();

	/**
	 * Returns the MIME type of the body of the request, or <code>null</code> if the type is not known.
	 *
	 * @return a <code>String</code> containing the name of the MIME type of the request, or null if the type is not known
	 */
	public String getRequestContentType();

	/**
	 * Returns a java.util.Map of the form parameters of this request.
	 * This also processes any multi-part form data since the request body is parsed at this time
	 */
	public Map<String, String[]> getRequestFormMap();

	/**
	 * Returns data about multi-part file uploads that were processed in the request.
	 * The order of the file upload objects is the order they were sent in the form. (note when the file component is called
	 * with action="upload" and no file name, the first file is processed.)
	 * Each FileUpload object contains the temporary file path and the original file name.
	 */
	public FileUpload[] getUploadData();

	/**
	 * Returns a java.util.Map of the URL parameters of this request.
	 */
	public Map<String, String[]> getRequestURLMap();

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
	 * Retrieves the body of the request. Will be a string or a byte array depending on the content type.
	 */
	public Object getRequestBody();

	/**
	 * Returns the Internet Protocol (IP) of the remote end of the connection on which the request was received. By default
	 * this is either the address of the client or last proxy that sent the request. In some cases a protocol specific
	 * mechanism, such as <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>, may be used to obtain an address
	 * different to that of the actual TCP/IP connection.
	 */
	public String getRequestRemoteAddr();

	/**
	 * Returns the fully qualified name of the address returned by getRemoteAddr(). If the engine cannot or chooses
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
	 * Returns the fully qualified name of the address returned by getLocalAddr(). If the engine cannot or chooses
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
	 * Returns a boolean indicating if the response has been started.
	 *
	 * @return true if the response has been started, false otherwise
	 */
	public boolean isResponseStarted();

	/**
	 * Adds the specified cookie to the response. This method can be called multiple times to set more than one cookie.
	 *
	 */
	public void addResponseCookie( BoxCookie cookie );

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
	 * Sets the status code and reason phrase for this response.
	 */
	public void setResponseStatus( int sc, String sm );

	/**
	 * Gets the current status code of this response.
	 */
	public int getResponseStatus();

	/**
	 * Gets the value of the response header with the given name.
	 */
	public String getResponseHeader( String name );

	/**
	 * Gets the values of the response headers
	 */
	public Map<String, String[]> getResponseHeaderMap();

	/**
	 * Returns a <code>PrintWriter</code> object that can send character text to the client. The <code>PrintWriter</code>
	 * uses the character encoding returned by getCharacterEncoding. If the response's character encoding has not
	 * been specified as described in <code>getCharacterEncoding</code> (i.e., the method just returns the default value
	 * <code>ISO-8859-1</code>), <code>getWriter</code> updates it to <code>ISO-8859-1</code>.
	 */
	public PrintWriter getResponseWriter();

	/**
	 * Send binary data as response. Rest any other response body content.
	 *
	 * @param data the binary data to send
	 */
	public void sendResponseBinary( byte[] data );

	/**
	 * Send a file as response. Rest any other response body content.
	 *
	 * @param file the file to send
	 */
	public void sendResponseFile( File file );

	/**
	 * Forces any content in the buffer to be written to the client. A call to this method automatically commits the
	 * response, meaning the status code and headers will be written.
	 *
	 */
	public void flushResponseBuffer();

	/**
	 * Clears the content of the underlying buffer in the response without clearing headers or status code. If the response
	 * has been committed, this method throws an <code>IllegalStateException</code>.
	 */
	public void resetResponseBuffer();

	/**
	 * Clear all reponse headers, status code, and buffer
	 */
	public void reset();

	public static record FileUpload( Key formFieldName, Path tmpPath, String originalFileName ) {
	}

}
