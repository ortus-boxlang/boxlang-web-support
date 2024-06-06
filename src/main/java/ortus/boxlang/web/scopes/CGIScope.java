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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

/**
 * Variables scope implementation in BoxLang
 */
public class CGIScope extends BaseScope {

	Set<Key>					knownKeys	= new HashSet<Key>( Arrays.asList(
	    Key.auth_password,
	    Key.auth_type,
	    Key.auth_user,
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
	    Key.http_accept,
	    Key.http_accept_encoding,
	    Key.http_accept_language,
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
	    Key.path_translated,
	    Key.query_string,
	    Key.remote_addr,
	    Key.remote_host,
	    Key.remote_user,
	    Key.request_method,
	    Key.request_url,
	    Key.script_name,
	    Key.server_name,
	    Key.server_port,
	    Key.server_port_secure,
	    Key.server_protocol,
	    Key.server_software,
	    Key.web_server_api ) );

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key		name		= Key.of( "cgi" );

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
	 * Assign a value to a key
	 *
	 * @param key   The key to assign
	 * @param value The value to assign
	 */
	@Override
	public Object assign( IBoxContext context, Key key, Object value ) {
		throw new BoxRuntimeException( "Cannot assign to the CGI scope" );
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

		if ( key.equals( Key.script_name ) ) {
			return exchange.getRequestURI();
		}
		if ( key.equals( Key.server_name ) ) {
			return exchange.getRequestServerName();
		}
		if ( key.equals( Key.server_port ) ) {
			return exchange.getRequestServerPort();
		}
		if ( key.equals( Key.query_string ) ) {
			return exchange.getRequestQueryString();
		}
		if ( key.equals( Key.http_host ) ) {
			return exchange.getRequestServerName();
		}
		if ( key.equals( Key.request_method ) ) {
			return exchange.getRequestMethod();
		}
		if ( key.equals( Key.content_type ) ) {
			String result = exchange.getRequestHeader( "Content-Type" );
			return result == null ? "" : result;
		}

		if ( key.equals( Key.path_info ) ) {
			String pathInfo = exchange.getRequestPathInfo();
			return pathInfo == null ? "" : pathInfo;
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
		 * cf_template_path
		 * content_length
		 * context_path
		 * gateway_interface
		 * http_accept
		 * http_accept_encoding
		 * http_accept_language
		 * http_connection
		 * http_cookie
		 * http_referer
		 * http_user_agent
		 * https_keysize
		 * https_secretkeysize
		 * https_server_issuer
		 * https_server_subject
		 * https,
		 * local_addr
		 * local_host
		 * path_translated
		 * remote_addr
		 * remote_host
		 * remote_user
		 * request_url
		 * server_name
		 * server_port
		 * server_port_secure
		 * server_protocol
		 * server_software
		 * web_server_api
		 */

		// HTTP header fallbacks
		String header = exchange.getRequestHeader( key.getName() );
		if ( header != null ) {
			return header;
		}
		if ( key.getName().toLowerCase().startsWith( "http" ) ) {
			header = exchange.getRequestHeader( key.getName().substring( 5 ) );
			if ( header != null ) {
				return header;
			}
		}

		// CGI scope NEVER errors. It simply returns empty string if the key is not
		// found
		return "";
	}

	public Set<Key> getDumpKeys() {
		return knownKeys;
	}
}
