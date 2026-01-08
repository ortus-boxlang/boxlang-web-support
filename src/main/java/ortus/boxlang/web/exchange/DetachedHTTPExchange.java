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
import java.io.StringWriter;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ortus.boxlang.web.context.WebRequestBoxContext;

/**
 * A detached HTTP exchange that copies all data from an existing exchange.
 * All mutator methods become no-ops since the exchange is detached from the actual request/response.
 * This is useful for asynchronous processing where the original exchange may be closed.
 */
public class DetachedHTTPExchange implements IBoxHTTPExchange {

	// Request data (immutable copies)
	private final String				requestAuthType;
	private final BoxCookie[]			requestCookies;
	private final Map<String, String[]>	requestHeaderMap;
	private final String				requestMethod;
	private final String				requestPathInfo;
	private final String				requestPathTranslated;
	private final String				requestContextPath;
	private final String				requestQueryString;
	private final String				requestRemoteUser;
	private final Principal				requestUserPrincipal;
	private final String				requestURI;
	private final StringBuffer			requestURL;
	private final Map<String, Object>	requestAttributeMap;
	private final String				requestCharacterEncoding;
	private final long					requestContentLength;
	private final String				requestContentType;
	private final Map<String, String[]>	requestFormMap;
	private final FileUpload[]			uploadData;
	private final Map<String, String[]>	requestURLMap;
	private final String				requestProtocol;
	private final String				requestScheme;
	private final String				requestServerName;
	private final int					requestServerPort;
	private final Object				requestBody;
	private final String				requestRemoteAddr;
	private final String				requestRemoteHost;
	private final Locale				requestLocale;
	private final Enumeration<Locale>	requestLocales;
	private final boolean				requestSecure;
	private final int					requestRemotePort;
	private final String				requestLocalName;
	private final String				requestLocalAddr;
	private final int					requestLocalPort;

	// Response data (copies from original)
	private final int					responseStatus;
	private final Map<String, String[]>	responseHeaderMap;
	private final boolean				responseStarted;

	// Context reference
	private WebRequestBoxContext		webContext;

	// No-op writer for response operations
	private final PrintWriter			noOpWriter;

	/**
	 * Private constructor - use the static from() method to create instances
	 */
	private DetachedHTTPExchange( IBoxHTTPExchange source ) {
		// Copy all request data
		this.requestAuthType			= source.getRequestAuthType();
		this.requestCookies				= source.getRequestCookies();
		this.requestHeaderMap			= copyMap( source.getRequestHeaderMap() );
		this.requestMethod				= source.getRequestMethod();
		this.requestPathInfo			= source.getRequestPathInfo();
		this.requestPathTranslated		= source.getRequestPathTranslated();
		this.requestContextPath			= source.getRequestContextPath();
		this.requestQueryString			= source.getRequestQueryString();
		this.requestRemoteUser			= source.getRequestRemoteUser();
		this.requestUserPrincipal		= source.getRequestUserPrincipal();
		this.requestURI					= source.getRequestURI();
		this.requestURL					= new StringBuffer( source.getRequestURL().toString() );
		this.requestAttributeMap		= new HashMap<>( source.getRequestAttributeMap() );
		this.requestCharacterEncoding	= source.getRequestCharacterEncoding();
		this.requestContentLength		= source.getRequestContentLength();
		this.requestContentType			= source.getRequestContentType();
		this.requestFormMap				= copyMap( source.getRequestFormMap() );
		this.uploadData					= source.getUploadData();
		this.requestURLMap				= copyMap( source.getRequestURLMap() );
		this.requestProtocol			= source.getRequestProtocol();
		this.requestScheme				= source.getRequestScheme();
		this.requestServerName			= source.getRequestServerName();
		this.requestServerPort			= source.getRequestServerPort();
		this.requestBody				= source.getRequestBody();
		this.requestRemoteAddr			= source.getRequestRemoteAddr();
		this.requestRemoteHost			= source.getRequestRemoteHost();
		this.requestLocale				= source.getRequestLocale();
		this.requestLocales				= source.getRequestLocales();
		this.requestSecure				= source.isRequestSecure();
		this.requestRemotePort			= source.getRequestRemotePort();
		this.requestLocalName			= source.getRequestLocalName();
		this.requestLocalAddr			= source.getRequestLocalAddr();
		this.requestLocalPort			= source.getRequestLocalPort();

		// Copy response data
		this.responseStatus				= source.getResponseStatus();
		this.responseHeaderMap			= copyMap( source.getResponseHeaderMap() );
		this.responseStarted			= source.isResponseStarted();

		// Copy context reference
		this.webContext					= source.getWebContext();

		// Create a no-op writer that discards all output
		this.noOpWriter					= new PrintWriter( new StringWriter() );
	}

	/**
	 * Creates a detached copy of an existing HTTP exchange.
	 * All data is copied from the source exchange, and all mutator methods become no-ops.
	 *
	 * @param source The source exchange to copy data from
	 *
	 * @return A new DetachedHTTPExchange instance with copied data
	 */
	public static DetachedHTTPExchange from( IBoxHTTPExchange source ) {
		return new DetachedHTTPExchange( source );
	}

	/**
	 * Helper method to create an immutable copy of a map
	 */
	private static Map<String, String[]> copyMap( Map<String, String[]> source ) {
		if ( source == null ) {
			return Collections.emptyMap();
		}
		Map<String, String[]> copy = new HashMap<>();
		for ( Map.Entry<String, String[]> entry : source.entrySet() ) {
			String[] values = entry.getValue();
			if ( values != null ) {
				copy.put( entry.getKey(), values.clone() );
			} else {
				copy.put( entry.getKey(), null );
			}
		}
		return Collections.unmodifiableMap( copy );
	}

	/*****************************************
	 * NO-OP MUTATOR METHODS
	 *****************************************/

	@Override
	public void forward( String URI ) {
		// No-op: Cannot forward on a detached exchange
	}

	@Override
	public void setWebContext( WebRequestBoxContext context ) {
		// Allow setting context as it may be needed for async processing
		this.webContext = context;
	}

	@Override
	public void setRequestAttribute( String name, Object o ) {
		// No-op: Cannot modify request attributes on a detached exchange
	}

	@Override
	public void removeRequestAttribute( String name ) {
		// No-op: Cannot modify request attributes on a detached exchange
	}

	@Override
	public void addResponseCookie( BoxCookie cookie ) {
		// No-op: Cannot add cookies on a detached exchange
	}

	@Override
	public void setResponseHeader( String name, String value ) {
		// No-op: Cannot set headers on a detached exchange
	}

	@Override
	public void addResponseHeader( String name, String value ) {
		// No-op: Cannot add headers on a detached exchange
	}

	@Override
	public void setResponseStatus( int sc ) {
		// No-op: Cannot set status on a detached exchange
	}

	@Override
	public void setResponseStatus( int sc, String sm ) {
		// No-op: Cannot set status on a detached exchange
	}

	@Override
	public void sendResponseBinary( byte[] data ) {
		// No-op: Cannot send binary data on a detached exchange
	}

	@Override
	public void sendResponseFile( File file ) {
		// No-op: Cannot send file on a detached exchange
	}

	@Override
	public void flushResponseBuffer() {
		// No-op: Cannot flush buffer on a detached exchange
	}

	@Override
	public void resetResponseBuffer() {
		// No-op: Cannot reset buffer on a detached exchange
	}

	@Override
	public void reset() {
		// No-op: Cannot reset on a detached exchange
	}

	/*****************************************
	 * GETTER METHODS - Return copied data
	 *****************************************/

	@Override
	public WebRequestBoxContext getWebContext() {
		return this.webContext;
	}

	@Override
	public String getRequestAuthType() {
		return this.requestAuthType;
	}

	@Override
	public BoxCookie[] getRequestCookies() {
		return this.requestCookies;
	}

	@Override
	public BoxCookie getRequestCookie( String name ) {
		if ( this.requestCookies == null ) {
			return null;
		}
		for ( BoxCookie cookie : this.requestCookies ) {
			if ( cookie.getName().equalsIgnoreCase( name ) ) {
				return cookie;
			}
		}
		return null;
	}

	@Override
	public Map<String, String[]> getRequestHeaderMap() {
		return this.requestHeaderMap;
	}

	@Override
	public String getRequestHeader( String name ) {
		String[] values = this.requestHeaderMap.get( name );
		if ( values == null ) {
			// Try case-insensitive lookup
			for ( Map.Entry<String, String[]> entry : this.requestHeaderMap.entrySet() ) {
				if ( entry.getKey().equalsIgnoreCase( name ) ) {
					values = entry.getValue();
					break;
				}
			}
		}
		return ( values != null && values.length > 0 ) ? values[ 0 ] : null;
	}

	@Override
	public String getRequestMethod() {
		return this.requestMethod;
	}

	@Override
	public String getRequestPathInfo() {
		return this.requestPathInfo;
	}

	@Override
	public String getRequestPathTranslated() {
		return this.requestPathTranslated;
	}

	@Override
	public String getRequestContextPath() {
		return this.requestContextPath;
	}

	@Override
	public String getRequestQueryString() {
		return this.requestQueryString;
	}

	@Override
	public String getRequestRemoteUser() {
		return this.requestRemoteUser;
	}

	@Override
	public Principal getRequestUserPrincipal() {
		return this.requestUserPrincipal;
	}

	@Override
	public String getRequestURI() {
		return this.requestURI;
	}

	@Override
	public StringBuffer getRequestURL() {
		return this.requestURL;
	}

	@Override
	public Object getRequestAttribute( String name ) {
		return this.requestAttributeMap.get( name );
	}

	@Override
	public Map<String, Object> getRequestAttributeMap() {
		return Collections.unmodifiableMap( this.requestAttributeMap );
	}

	@Override
	public String getRequestCharacterEncoding() {
		return this.requestCharacterEncoding;
	}

	@Override
	public long getRequestContentLength() {
		return this.requestContentLength;
	}

	@Override
	public String getRequestContentType() {
		return this.requestContentType;
	}

	@Override
	public Map<String, String[]> getRequestFormMap() {
		return this.requestFormMap;
	}

	@Override
	public FileUpload[] getUploadData() {
		return this.uploadData;
	}

	@Override
	public Map<String, String[]> getRequestURLMap() {
		return this.requestURLMap;
	}

	@Override
	public String getRequestProtocol() {
		return this.requestProtocol;
	}

	@Override
	public String getRequestScheme() {
		return this.requestScheme;
	}

	@Override
	public String getRequestServerName() {
		return this.requestServerName;
	}

	@Override
	public int getRequestServerPort() {
		return this.requestServerPort;
	}

	@Override
	public Object getRequestBody() {
		return this.requestBody;
	}

	@Override
	public String getRequestRemoteAddr() {
		return this.requestRemoteAddr;
	}

	@Override
	public String getRequestRemoteHost() {
		return this.requestRemoteHost;
	}

	@Override
	public Locale getRequestLocale() {
		return this.requestLocale;
	}

	@Override
	public Enumeration<Locale> getRequestLocales() {
		return this.requestLocales;
	}

	@Override
	public boolean isRequestSecure() {
		return this.requestSecure;
	}

	@Override
	public int getRequestRemotePort() {
		return this.requestRemotePort;
	}

	@Override
	public String getRequestLocalName() {
		return this.requestLocalName;
	}

	@Override
	public String getRequestLocalAddr() {
		return this.requestLocalAddr;
	}

	@Override
	public int getRequestLocalPort() {
		return this.requestLocalPort;
	}

	@Override
	public boolean isResponseStarted() {
		return this.responseStarted;
	}

	@Override
	public int getResponseStatus() {
		return this.responseStatus;
	}

	@Override
	public String getResponseHeader( String name ) {
		String[] values = this.responseHeaderMap.get( name );
		if ( values == null ) {
			// Try case-insensitive lookup
			for ( Map.Entry<String, String[]> entry : this.responseHeaderMap.entrySet() ) {
				if ( entry.getKey().equalsIgnoreCase( name ) ) {
					values = entry.getValue();
					break;
				}
			}
		}
		return ( values != null && values.length > 0 ) ? values[ 0 ] : null;
	}

	@Override
	public Map<String, String[]> getResponseHeaderMap() {
		return this.responseHeaderMap;
	}

	@Override
	public PrintWriter getResponseWriter() {
		// Return a no-op writer since we can't write to a detached exchange
		return this.noOpWriter;
	}
}
