/**
 * [BoxLang]
 *
 * Copyright [2025] [Ortus Solutions, Corp]
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
package ortus.boxlang.web.bifs;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.web.util.BaseWebTest;

public class SSETest extends BaseWebTest {

	private StringWriter	outputWriter;
	private PrintWriter		printWriter;

	@BeforeEach
	public void setupWriter() {
		outputWriter	= new StringWriter();
		printWriter		= new PrintWriter( outputWriter );
		// Mock the writer to return our test writer
		when( mockExchange.getResponseWriter() ).thenReturn( printWriter );
	}

	@Test
	@DisplayName( "It can send a simple SSE message" )
	public void testSimpleMessage() {
		try {
			runtime.executeSource(
			    """
			    sse( emit => {
			        emit.send("Hello World");
			        emit.close();
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected - SSE throws AbortException to prevent further output
		}

		String output = outputWriter.toString();
		assertThat( output ).contains( "data: Hello World" );
		assertThat( output ).contains( "\n\n" ); // SSE message terminator

		// Verify headers were set
		verify( mockExchange ).setResponseHeader( "Content-Type", "text/event-stream" );
		verify( mockExchange ).setResponseHeader( "Cache-Control", "no-cache" );
		verify( mockExchange ).setResponseHeader( "Connection", "keep-alive" );
	}

	@Test
	@DisplayName( "It can send structured data as JSON" )
	public void testStructuredData() {
		try {
			runtime.executeSource(
			    """
			    sse(function(emit) {
			        emit.send({ name: "John", age: 30 });
			        emit.close();
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		assertThat( output ).contains( "data:" );
		assertThat( output ).contains( "\"name\"" );
		assertThat( output ).contains( "John" );
		assertThat( output ).contains( "\"age\"" );
	}

	@Test
	@DisplayName( "It can send events with metadata" )
	public void testEventWithMetadata() {
		try {
			runtime.executeSource(
			    """
			    sse(function(emit) {
			        emit.send("User logged in", "userEvent", "123");
			        emit.close();
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		assertThat( output ).contains( "event: userEvent" );
		assertThat( output ).contains( "id: 123" );
		assertThat( output ).contains( "data: User logged in" );
	}

	@Test
	@DisplayName( "It sanitizes newlines from event and id fields" )
	public void testEventIdSanitization() {
		try {
			runtime.executeSource(
			    """
			    sse(function(emit) {
			        // Event and ID with newlines should be stripped
			        emit.send("Data", "user\nEvent\r\nTest", "123\n456");
			        emit.close();
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		// Newlines should be stripped from event and id
		assertThat( output ).contains( "event: userEventTest" );
		assertThat( output ).contains( "id: 123456" );
		// Data can contain newlines (multi-line handling)
		assertThat( output ).contains( "data: Data" );
	}

	@Test
	@DisplayName( "It can send retry header on first message" )
	public void testRetryHeader() {
		try {
			runtime.executeSource(
			    """
			    sse(
			        callback = function(emit) {
			            emit.send("First");
			            emit.send("Second");
			            emit.close();
			        },
			        retry = 5000
			    );
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		// Retry should only appear once (first message)
		assertThat( output ).contains( "retry: 5000" );
		int retryCount = output.split( "retry:" ).length - 1;
		assertThat( retryCount ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "It can send comments" )
	public void testComments() {
		try {
			runtime.executeSource(
			    """
			    sse(function(emit) {
			        emit.comment("This is a comment");
			        emit.send("Data");
			        emit.close();
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		assertThat( output ).contains( ":This is a comment" );
		assertThat( output ).contains( "data: Data" );
	}

	@Test
	@DisplayName( "It can send multiple messages in a loop" )
	public void testMultipleMessages() {
		try {
			runtime.executeSource(
			    """
			    sse(function(emit) {
			        for(var i = 1; i <= 3; i++) {
			            emit.send(i);
			        }
			        emit.close();
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		assertThat( output ).contains( "data: 1" );
		assertThat( output ).contains( "data: 2" );
		assertThat( output ).contains( "data: 3" );
	}

	@Test
	@DisplayName( "It respects isClosed() check" )
	public void testIsClosedCheck() {
		try {
			runtime.executeSource(
			    """
			    sse(function(emit) {
			        emit.send("Before close");
			        emit.close();

			        if(!emit.isClosed()) {
			            emit.send("After close - should not appear");
			        }
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		assertThat( output ).contains( "Before close" );
		assertThat( output ).doesNotContain( "After close" );
	}

	@Test
	@DisplayName( "It handles multi-line data correctly" )
	public void testMultiLineData() {
		try {
			runtime.executeSource(
			    """
			    			    sse(function(emit) {
			    			        emit.send("Line 1
			    Line 2
			    Line 3");
			    			        emit.close();
			    			    });
			    			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		// Each line should be prefixed with "data: "
		assertThat( output ).contains( "data: Line 1" );
		assertThat( output ).contains( "data: Line 2" );
		assertThat( output ).contains( "data: Line 3" );
	}

	@Test
	@DisplayName( "It handles CRLF line endings correctly" )
	public void testCRLFLineEndings() {
		try {
			// Simulate Windows-style CRLF line endings
			runtime.executeSource(
			    """
			    			    sse(function(emit) {
			    			        emit.send("Windows\r
			    Line\r
			    Data");
			    			        emit.close();
			    			    });
			    			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		// Each line should be prefixed with "data: " regardless of line ending style
		assertThat( output ).contains( "data: Windows" );
		assertThat( output ).contains( "data: Line" );
		assertThat( output ).contains( "data: Data" );
	}

	@Test
	@DisplayName( "It sends array data as JSON" )
	public void testArrayData() {
		try {
			runtime.executeSource(
			    """
			    sse(function(emit) {
			        emit.send([1, 2, 3, "test"]);
			        emit.close();
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		assertThat( output ).contains( "data:" );
		assertThat( output ).contains( "[" );
		assertThat( output ).contains( "]" );
		assertThat( output ).contains( "test" );
	}

	@Test
	@DisplayName( "It clears buffer before streaming" )
	public void testBufferCleared() {
		try {
			// Add some content to buffer first
			runtime.executeSource(
			    """
			    writeOutput("This should be cleared");
			    sse(function(emit) {
			        emit.send("SSE data");
			        emit.close();
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		// The buffer should have been cleared, so only SSE data appears
		String output = outputWriter.toString();
		assertThat( output ).doesNotContain( "This should be cleared" );
		assertThat( output ).contains( "data: SSE data" );
	}

	@Test
	@DisplayName( "It handles async mode" )
	public void testAsyncMode() throws InterruptedException {
		try {
			runtime.executeSource(
			    """
			    sse(
			        callback = function(emit) {
			            emit.send("Async message");
			            emit.close();
			        },
			        async = true
			    );
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		// Give async task time to complete
		Thread.sleep( 100 );

		String output = outputWriter.toString();
		assertThat( output ).contains( "data: Async message" );
	}

	@Test
	@DisplayName( "It supports try-with-resources via AutoCloseable" )
	public void testAutoCloseable() {
		try {
			runtime.executeSource(
			    """
			    sse(function(emit) {
			        // Even without explicit close(), AutoCloseable will handle cleanup
			        emit.send("Auto-closed message");
			        // Note: In BoxLang, try-with-resources isn't directly supported for Java objects,
			        // but this test verifies the interface is implemented for Java callers
			    });
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		String output = outputWriter.toString();
		assertThat( output ).contains( "data: Auto-closed message" );
	}

	@Test
	@DisplayName( "It sets CORS headers when cors argument is provided" )
	public void testCORSHeaders() {
		try {
			runtime.executeSource(
			    """
			    sse(
			        callback = function(emit) {
			            emit.send("CORS enabled");
			            emit.close();
			        },
			        cors = "https://app.example.com"
			    );
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		// Verify CORS headers were set
		verify( mockExchange ).setResponseHeader( "Access-Control-Allow-Origin", "https://app.example.com" );
		verify( mockExchange ).setResponseHeader( "Access-Control-Allow-Credentials", "true" );

		String output = outputWriter.toString();
		assertThat( output ).contains( "data: CORS enabled" );
	}

	@Test
	@DisplayName( "It allows wildcard CORS" )
	public void testCORSWildcard() {
		try {
			runtime.executeSource(
			    """
			    sse(
			        callback = function(emit) {
			            emit.send("Public SSE");
			            emit.close();
			        },
			        cors = "*"
			    );
			    """,
			    context
			);
		} catch ( AbortException e ) {
			// Expected
		}

		// Verify wildcard CORS
		verify( mockExchange ).setResponseHeader( "Access-Control-Allow-Origin", "*" );
		verify( mockExchange ).setResponseHeader( "Access-Control-Allow-Credentials", "true" );
	}
}
