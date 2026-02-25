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
package ortus.boxlang.web.context;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;
import ortus.boxlang.runtime.util.Mapping;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.DetachedHTTPExchange;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.scopes.CGIScope;
import ortus.boxlang.web.scopes.CookieScope;
import ortus.boxlang.web.scopes.FormScope;
import ortus.boxlang.web.scopes.RequestScope;
import ortus.boxlang.web.scopes.URLScope;
import ortus.boxlang.web.util.KeyDictionary;

/**
 * This context represents the context of a web/HTTP site in BoxLang
 * There a variables and request scope present.
 */
public class WebRequestBoxContext extends RequestBoxContext {

	private static BoxRuntime	runtime					= BoxRuntime.getInstance();

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The variables scope
	 */
	protected IScope			variablesScope			= new VariablesScope();

	/**
	 * The request scope
	 */
	protected IScope			requestScope;

	/**
	 * The URL scope
	 */
	protected IScope			URLScope;

	/**
	 * The form scope
	 */
	protected IScope			formScope;

	/**
	 * The CGI scope
	 */
	protected IScope			CGIScope;

	/**
	 * The cookie scope
	 */
	protected IScope			cookieScope;

	protected IBoxHTTPExchange	httpExchange;

	/**
	 * The request body can only be read once, so we cache it here
	 */
	protected Object			requestBody				= null;

	/**
	 * The web root for this request
	 */
	protected String			webRoot;

	/**
	 * The session ID for this request
	 */
	protected Key				sessionID				= null;

	protected IStruct			appSettings;

	protected IStruct			sessionCookieDefaults	= Struct.of(
	    Key._NAME, "jsessionid",
	    KeyDictionary.secure, false,
	    KeyDictionary.httpOnly, true,
	    KeyDictionary.disableUpdate, false,
	    Key.timeout, new DateTime().modify( "yyyy", 30l ),
	    KeyDictionary.sameSite, "Lax" );

	protected boolean			sessionCookieProvided	= false;
	protected boolean			isSessionReset			= false;

	/**
	 * Since getting config for the request happens a lot and rarley changes, cache it to improve performance
	 * If config is changed at any context "above" us in the chain, we'll need to clear the cache via clearConfigCache()
	 */
	private IStruct				configCache				= null;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template and parent
	 * context
	 *
	 * @param parent The parent context
	 */
	public WebRequestBoxContext( IBoxContext parent, IBoxHTTPExchange httpExchange, String webRoot, URI template ) {
		super( parent );
		httpExchange.setWebContext( this );
		this.httpExchange	= httpExchange;
		this.webRoot		= webRoot;
		URLScope			= new URLScope( this );
		formScope			= new FormScope( this );
		CGIScope			= new CGIScope( this );
		cookieScope			= new CookieScope( this );
		requestScope		= new RequestScope( this );
	}

	/**
	 * Creates a new execution context with a bounded execution template and parent
	 * context
	 *
	 * @param parent The parent context
	 */
	public WebRequestBoxContext( IBoxContext parent, IBoxHTTPExchange exchange, String webRoot ) {
		this( parent, exchange, webRoot, null );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the session key for this request, creating if neccessary
	 *
	 * @return The session key
	 */
	public Key getSessionID() {

		IStruct appSettings = getConfig().getAsStruct( Key.applicationSettings );

		appSettings.putIfAbsent( KeyDictionary.sessionCookie, new Struct() );
		IStruct sessionCookieSettings = appSettings.getAsStruct( KeyDictionary.sessionCookie );
		sessionCookieDefaults.entrySet().stream().forEach( entry -> {
			sessionCookieSettings.putIfAbsent( entry.getKey(), entry.getValue() );
		} );

		// Only look if this is the first time for this request
		if ( this.sessionID == null ) {
			synchronized ( this ) {
				// double check...
				if ( this.sessionID == null ) {
					// Check for existing request cookie
					BoxCookie sessionCookie = httpExchange
					    .getRequestCookie( sessionCookieDefaults.getAsString( Key._NAME ) );
					if ( !this.isSessionReset && sessionCookie != null
					    && ( sessionCookie.getMaxAge() == null || !Integer.valueOf( 0 ).equals( sessionCookie.getMaxAge() ) ) ) {
						String idValue = sessionCookie.getValue();
						// We need to ensure that we are not dealing with an empty value or a null derivation
						if ( idValue != null && !idValue.isEmpty() && !idValue.toLowerCase().equals( "null" ) ) {
							this.sessionID = Key.of( sessionCookie.getValue() );
						} else {
							this.sessionID = Key.of( UUID.randomUUID().toString() );
						}
					} else {
						// Otherwise generate a new one
						this.sessionID = Key.of( UUID.randomUUID().toString() );
					}
				}
			}
		}
		// assert the sessionID has been defined now
		return this.sessionID;
	}

	/**
	 *
	 * Generate a session cookie based on the settings in the application config
	 *
	 * @param sessionid The session ID key
	 * @param maxAge    The max age of the cookie in seconds. If null, expiration is used
	 *
	 * @return The BoxCookie instance
	 */
	private BoxCookie generateSessionCookie( Key newId, Integer maxAge ) {

		IStruct appSettings = getConfig().getAsStruct( Key.applicationSettings );

		appSettings.putIfAbsent( KeyDictionary.sessionCookie, new Struct() );
		IStruct sessionCookieSettings = appSettings.getAsStruct( KeyDictionary.sessionCookie );
		sessionCookieDefaults.entrySet().stream().forEach( entry -> {
			sessionCookieSettings.putIfAbsent( entry.getKey(), entry.getValue() );
		} );

		BoxCookie sessionCookie = new BoxCookie( sessionCookieDefaults.getAsString( Key._NAME ),
		    newId.getName() )
		        .setPath( "/" );

		Optional.ofNullable( sessionCookieSettings.get( KeyDictionary.httpOnly ) ).map( BooleanCaster::cast ).map( sessionCookie::setHttpOnly );

		Optional.ofNullable( sessionCookieSettings.get( KeyDictionary.secure ) ).map( BooleanCaster::cast ).map( sessionCookie::setSecure );

		Optional.ofNullable( sessionCookieSettings.get( Key.domain ) ).map( StringCaster::cast ).map( sessionCookie::setDomain );

		Optional.ofNullable( sessionCookieSettings.get( KeyDictionary.sameSite ) ).map( StringCaster::cast )
		    .map( sessionCookie::setSameSiteMode );

		Object expiration = sessionCookieSettings.get( Key.timeout );
		// The browser will not honor maxAge if expiration is present
		if ( maxAge == null ) {
			if ( expiration instanceof DateTime expireDateTime ) {
				sessionCookie.setExpires( Date.from( expireDateTime.toInstant() ) );
			} else if ( expiration instanceof Duration expireDuration ) {
				sessionCookie.setExpires( Date.from( Instant.now().plus( expireDuration ) ) );
			} else {
				sessionCookie.setExpires(
				    Date.from( sessionCookieDefaults.getAsDateTime( Key.timeout ).toInstant() ) );
			}
		} else {
			sessionCookie.setMaxAge( maxAge );
		}

		return sessionCookie;
	}

	/**
	 * Invalidate a session
	 */
	public void resetSession() {
		synchronized ( this ) {
			this.sessionID = null;
			if ( this.sessionID == null ) {
				BoxCookie sessionCookie = httpExchange
				    .getRequestCookie( sessionCookieDefaults.getAsString( Key._NAME ) );
				if ( sessionCookie != null ) {
					// Add an expiration cookie to the response.
					BoxCookie expiryCookie = generateSessionCookie( Key.of( sessionCookie.getValue() ), 0 );
					httpExchange.addResponseCookie( expiryCookie );
					// throw up our flag to force new session ID
					this.isSessionReset = true;
					// Force a new session ID and ensure that the cookie is sent
					getApplicationListener().invalidateSession( getSessionID() );
					// Now reset the flag so that subsequent calls work normally
					this.isSessionReset = false;
				} else {
					getApplicationListener().invalidateSession( getSessionID() );
				}
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		scopes.getAsStruct( Key.contextual ).put( ortus.boxlang.web.scopes.URLScope.name, URLScope );
		scopes.getAsStruct( Key.contextual ).put( FormScope.name, formScope );
		scopes.getAsStruct( Key.contextual ).put( ortus.boxlang.web.scopes.CGIScope.name, CGIScope );
		scopes.getAsStruct( Key.contextual ).put( CookieScope.name, cookieScope );
		scopes.getAsStruct( Key.contextual ).put( RequestScope.name, requestScope );
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name, variablesScope );
		}
		return scopes;
	}

	/**
	 * Check if a key is visible in the current context as a scope name.
	 * This allows us to "reserve" known scope names to ensure arguments.foo
	 * will always look in the proper arguments scope and never in
	 * local.arguments.foo for example
	 *
	 * @param key     The key to check for visibility
	 * @param nearby  true, check only scopes that are nearby to the current
	 *                execution context
	 * @param shallow true, do not delegate to parent or default scope if not found
	 *
	 * @return True if the key is visible in the current context, else false
	 */
	@Override
	public boolean isKeyVisibleScope( Key key, boolean nearby, boolean shallow ) {
		if ( key.equals( URLScope.getName() ) ||
		    key.equals( formScope.getName() ) ||
		    key.equals( CGIScope.getName() ) ||
		    key.equals( cookieScope.getName() ) ||
		    key.equals( requestScope.getName() ) ) {
			return true;
		}
		if ( nearby && key.equals( VariablesScope.name ) ) {
			return true;
		}
		return super.isKeyVisibleScope( key, false, false );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * A local lookup is used for the closest context to the executing code
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {

		// In query loop?
		if ( !forAssign ) {
			var querySearch = queryFindNearby( key );
			if ( querySearch != null ) {
				return querySearch;
			}
		}

		// In Variables scope? (thread-safe lookup and get)
		Object result = variablesScope.getRaw( key );
		// Null means not found
		if ( isDefined( result, forAssign ) ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( variablesScope, Struct.unWrapNull( result ), key );
		}

		if ( shallow ) {
			return null;
		}

		return scopeFind( key, defaultScope, forAssign );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * Unlike scopeFindNearby(), this version only searches trancedent scopes like
	 * cgi or server which are never encapsulated like variables is inside a class.
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign ) {

		if ( key.equals( requestScope.getName() ) ) {
			return new ScopeSearchResult( requestScope, requestScope, key, true );
		}
		if ( key.equals( CGIScope.getName() ) ) {
			return new ScopeSearchResult( CGIScope, CGIScope, key, true );
		}
		if ( key.equals( URLScope.getName() ) ) {
			return new ScopeSearchResult( URLScope, URLScope, key, true );
		}
		if ( key.equals( formScope.getName() ) ) {
			return new ScopeSearchResult( formScope, formScope, key, true );
		}
		if ( key.equals( cookieScope.getName() ) ) {
			return new ScopeSearchResult( cookieScope, cookieScope, key, true );
		}

		if ( !forAssign ) {
			Object result = CGIScope.getRaw( key );
			// Null means not found
			if ( isDefined( result, forAssign ) ) {
				// Unwrap the value now in case it was really actually null for real
				return new ScopeSearchResult( CGIScope, Struct.unWrapNull( result ), key );
			}

			result = URLScope.getRaw( key );
			// Null means not found
			if ( isDefined( result, forAssign ) ) {
				// Unwrap the value now in case it was really actually null for real
				return new ScopeSearchResult( URLScope, Struct.unWrapNull( result ), key );
			}

			result = formScope.getRaw( key );
			// Null means not found
			if ( isDefined( result, forAssign ) ) {
				// Unwrap the value now in case it was really actually null for real
				return new ScopeSearchResult( formScope, Struct.unWrapNull( result ), key );
			}
		}

		return super.scopeFind( key, defaultScope, forAssign );
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Don't search for scopes which are local to an execution context
	 *
	 * @return The requested scope
	 */
	@Override
	public IScope getScope( Key name ) throws ScopeNotFoundException {

		if ( name.equals( requestScope.getName() ) ) {
			return requestScope;
		}

		if ( name.equals( URLScope.getName() ) ) {
			return URLScope;
		}

		if ( name.equals( formScope.getName() ) ) {
			return formScope;
		}

		if ( name.equals( CGIScope.getName() ) ) {
			return CGIScope;
		}

		if ( name.equals( cookieScope.getName() ) ) {
			return cookieScope;
		}

		if ( parent != null ) {
			return parent.getScope( name );
		}

		// Not found anywhere
		throw new ScopeNotFoundException(
		    String.format( "The requested scope name [%s] was not located in any context", name.getName() ) );

	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all konwn scopes
	 *
	 * @return The requested scope
	 */
	@Override
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {
		// Check the scopes I know about
		if ( name.equals( variablesScope.getName() ) ) {
			return variablesScope;
		}

		if ( shallow ) {
			return null;
		}

		return getScope( name );
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void registerUDF( UDF udf, boolean override ) {
		registerUDF( variablesScope, udf, override );
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		return variablesScope;
	}

	/**
	 * Flush the buffer to the output stream
	 *
	 * @param force true, flush even if output is disabled
	 *
	 * @return This context
	 */
	@Override
	public IBoxContext flushBuffer( boolean force ) {
		if ( !canOutput() && !force ) {
			return this;
		}

		// Send our session cookie, if not already done, in order to keep alive the session and forward the expiration
		synchronized ( this ) {
			if ( !sessionCookieProvided ) {
				// Ensure session cookie is sent if not already
				BoxCookie sessionCookie = generateSessionCookie( getSessionID(), null );
				httpExchange.addResponseCookie( sessionCookie );
				sessionCookieProvided = true;
			}
		}

		// This will commit the response so we don't want to do it unless we're forcing
		// a flush, or it's the end of the request
		// in which case, the web request executor will always issue a final forced
		// flush. Otherwise, just let the buffer keep accumulating
		if ( force ) {
			var		theBuffers	= super._getBuffers();
			String	output		= "";
			for ( StringBuffer buf : theBuffers ) {
				synchronized ( buf ) {
					output = output.concat( buf.toString() );
					buf.setLength( 0 );
				}
			}
			httpExchange.ensureResponseContentType();
			httpExchange.getResponseWriter().write( output );
			httpExchange.flushResponseBuffer();
		}
		return this;
	}

	/**
	 * Get the HTTP exchange
	 *
	 * @return The HTTP exchange
	 */
	public IBoxHTTPExchange getHTTPExchange() {
		if ( httpExchange == null ) {
			throw new BoxRuntimeException(
			    "This context has been shutdown, and discarded the HTTP exchange.  No threads should be using this context.  Please report this as a bug." );
		}
		return httpExchange;
	}

	/**
	 * Set the HTTP exchange
	 *
	 */
	public void setHTTPExchange( IBoxHTTPExchange httpExchange ) {
		this.httpExchange = httpExchange;
	}

	/**
	 * Get the request body as a byte array
	 *
	 * @return The request body
	 */
	public Object getRequestBody() {
		// TODO: rework this to deal with binary and text request bodies
		if ( requestBody != null ) {
			return requestBody;
		}
		synchronized ( httpExchange ) {
			if ( requestBody != null ) {
				return requestBody;
			}
			requestBody = httpExchange.getRequestBody();
		}

		return requestBody;
	}

	public IStruct getConfig() {
		if ( configCache != null ) {
			return configCache;
		}

		var		config		= super.getConfig();

		IStruct	appMappings	= getApplicationListener().getSettings().getAsStruct( Key.mappings );
		// Only set this if our application this.mappings doesn't already override it
		if ( appMappings == null || appMappings.get( Key._slash ) == null ) {
			config.getAsStruct( Key.mappings ).put( Key._slash, Mapping.ofInternal( "/", webRoot ) );
		}
		configCache = config;
		return config;
	}

	/**
	 * Contexts can optionally cache their config. If so, they must override this method
	 * to clear the cache when requested, and propagate the request to their parent context
	 */
	public void clearConfigCache() {
		configCache = null;
		super.clearConfigCache();
	}

	/**
	 * Get the web root for this request
	 *
	 * @return The web root
	 */
	public String getWebRoot() {
		return webRoot;
	}

	/**
	 * Get cookie scope
	 */
	public IScope getCookieScope() {
		return cookieScope;
	}

	/**
	 * Check if whitespace compression is enabled.
	 * Return true if the content-type is HTML and the compression is enabled in the
	 * config
	 */
	public boolean isWhitespaceCompressionEnabled() {
		IStruct config = getConfig();
		// If the global setting is disabled, return false
		if ( !BooleanCaster.cast( config.getOrDefault( Key.whitespaceCompressionEnabled, true ) ) ) {
			return false;
		}
		// If the response is HTML, return true
		String contentTypeHeader = httpExchange.getResponseHeader( "Content-Type" );
		if ( contentTypeHeader != null && contentTypeHeader.startsWith( "text/html" ) ) {
			return true;
		}
		// It's another content type like binary or JSON, etc
		return false;
	}

	@Override
	public void shutdown() {
		if ( hasDependentThreads() ) {
			detachFromHTTPExchange();
		} else {
			this.httpExchange = null;
		}
		super.shutdown();
	}

	/**
	 * Detach this context from the HTTP exchange to allow background threads to
	 * complete without holding onto the exchange
	 */
	private void detachFromHTTPExchange() {
		// Replace the exchange with a detached version
		// It has a copy of all the original data, but all mutator methods are no-ops and it holds
		// no actual references to the upstream servlet or server objects
		setHTTPExchange( DetachedHTTPExchange.from( getHTTPExchange() ) );
	}

}
