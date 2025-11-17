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
package ortus.boxlang.web.util;

import java.io.File;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

/**
 * Provides a mock implementation of the IBoxHTTPExchange interface for testing purposes.
 */
public class MockHTTPExchange implements IBoxHTTPExchange {

	String					requestMethod			= "GET";
	WebRequestBoxContext	context;
	BoxCookie[]				cookies;
	BoxCookie[]				responseCookies			= new BoxCookie[ 0 ];
	Map<String, String[]>	requestHeaders			= new HashMap<>();
	Map<String, String[]>	responseHeaders			= new HashMap<>();
	Map<String, Object>		requestAttributes		= new HashMap<>();
	String					requestAuthType;
	String					requestPathInfo;
	String					requestPathTranslated;
	String					requestContextPath;
	String					requestQueryString;
	String					requestRemoteUser;
	Principal				requestUserPrincipal;
	String					requestURI;
	StringBuffer			requestURL;
	String					requestCharacterEncoding;
	long					requestContentLength	= -1;
	String					requestContentType;
	Map<String, String[]>	requestFormMap			= new HashMap<>();
	FileUpload[]			uploadData;
	Map<String, String[]>	requestURLMap			= new HashMap<>();
	String					requestProtocol;
	String					requestScheme;
	String					requestServerName		= "localhost";
	int						requestServerPort		= 0;
	Object					requestBody;
	String					requestRemoteAddr;
	String					requestRemoteHost;
	Locale					requestLocale			= Locale.getDefault();
	List<Locale>			requestLocales			= new ArrayList<>();
	boolean					requestSecure			= false;
	int						requestRemotePort		= 0;
	String					requestLocalName;
	String					requestLocalAddr;
	int						requestLocalPort		= 0;
	boolean					responseStarted			= false;
	int						responseStatus			= 200;
	String					responseStatusMessage;
	PrintWriter				responseWriter			= new PrintWriter( System.out );

	public MockHTTPExchange( BoxCookie[] cookies, Map<String, String[]> requestHeaders ) {
		this.cookies		= cookies;
		this.requestHeaders	= requestHeaders;
	}

	@Override
	public void forward( String URI ) {
		// No-op for mock
	}

	@Override
	public void setWebContext( WebRequestBoxContext context ) {
		this.context = context;
	}

	@Override
	public WebRequestBoxContext getWebContext() {
		return this.context;
	}

	@Override
	public String getRequestAuthType() {
		return requestAuthType;
	}

	@Override
	public BoxCookie[] getRequestCookies() {
		return this.cookies;
	}

	@Override
	public BoxCookie getRequestCookie( String name ) {
		if ( this.cookies != null ) {
			for ( BoxCookie cookie : this.cookies ) {
				if ( cookie.getName().equals( name ) ) {
					return cookie;
				}
			}
		}
		return null;
	}

	@Override
	public Map<String, String[]> getRequestHeaderMap() {
		return requestHeaders;
	}

	@Override
	public String getRequestHeader( String name ) {
		String[] values = requestHeaders.get( name );
		if ( values != null && values.length > 0 ) {
			return values[ 0 ];
		}
		return null;
	}

	@Override
	public String getRequestMethod() {
		return requestMethod;
	}

	@Override
	public String getRequestPathInfo() {
		return requestPathInfo;
	}

	@Override
	public String getRequestPathTranslated() {
		return requestPathTranslated;
	}

	@Override
	public String getRequestContextPath() {
		return requestContextPath;
	}

	@Override
	public String getRequestQueryString() {
		return requestQueryString;
	}

	@Override
	public String getRequestRemoteUser() {
		return requestRemoteUser;
	}

	@Override
	public Principal getRequestUserPrincipal() {
		return requestUserPrincipal;
	}

	@Override
	public String getRequestURI() {
		return requestURI;
	}

	@Override
	public StringBuffer getRequestURL() {
		return requestURL;
	}

	@Override
	public Object getRequestAttribute( String name ) {
		return requestAttributes.get( name );
	}

	@Override
	public Map<String, Object> getRequestAttributeMap() {
		return requestAttributes;
	}

	@Override
	public String getRequestCharacterEncoding() {
		return requestCharacterEncoding;
	}

	@Override
	public long getRequestContentLength() {
		return requestContentLength;
	}

	@Override
	public String getRequestContentType() {
		return requestContentType;
	}

	@Override
	public Map<String, String[]> getRequestFormMap() {
		return requestFormMap;
	}

	@Override
	public FileUpload[] getUploadData() {
		return uploadData;
	}

	@Override
	public Map<String, String[]> getRequestURLMap() {
		return requestURLMap;
	}

	@Override
	public String getRequestProtocol() {
		return requestProtocol;
	}

	@Override
	public String getRequestScheme() {
		return requestScheme;
	}

	@Override
	public String getRequestServerName() {
		return requestServerName;
	}

	@Override
	public int getRequestServerPort() {
		return requestServerPort;
	}

	@Override
	public Object getRequestBody() {
		return requestBody;
	}

	@Override
	public String getRequestRemoteAddr() {
		return requestRemoteAddr;
	}

	@Override
	public String getRequestRemoteHost() {
		return requestRemoteHost;
	}

	@Override
	public void setRequestAttribute( String name, Object o ) {
		requestAttributes.put( name, o );
	}

	@Override
	public void removeRequestAttribute( String name ) {
		requestAttributes.remove( name );
	}

	@Override
	public Locale getRequestLocale() {
		return requestLocale;
	}

	@Override
	public Enumeration<Locale> getRequestLocales() {
		return Collections.enumeration( requestLocales.isEmpty() ? List.of( requestLocale ) : requestLocales );
	}

	@Override
	public boolean isRequestSecure() {
		return requestSecure;
	}

	@Override
	public int getRequestRemotePort() {
		return requestRemotePort;
	}

	@Override
	public String getRequestLocalName() {
		return requestLocalName;
	}

	@Override
	public String getRequestLocalAddr() {
		return requestLocalAddr;
	}

	@Override
	public int getRequestLocalPort() {
		return requestLocalPort;
	}

	@Override
	public boolean isResponseStarted() {
		return responseStarted;
	}

	@Override
	public void addResponseCookie( BoxCookie cookie ) {
		List<BoxCookie> list = new ArrayList<>( List.of( responseCookies ) );
		list.add( cookie );
		responseCookies = list.toArray( new BoxCookie[ 0 ] );
	}

	@Override
	public void setResponseHeader( String name, String value ) {
		responseHeaders.put( name, new String[] { value } );
	}

	@Override
	public void addResponseHeader( String name, String value ) {
		responseHeaders.computeIfAbsent( name, k -> new String[ 0 ] );
		String[]	old		= responseHeaders.get( name );
		String[]	updated	= new String[ old.length + 1 ];
		System.arraycopy( old, 0, updated, 0, old.length );
		updated[ old.length ] = value;
		responseHeaders.put( name, updated );
	}

	@Override
	public void setResponseStatus( int sc ) {
		responseStatus = sc;
	}

	@Override
	public void setResponseStatus( int sc, String sm ) {
		responseStatus			= sc;
		responseStatusMessage	= sm;
	}

	@Override
	public int getResponseStatus() {
		return responseStatus;
	}

	@Override
	public String getResponseHeader( String name ) {
		String[] values = responseHeaders.get( name );
		return ( values != null && values.length > 0 ) ? values[ 0 ] : null;
	}

	@Override
	public Map<String, String[]> getResponseHeaderMap() {
		return responseHeaders;
	}

	@Override
	public PrintWriter getResponseWriter() {
		return responseWriter;
	}

	@Override
	public void sendResponseBinary( byte[] data ) {
		// No-op for mock
	}

	@Override
	public void sendResponseFile( File file ) {
		// No-op for mock
	}

	@Override
	public void flushResponseBuffer() {
		// No-op for mock
	}

	@Override
	public void resetResponseBuffer() {
		// No-op for mock
	}

	@Override
	public void reset() {
		requestHeaders.clear();
		responseHeaders.clear();
		requestAttributes.clear();
		responseCookies			= new BoxCookie[ 0 ];
		responseStatus			= 200;
		responseStatusMessage	= null;
	}

	public BoxCookie[] getResponseCookies() {
		return this.responseCookies;
	}

	public void setRequestMethod( String method ) {
		this.requestMethod = method;
	}

	public void setRequestHeaders( Map<String, String[]> headers ) {
		this.requestHeaders = headers;
	}

	public void setRequestCookies( BoxCookie[] cookies ) {
		this.cookies = cookies;
	}

	// Setters for properties without them
	public void setRequestAuthType( String requestAuthType ) {
		this.requestAuthType = requestAuthType;
	}

	public void setRequestPathInfo( String requestPathInfo ) {
		this.requestPathInfo = requestPathInfo;
	}

	public void setRequestPathTranslated( String requestPathTranslated ) {
		this.requestPathTranslated = requestPathTranslated;
	}

	public void setRequestContextPath( String requestContextPath ) {
		this.requestContextPath = requestContextPath;
	}

	public void setRequestQueryString( String requestQueryString ) {
		this.requestQueryString = requestQueryString;
	}

	public void setRequestRemoteUser( String requestRemoteUser ) {
		this.requestRemoteUser = requestRemoteUser;
	}

	public void setRequestUserPrincipal( Principal requestUserPrincipal ) {
		this.requestUserPrincipal = requestUserPrincipal;
	}

	public void setRequestURI( String requestURI ) {
		this.requestURI = requestURI;
	}

	public void setRequestURL( StringBuffer requestURL ) {
		this.requestURL = requestURL;
	}

	public void setRequestCharacterEncoding( String requestCharacterEncoding ) {
		this.requestCharacterEncoding = requestCharacterEncoding;
	}

	public void setRequestContentLength( long requestContentLength ) {
		this.requestContentLength = requestContentLength;
	}

	public void setRequestContentType( String requestContentType ) {
		this.requestContentType = requestContentType;
	}

	public void setRequestFormMap( Map<String, String[]> requestFormMap ) {
		this.requestFormMap = requestFormMap;
	}

	public void setUploadData( FileUpload[] uploadData ) {
		this.uploadData = uploadData;
	}

	public void setRequestURLMap( Map<String, String[]> requestURLMap ) {
		this.requestURLMap = requestURLMap;
	}

	public void setRequestProtocol( String requestProtocol ) {
		this.requestProtocol = requestProtocol;
	}

	public void setRequestScheme( String requestScheme ) {
		this.requestScheme = requestScheme;
	}

	public void setRequestServerName( String requestServerName ) {
		this.requestServerName = requestServerName;
	}

	public void setRequestServerPort( int requestServerPort ) {
		this.requestServerPort = requestServerPort;
	}

	public void setRequestBody( Object requestBody ) {
		this.requestBody = requestBody;
	}

	public void setRequestRemoteAddr( String requestRemoteAddr ) {
		this.requestRemoteAddr = requestRemoteAddr;
	}

	public void setRequestRemoteHost( String requestRemoteHost ) {
		this.requestRemoteHost = requestRemoteHost;
	}

	public void setRequestLocale( Locale requestLocale ) {
		this.requestLocale = requestLocale;
	}

	public void setRequestLocales( List<Locale> requestLocales ) {
		this.requestLocales = requestLocales;
	}

	public void setRequestSecure( boolean requestSecure ) {
		this.requestSecure = requestSecure;
	}

	public void setRequestRemotePort( int requestRemotePort ) {
		this.requestRemotePort = requestRemotePort;
	}

	public void setRequestLocalName( String requestLocalName ) {
		this.requestLocalName = requestLocalName;
	}

	public void setRequestLocalAddr( String requestLocalAddr ) {
		this.requestLocalAddr = requestLocalAddr;
	}

	public void setRequestLocalPort( int requestLocalPort ) {
		this.requestLocalPort = requestLocalPort;
	}

	public void setResponseStarted( boolean responseStarted ) {
		this.responseStarted = responseStarted;
	}

	public void setResponseStatusMessage( String responseStatusMessage ) {
		this.responseStatusMessage = responseStatusMessage;
	}

	public void setResponseWriter( PrintWriter responseWriter ) {
		this.responseWriter = responseWriter;
	}

}
