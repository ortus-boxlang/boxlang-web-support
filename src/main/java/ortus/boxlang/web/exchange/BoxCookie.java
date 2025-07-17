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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.logging.BoxLangLogger;

public class BoxCookie {

	private final String		name;
	// This will always contain the NON-encoded value, even if encodeValue is set to true.
	// The encoded value is returned by getEncodedValue()
	private String				value;
	private String				path;
	private String				domain;
	private Integer				maxAge;
	private Date				expires;
	private boolean				discard;
	private boolean				secure;
	private boolean				httpOnly;
	private int					version	= 1;
	private String				comment;
	private boolean				sameSite;
	private String				sameSiteMode;
	private boolean				encodeValue;

	private final BoxLangLogger	logger	= BoxRuntime.getInstance()
	    .getLoggingService()
	    .EXCEPTION_LOGGER;

	public BoxCookie( final String name, final String value ) {
		this( name, value, true );
	}

	public BoxCookie( final String name, final String value, Boolean encodeValue ) {
		this.name			= name;
		this.value			= value;
		this.encodeValue	= encodeValue;
	}

	public BoxCookie( final String name ) {
		this( name, null );
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Get the encoded cookie value in a format suitable for the HTTP header.
	 * If encodeValue is set to false, this will return the value as-is.
	 *
	 * @return The cookie value, encoded if necessary
	 */
	public String getEncodedValue() {
		if ( encodeValue && value != null ) {
			return URLEncoder.encode( value, java.nio.charset.StandardCharsets.UTF_8 );
		}
		return value;
	}

	/**
	 * This always returns the NON-encoded value, even if encodeValue is set to true.
	 *
	 * @param value The cookie value with no encoding. Do NOT use this to set an HTTP SET-COOKIE header.
	 *
	 * @return The BoxCookie instance for chaining
	 */
	public BoxCookie setValue( final String value ) {
		this.value = value;
		return this;
	}

	public String getPath() {
		return path;
	}

	public BoxCookie setPath( final String path ) {
		this.path = path;
		return this;
	}

	public String getDomain() {
		return domain;
	}

	public BoxCookie setDomain( final String domain ) {
		this.domain = domain;
		return this;
	}

	public Integer getMaxAge() {
		return maxAge;
	}

	public BoxCookie setMaxAge( final Integer maxAge ) {
		this.maxAge = maxAge;
		return this;
	}

	public boolean isDiscard() {
		return discard;
	}

	public BoxCookie setDiscard( final boolean discard ) {
		this.discard = discard;
		return this;
	}

	public boolean isSecure() {
		return secure;
	}

	public BoxCookie setSecure( final boolean secure ) {
		this.secure = secure;
		return this;
	}

	public int getVersion() {
		return version;
	}

	public BoxCookie setVersion( final int version ) {
		this.version = version;
		return this;
	}

	public boolean isHttpOnly() {
		return httpOnly;
	}

	public BoxCookie setHttpOnly( final boolean httpOnly ) {
		this.httpOnly = httpOnly;
		return this;
	}

	public Date getExpires() {
		return expires;
	}

	public BoxCookie setExpires( final Date expires ) {
		this.expires = expires;
		return this;
	}

	public String getComment() {
		return comment;
	}

	public BoxCookie setComment( final String comment ) {
		this.comment = comment;
		return this;
	}

	public boolean isSameSite() {
		return sameSite;
	}

	public BoxCookie setSameSite( final boolean sameSite ) {
		this.sameSite = sameSite;
		return this;
	}

	public String getSameSiteMode() {
		return sameSiteMode;
	}

	/**
	 * Set the SameSite mode for the cookie. This will automatically set the SameSite flag to true.
	 *
	 * @param mode One of the strings Strict, Lax, or None
	 *
	 * @return
	 */
	public BoxCookie setSameSiteMode( final String mode ) {
		if ( mode == null ) {
			return this;
		}
		final String m = CookieSameSiteMode.lookupModeString( mode );
		if ( m != null ) {
			this.sameSiteMode = m;
			this.setSameSite( true );
		} else {
			logger.error( "Ignoring invalid cookie same site mode: " + mode );
		}
		return this;
	}

	@Override
	public final int hashCode() {
		int result = 17;
		result	= 37 * result + ( getName() == null ? 0 : getName().hashCode() );
		result	= 37 * result + ( getPath() == null ? 0 : getPath().hashCode() );
		result	= 37 * result + ( getDomain() == null ? 0 : getDomain().hashCode() );
		return result;
	}

	@Override
	public final boolean equals( final Object other ) {
		if ( other == this )
			return true;
		if ( ! ( other instanceof BoxCookie ) )
			return false;
		final BoxCookie o = ( BoxCookie ) other;
		// compare names
		if ( getName() == null && o.getName() != null )
			return false;
		if ( getName() != null && !getName().equals( o.getName() ) )
			return false;
		// compare paths
		if ( getPath() == null && o.getPath() != null )
			return false;
		if ( getPath() != null && !getPath().equals( o.getPath() ) )
			return false;
		// compare domains
		if ( getDomain() == null && o.getDomain() != null )
			return false;
		if ( getDomain() != null && !getDomain().equals( o.getDomain() ) )
			return false;
		// same cookie
		return true;
	}

	@Override
	public final String toString() {
		return "{BoxCookie@" + System.identityHashCode( this ) + " name=" + getName() + " path=" + getPath() + " domain=" + getDomain() + " value=" + getValue()
		    + " secure=" + isSecure() + " httpOnly=" + isHttpOnly() + " expires=" + getExpires() + " maxAge=" + getMaxAge() + " discard=" + isDiscard()
		    + " version=" + getVersion() + " comment=" + getComment() + " sameSite=" + isSameSite() + " sameSiteMode=" + getSameSiteMode() + "}";
	}

	/**
	 * Create a new cookie from an incoming cookie header, decoding the value.
	 *
	 * @param name  The cookie name
	 * @param value The encoded cookie value
	 *
	 * @return A new BoxCookie instance with the name and value set
	 */
	public static BoxCookie fromEncoded( String name, String value ) {
		try {
			if ( value != null ) {
				value = URLDecoder.decode( value, java.nio.charset.StandardCharsets.UTF_8 );
			}
		} catch ( IllegalArgumentException e ) {
			// TODO: remove this later. Just want to see if it happens in the wild.
			System.out.println( "IllegalArgumentException decoding cookie name: [" + name + "] value: [" + value + "] error: " + e.getMessage() );

			// Ignore, just return the cookie with the encoded value
			// This cookie may have been set without any encoding, but there's no way to know that.
		}
		return new BoxCookie( name, value );
	}

	/**
	 * Generate the Set-Cookie header value for this cookie.
	 *
	 * @return A string representing the Set-Cookie header value.
	 */
	public String toSetCookieHeader() {
		StringBuilder header = new StringBuilder();

		// Add the cookie name and value
		header.append( getName() ).append( "=" ).append( getEncodedValue() ).append( "; " );

		// Add the Path attribute if set
		if ( getPath() != null ) {
			header.append( "Path=" ).append( getPath() ).append( "; " );
		}

		// Add the Domain attribute if set
		if ( getDomain() != null ) {
			header.append( "Domain=" ).append( getDomain() ).append( "; " );
		}

		// Add the Max-Age attribute if set
		if ( getMaxAge() != null ) {
			header.append( "Max-Age=" ).append( getMaxAge() ).append( "; " );
		}

		// Add the Expires attribute if set
		if ( getExpires() != null ) {
			header.append( "Expires=" ).append( new SimpleDateFormat( "EEE, dd MMM yyyy HH:mm:ss z" ).format( getExpires() ) ).append( "; " );
		}

		// Add the Secure attribute if set
		if ( isSecure() ) {
			header.append( "Secure; " );
		}

		// Add the HttpOnly attribute if set
		if ( isHttpOnly() ) {
			header.append( "HttpOnly; " );
		}

		// Add the SameSite attribute if set
		if ( isSameSite() && getSameSiteMode() != null ) {
			header.append( "SameSite=" ).append( getSameSiteMode() ).append( "; " );
		}

		// Remove the trailing "; " if present
		if ( header.length() > 2 && header.substring( header.length() - 2 ).equals( "; " ) ) {
			header.setLength( header.length() - 2 );
		}

		return header.toString();
	}
}
