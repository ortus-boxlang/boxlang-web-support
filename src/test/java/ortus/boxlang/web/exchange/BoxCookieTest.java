package ortus.boxlang.web.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;

public class BoxCookieTest {

	static BoxRuntime instance;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@Test
	@DisplayName( "It tests the output of the cookie header string" )
	public void testBoxCookieHeaderString() {
		BoxCookie cookie = new BoxCookie( "TestCookie", "TestValue" );
		cookie.setPath( "/" );
		cookie.setDomain( "example.com" );
		cookie.setSecure( true );
		cookie.setHttpOnly( true );
		cookie.setSameSite( true );
		cookie.setSameSiteMode( "Lax" );
		cookie.setExpires( DateTimeCaster.cast( "2024-12-31T23:59:59-05:00" ).toDate() );

		String headerString = cookie.toSetCookieHeader();

		// System.out.println( "Set-Cookie Header: " + headerString );

		assertEquals( "TestCookie=TestValue; Path=/; Domain=example.com; Expires=Wed, 01 Jan 2025 04:59:59 GMT; Secure; HttpOnly; SameSite=Lax", headerString );
	}

	@Test
	@DisplayName( "It can parse expires as a number of days" )
	public void testApplyExpiresNumber() {
		BoxCookie cookie = new BoxCookie( "test", "val" );
		BoxCookie.applyExpires( cookie, 1 );
		assertEquals( Integer.valueOf( 86400 ), cookie.getMaxAge() );
	}

	@Test
	@DisplayName( "It can parse expires as the string 'now'" )
	public void testApplyExpiresNow() {
		BoxCookie cookie = new BoxCookie( "test", "val" );
		BoxCookie.applyExpires( cookie, "now" );
		assertEquals( Integer.valueOf( 0 ), cookie.getMaxAge() );
	}

	@Test
	@DisplayName( "It can parse expires as the string 'never'" )
	public void testApplyExpiresNever() {
		BoxCookie cookie = new BoxCookie( "test", "val" );
		BoxCookie.applyExpires( cookie, "never" );
		assertEquals( Integer.valueOf( 60 * 60 * 24 * 365 * 30 ), cookie.getMaxAge() );
	}

	@Test
	@DisplayName( "It can parse expires as a date string" )
	public void testApplyExpiresDateString() {
		BoxCookie cookie = new BoxCookie( "test", "val" );
		BoxCookie.applyExpires( cookie, "2025-12-31T23:59:59-05:00" );
		assertNotNull( cookie.getExpires() );
		assertNull( cookie.getMaxAge() );
	}

	@Test
	@DisplayName( "It handles null expires without error" )
	public void testApplyExpiresNull() {
		BoxCookie cookie = new BoxCookie( "test", "val" );
		BoxCookie.applyExpires( cookie, null );
		assertNull( cookie.getMaxAge() );
		assertNull( cookie.getExpires() );
	}
}
