package ortus.boxlang.web.util;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

public class BaseWebTest {

	public static BoxRuntime	runtime;
	public static Key			result			= new Key( "result" );
	public static final String	TEST_WEBROOT	= Path.of( "src/test/resources/webroot" ).toAbsolutePath().toString();
	public IBoxContext			context;
	public IScope				variables;
	// Web Assets for mocking
	public WebRequestBoxContext	webContext;
	public IBoxHTTPExchange		mockExchange;

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
		mockExchange	= Mockito.mock( IBoxHTTPExchange.class );

		// Create the mock contexts
		context			= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		webContext		= new WebRequestBoxContext( context, mockExchange, TEST_WEBROOT );
		variables		= webContext.getScopeNearby( VariablesScope.name );
	}

}
