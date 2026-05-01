package ortus.boxlang.web.exchange;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.async.BoxFuture;
import ortus.boxlang.web.util.BaseWebTest;

public class FutureWebContextTest extends BaseWebTest {

	@DisplayName( "A future on io-tasks can access CGI scope after request shutdown" )
	@Test
	public void testFutureAccessesCGIScopeAfterShutdown() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			result = futureNew( function() {
				sleep( 1000 );
				return cgi.script_name;
			}, "io-tasks" );
			""",
			context
		);
		// @formatter:on

		// Simulate what WebRequestExecutor does after the page finishes:
		// shut down the context (which detaches or nulls the exchange)
		context.shutdown();

		// Now wait for the future to complete and get its value
		Object futureRef = variables.get( result );
		assertThat( futureRef ).isInstanceOf( BoxFuture.class );

		// The future's get() should succeed — the background thread should still
		// be able to read CGI scope even though the request context has been shut down.
		Object value = ( ( BoxFuture<?> ) futureRef ).get();
		assertThat( value ).isNotNull();
		assertThat( value.toString() ).isEqualTo( "/" );
	}

}
