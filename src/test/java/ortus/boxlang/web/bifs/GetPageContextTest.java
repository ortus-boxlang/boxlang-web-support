package ortus.boxlang.web.bifs;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.web.context.BoxlangHTTPRequest;
import ortus.boxlang.web.context.BoxlangHTTPResponse;
import ortus.boxlang.web.util.BaseWebTest;

public class GetPageContextTest extends BaseWebTest {

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@DisplayName( "test get page context" )
	@Test
	public void testGetPageContext() {
		// @formatter:off
		runtime.executeSource(
		    """
		    	result = getPageContext();
		    """,
		    context );
		// @formatter:on

		// Check the result
		assertThat( variables.get( result ) ).isInstanceOf( GetPageContext.PageContext.class );
	}

	@DisplayName( "test buffer clearing" )
	@Test
	public void testPageContextBufferOps() {
		// @formatter:off
		runtime.executeSource(
		    """
		    	getPageContext().getOut().clearBuffer();
		    	getPageContext().clearBuffer();
				getPageContext().reset();
		    """,
		    context );
		// @formatter:on
	}

	@DisplayName( "test get request and response wrappers" )
	@Test
	public void testGetRequestAndResponse() {
		when( mockExchange.getRequestLocalPort() ).thenReturn( 8080 );
		when( mockExchange.getRequestServerPort() ).thenReturn( 8080 );
		when( mockExchange.getRequestScheme() ).thenReturn( "http" );
		when( mockExchange.getRequestHeader( "Host" ) ).thenReturn( "localhost" );

		// @formatter:off
		runtime.executeSource(
		    """
		    	pc = getPageContext();
		    	req = pc.getRequest();
		    	res = pc.getResponse();
		    """,
		    context );
		// @formatter:on

		Object pc = variables.get( Key.of( "pc" ) );
		assertThat( pc ).isInstanceOf( GetPageContext.PageContext.class );

		Object req = variables.get( Key.of( "req" ) );
		assertThat( req ).isInstanceOf( BoxlangHTTPRequest.class );
		assertThat( req ).isNotSameInstanceAs( pc );

		BoxlangHTTPRequest request = ( BoxlangHTTPRequest ) req;
		assertThat( request.getLocalPort() ).isEqualTo( 8080 );
		assertThat( request.getServerPort() ).isEqualTo( 8080 );
		assertThat( request.getScheme() ).isEqualTo( "http" );
		assertThat( request.getHeader( "Host" ) ).isEqualTo( "localhost" );
		assertThat( request.getHeaderNames() ).isInstanceOf( Array.class );
		assertThat( request.getAttributeNames() ).isInstanceOf( Array.class );
		assertThat( request.getCookies() ).isInstanceOf( Array.class );

		Object res = variables.get( Key.of( "res" ) );
		assertThat( res ).isInstanceOf( BoxlangHTTPResponse.class );
		assertThat( res ).isNotSameInstanceAs( pc );

		BoxlangHTTPResponse response = ( BoxlangHTTPResponse ) res;
		assertThat( response.getStatus() ).isEqualTo( 200 );

		response.setStatus( 201 );
		assertThat( response.getStatus() ).isEqualTo( 201 );

		response.setHeader( "X-Test", "1" );
		assertThat( response.getHeader( "X-Test" ) ).isEqualTo( "1" );
		assertThat( response.getHeaderNames() ).isInstanceOf( Array.class );
		assertThat( response.getCookies() ).isInstanceOf( Array.class );
	}

}
