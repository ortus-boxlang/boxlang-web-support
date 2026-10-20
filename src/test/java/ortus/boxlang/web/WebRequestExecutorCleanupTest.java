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
package ortus.boxlang.web;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.util.MockHTTPExchange;

@DisplayName( "Web Request Executor Cleanup Tests" )
public class WebRequestExecutorCleanupTest {

	private static final String TEST_WEBROOT = Path.of( "src/test/resources/webroot" ).toAbsolutePath().toString();

	@DisplayName( "Request resources are cleaned up even when the final response flush throws" )
	@Test
	void testCleanupRunsWhenFinalResponseFlushThrows() {
		BoxRuntime.getInstance( true );

		MockHTTPExchange exchange = Mockito.spy( new MockHTTPExchange( new BoxCookie[ 0 ], new HashMap<String, String[]>() ) );
		exchange.setRequestURI( "/badError.bxm" );

		// Simulate a failure while flushing the response - e.g. a session persistence or client disconnect failure.
		// Writing the response body and persisting the session scope both funnel through this call.
		Mockito.doThrow( new RuntimeException( "Simulated response flush failure" ) ).when( exchange ).flushResponseBuffer();

		// A failing final flush must be logged, not surfaced to the caller, and must not skip the resource cleanup
		assertDoesNotThrow( () -> WebRequestExecutor.execute( exchange, TEST_WEBROOT, false ) );

		// The request context must always be removed from the thread-local stack, otherwise it - and any open
		// transaction holding a JDBC connection - leaks across requests on a pooled thread
		assertThat( RequestBoxContext.getCurrent() ).isNull();

		// And the context must have been shut down, which is what ends open transactions and releases their connections
		WebRequestBoxContext requestContext = exchange.getWebContext();
		assertThat( requestContext ).isNotNull();
		assertThrows( BoxRuntimeException.class, requestContext::getHTTPExchange );
	}
}
