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

import java.util.Date;

public class BoxCookie {

	private final String	name;
	private String			value;
	private String			path;
	private String			domain;
	private Integer			maxAge;
	private Date			expires;
	private boolean			discard;
	private boolean			secure;
	private boolean			httpOnly;
	private int				version	= 1;
	private String			comment;
	private boolean			sameSite;
	private String			sameSiteMode;

	public BoxCookie( final String name, final String value ) {
		this.name	= name;
		this.value	= value;
	}

	public BoxCookie( final String name ) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

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
			System.out.println( "Ignoring invalid cookie same site mode: " + mode );
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
		return "{BoxCookie@" + System.identityHashCode( this ) + " name=" + getName() + " path=" + getPath() + " domain=" + getDomain() + "}";
	}

}
