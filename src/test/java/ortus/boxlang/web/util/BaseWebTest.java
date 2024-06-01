package ortus.boxlang.web.util;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.xnio.OptionMap;

import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.util.HttpString;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.web.context.WebRequestBoxContext;

public class BaseWebTest {

	public static BoxRuntime	runtime;
	public static Key			result			= new Key( "result" );
	public static final String	TEST_WEBROOT	= Path.of( "src/test/resources/webroot" ).toAbsolutePath().toString();
	public IBoxContext			context;
	public IScope				variables;
	// Web Assets for mocking
	public WebRequestBoxContext	webContext;
	public HttpServerExchange	exchange;
	public ServerConnection		mockConnection;

	@BeforeAll
	public static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		// Mock a connection
		mockConnection = Mockito.mock( ServerConnection.class );
		OptionMap mockOptions = OptionMap.builder()
		    .set( UndertowOptions.MAX_ENTITY_SIZE, 1024L )
		    // Add cookie support
		    .set( UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL, true )
		    .set( UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, true )
		    .getMap();
		// Configure mock behavior
		Mockito.when( mockConnection.getUndertowOptions() ).thenReturn( mockOptions );

		// Create a mock Undertow HTTP exchange
		exchange = new HttpServerExchange( mockConnection );
		exchange.setRequestMethod( HttpString.tryFromString( "GET" ) );
		exchange.getRequestHeaders().put( HttpString.tryFromString( "Content-Type" ), "text/plain" );

		// Mock More things here needed for tests

		// Create the mock contexts
		context		= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		webContext	= new WebRequestBoxContext( context, exchange, TEST_WEBROOT );
		variables	= webContext.getScopeNearby( VariablesScope.name );
	}

}
