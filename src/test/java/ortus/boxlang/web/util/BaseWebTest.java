package ortus.boxlang.web.util;

import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.interceptors.WebRequest;

@TestInstance( TestInstance.Lifecycle.PER_CLASS )
public class BaseWebTest {

	public BoxRuntime			runtime;
	public Key					result			= new Key( "result" );
	public final String			TEST_WEBROOT	= Path.of( "src/test/resources/webroot" ).toAbsolutePath().toString();
	public WebRequestBoxContext	context;
	public IScope				variables;
	public IBoxHTTPExchange		mockExchange;
	public String				requestURI		= "/";

	@BeforeAll
	public void setUp() {
		runtime = BoxRuntime.getInstance( true );
		// Manually register our interceptor as it doesn't automatically get registered in the test environment
		runtime.getInterceptorService().register( new WebRequest() );
	}

	@BeforeEach
	public void setupEach() {
		// Mock a connection
		mockExchange = Mockito.mock( IBoxHTTPExchange.class );
		// Mock some objects which are used in the context
		when( mockExchange.getRequestCookies() ).thenReturn( new BoxCookie[ 0 ] );
		when( mockExchange.getRequestHeaderMap() ).thenReturn( new HashMap<String, String[]>() );
		when( mockExchange.getResponseWriter() ).thenReturn( new PrintWriter( OutputStream.nullOutputStream() ) );

		// Create the mock contexts
		context		= new WebRequestBoxContext( runtime.getRuntimeContext(), mockExchange, TEST_WEBROOT );
		variables	= context.getScopeNearby( VariablesScope.name );

		try {
			context.loadApplicationDescriptor( new URI( requestURI ) );
		} catch ( URISyntaxException e ) {
			throw new BoxRuntimeException( "Invalid URI", e );
		}

		BaseApplicationListener appListener = context.getApplicationListener();
		appListener.onRequestStart( context, new Object[] { requestURI } );
	}

}
