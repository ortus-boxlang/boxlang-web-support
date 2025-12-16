package ortus.boxlang.web.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
