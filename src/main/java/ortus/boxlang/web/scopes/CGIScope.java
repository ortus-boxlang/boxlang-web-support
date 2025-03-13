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
package ortus.boxlang.web.scopes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.util.KeyDictionary;

/**
 * CGI scope implementation in BoxLang
 */
public class CGIScope extends BaseScope {

	/**
	 * THE KEYS THAT ARE KNOWN TO THE CGI SCOPE
	 */
	private Set<Key>			knownKeys	= new TreeSet<>( Arrays.asList(
	    Key.auth_password,
	    Key.auth_type,
	    Key.auth_user,
	    KeyDictionary.bx_template_path,
	    Key.cert_cookie,
	    Key.cert_flags,
	    Key.cert_issuer,
	    Key.cert_keysize,
	    Key.cert_secretkeysize,
	    Key.cert_serialnumber,
	    Key.cert_server_issuer,
	    Key.cert_server_subject,
	    Key.cert_subject,
	    Key.cf_template_path,
	    Key.content_length,
	    Key.content_type,
	    Key.context_path,
	    Key.gateway_interface,
	    Key.http_accept_encoding,
	    Key.http_accept_language,
	    Key.http_accept,
	    Key.http_connection,
	    Key.http_cookie,
	    Key.http_host,
	    Key.http_referer,
	    Key.http_user_agent,
	    Key.https_keysize,
	    Key.https_secretkeysize,
	    Key.https_server_issuer,
	    Key.https_server_subject,
	    Key.https,
	    Key.local_addr,
	    Key.local_host,
	    Key.path_info,
	    Key.path_translated,
	    Key.query_string,
	    Key.remote_addr,
	    Key.remote_host,
	    Key.remote_user,
	    Key.request_method,
	    Key.request_url,
	    Key.script_name,
	    Key.server_name,
	    Key.server_port_secure,
	    Key.server_port,
	    Key.server_protocol,
	    Key.server_software,
	    Key.web_server_api
	) );

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key		name		= Key.of( "cgi" );

	/**
	 * The Linked Exchange
	 */
	protected IBoxHTTPExchange	exchange;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public CGIScope( IBoxHTTPExchange exchange ) {
		super( CGIScope.name );
		this.exchange = exchange;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the absolute path to the template
	 *
	 * @param context The current context
	 *
	 * @return The absolute path to the template
	 */
	private String getTemplatePath( IBoxContext context ) {
		WebRequestBoxContext	webContext	= context.getParentOfType( WebRequestBoxContext.class );
		String					requestURI	= exchange.getRequestURI();

		// Null checks
		if ( requestURI == null ) {
			return "";
		}

		// Build the path from the context.getWebRoot() + requestURI
		return Path.of( webContext.getWebRoot() + requestURI ).toAbsolutePath().toString();
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param key  The key to dereference
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereference( IBoxContext context, Key key, Boolean safe ) {
		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		Object value = getRaw( key );
		if ( value == null ) {
			// CGI scope NEVER errors. It simply returns empty string if the key is not
			// found
			return "";
		}
		return unWrapNullInternal( value );
	}

	/**
	 * Returns the value to which the specified Key is mapped
	 *
	 * @param key the key whose associated value is to be returned
	 *
	 * @return the value to which the specified key is mapped or null if not found
	 */
	@Override
	public Object get( String key ) {
		Key keyObj = Key.of( key );
		return unWrapNullInternal( getRaw( keyObj ) );
	}

	/**
	 * Returns the value of the key safely, nulls will be wrapped in a NullValue still.
	 *
	 * @param key The key to look for
	 *
	 * @return The value of the key or a NullValue object, null means the key didn't exist *
	 */
	@Override
	public Object getRaw( Key key ) {
		Object value = wrapped.get( key );
		// If we've already gotten this key once, it's cached
		if ( value != null ) {
			return value;
		}

		if ( key.equals( Key.content_type ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestHeader( "Content-Type" ) ) );
		}
		if ( key.equals( Key.content_length ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestContentLength() ) );
		}
		if ( key.equals( Key.cf_template_path ) || key.equals( KeyDictionary.bx_template_path ) || key.equals( Key.path_translated ) ) {
			return putAndReturn( key, defaultNullToString( getTemplatePath( exchange.getWebContext() ) ) );
		}
		if ( key.equals( Key.https ) ) {
			return putAndReturn( key, defaultNullToString( exchange.isRequestSecure() ) );
		}
		if ( key.equals( Key.http_host ) ) {
			int port = exchange.getRequestServerPort();
			return putAndReturn( key,
			    port == 80 || port == 443 ? exchange.getRequestServerName() : exchange.getRequestServerName() + ":" + defaultNullToString( port ) );
		}
		if ( key.equals( Key.local_addr ) ) {
			try {
				return putAndReturn( key, defaultNullToString( InetAddress.getLocalHost().getHostAddress() ) );
			} catch ( UnknownHostException e ) {
				return putAndReturn( key, "127.0.0.1" );
			}
		}
		if ( key.equals( Key.local_host ) ) {
			try {
				return putAndReturn( key, defaultNullToString( InetAddress.getLocalHost().getHostName() ) );
			} catch ( UnknownHostException e ) {
				return putAndReturn( key, "localhost" );
			}
		}
		if ( key.equals( Key.request_url ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestURL() ) );
		}
		if ( key.equals( Key.remote_addr ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestRemoteAddr() ) );
		}
		if ( key.equals( Key.remote_host ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestRemoteHost() ) );
		}
		if ( key.equals( Key.remote_user ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestRemoteUser() ) );
		}
		if ( key.equals( Key.path_info ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestPathInfo() ) );
		}
		if ( key.equals( Key.query_string ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestQueryString() ) );
		}
		if ( key.equals( Key.request_method ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestMethod() ) );
		}
		if ( key.equals( Key.script_name ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestURI() ) );
		}
		if ( key.equals( Key.server_name ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestServerName() ) );
		}
		if ( key.equals( Key.server_port ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestServerPort() ) );
		}
		if ( key.equals( Key.server_port_secure ) ) {
			return putAndReturn( key, exchange.isRequestSecure() ? defaultNullToString( exchange.getRequestServerPort() ) : 0 );
		}
		if ( key.equals( Key.server_protocol ) ) {
			return putAndReturn( key, defaultNullToString( exchange.getRequestProtocol() ) );
		}

		// TODO: All other CGI keys

		/*
		 * auth_password
		 * auth_typeauth_user
		 * cert_cookie
		 * cert_flags
		 * cert_issuer
		 * cert_keysize
		 * cert_secretkeysize
		 * cert_serialnumber
		 * cert_server_issuer
		 * cert_server_subject
		 * cert_subject
		 * context_path
		 * gateway_interface
		 * https_keysize
		 * https_secretkeysize
		 * https_server_issuer
		 * https_server_subject
		 * server_software
		 * web_server_api
		 */

		// HTTP header fallbacks
		String header = exchange.getRequestHeader( key.getName() );
		if ( header != null ) {
			return putAndReturn( key, header );
		}
		if ( key.getName().toLowerCase().startsWith( "http_" ) && key.getName().length() > 5 ) {
			header = exchange.getRequestHeader( key.getName().substring( 5 ).replace( "_", "-" ) );
			if ( header != null ) {
				return putAndReturn( key, header );
			}
		}

		return null;
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 */
	@Override
	public Set<Entry<Key, Object>> entrySet() {
		// combine the known keys
		Set<Key> allKeys = new LinkedHashSet<>( knownKeys );
		// with any additional keys added after the fact
		allKeys.addAll( wrapped.keySet() );
		return allKeys.stream().map( key -> new SimpleEntry<>( key, unWrapNullInternal( getRaw( key ) ) ) )
		    .collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	/**
	 * Get the keys that can be dumped by this scope
	 *
	 * @return The keys that can be dumped
	 */
	public Set<Key> getDumpKeys() {
		// return the keys in alphabetical order
		return this.knownKeys;

	}

	/**
	 * Get the keys that can be dumped by this scope
	 *
	 * @return The keys that can be dumped
	 */
	public Set<String> getDumpKeysAsString() {
		// return the keys in alphabetical order
		return this.knownKeys.stream().map( Key::getName ).collect( TreeSet::new, Set::add, Set::addAll );
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 */
	@Override
	public Set<Key> keySet() {
		return this.knownKeys;
	}

	/**
	 * Override the size since we are a virtual scope
	 *
	 * @return The size of the scope
	 */
	@Override
	public int size() {
		return this.knownKeys.size();
	}

	// overide containskey()
	/**
	 * Returns true if this map contains a mapping for the specified key.
	 * 
	 * @param value The key whose presence in this map is to be tested
	 * 
	 * @return true if this map contains a mapping for the specified key.
	 */
	@Override
	public boolean containsKey( Key key ) {
		// Always return true for known keys which are created on-access
		return this.knownKeys.contains( key ) ? true : wrapped.containsKey( key );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Wraps a null value in a NullValue object
	 *
	 * @param value The value to wrap
	 *
	 * @return The wrapped value
	 */
	private Object defaultNullToString( Object value ) {
		return value == null ? "" : value;
	}

	/**
	 * Wraps a null value in a NullValue object
	 *
	 * @param key   The key to wrap
	 * @param value The value to wrap
	 *
	 * @return The wrapped value
	 */
	private Object putAndReturn( Key key, Object value ) {
		wrapped.put( key, wrapNull( value ) );
		return value;
	}

}
