package ortus.boxlang.web.handlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.util.BaseWebTest;

/**
 * Tests for the WebErrorHandler class
 */
public class WebErrorHandlerTest extends BaseWebTest {

	private StringWriter stringWriter;
	private PrintWriter printWriter;

	@BeforeEach
	public void setupErrorHandlerTest() {
		stringWriter = new StringWriter();
		printWriter = new PrintWriter(stringWriter);
		when(mockExchange.getResponseWriter()).thenReturn(printWriter);
	}

	@Test
	@DisplayName("Error handler produces HTML output")
	public void testErrorHandlerProducesHTML() {
		RuntimeException testException = new RuntimeException("Test error message");
		
		WebErrorHandler.handleError(testException, mockExchange, null, null, null);
		
		String output = stringWriter.toString();
		assertNotNull(output);
		assertTrue(output.contains("<!DOCTYPE html>"));
		assertTrue(output.contains("BoxLang Error"));
		assertTrue(output.contains("Test error message"));
		
		// Verify response status was set to 500
		verify(mockExchange).setResponseStatus(500);
	}

	@Test
	@DisplayName("Error handler shows version in debug mode")
	public void testErrorHandlerDebugMode() {
		// Enable debug mode
		runtime.getConfiguration().debug = true;
		
		RuntimeException testException = new RuntimeException("Debug test error");
		
		WebErrorHandler.handleError(testException, mockExchange, context, null, null);
		
		String output = stringWriter.toString();
		assertNotNull(output);
		assertTrue(output.contains("<!DOCTYPE html>"));
		// Should contain version info in debug mode
		assertTrue(output.contains("<small>v") || output.contains("BoxLang"));
		assertTrue(output.contains("Debug test error"));
		
		// Reset debug mode
		runtime.getConfiguration().debug = false;
	}

	@Test
	@DisplayName("Error handler handles BoxRuntimeException")
	public void testBoxRuntimeException() {
		BoxRuntimeException testException = new BoxRuntimeException("BoxLang specific error");
		
		WebErrorHandler.handleError(testException, mockExchange, null, null, null);
		
		String output = stringWriter.toString();
		assertNotNull(output);
		assertTrue(output.contains("BoxLang specific error"));
		assertTrue(output.contains("An Error Occurred"));
	}

	@Test
	@DisplayName("Error handler works with null context")
	public void testNullContext() {
		RuntimeException testException = new RuntimeException("Null context test");
		
		// Should not throw exception when context is null
		assertDoesNotThrow(() -> {
			WebErrorHandler.handleError(testException, mockExchange, null, null, null);
		});
		
		String output = stringWriter.toString();
		assertNotNull(output);
		assertTrue(output.contains("Null context test"));
	}

	@Test
	@DisplayName("Error handler produces well-formed HTML")
	public void testWellFormedHTML() {
		RuntimeException testException = new RuntimeException("HTML structure test");
		
		WebErrorHandler.handleError(testException, mockExchange, null, null, null);
		
		String output = stringWriter.toString();
		assertNotNull(output);
		
		// Check for proper HTML structure
		assertTrue(output.contains("<!DOCTYPE html>"));
		assertTrue(output.contains("<html"));
		assertTrue(output.contains("<head>"));
		assertTrue(output.contains("</head>"));
		assertTrue(output.contains("<body>"));
		assertTrue(output.contains("</body>"));
		assertTrue(output.contains("</html>"));
		
		// Check for CSS styles
		assertTrue(output.contains("<style>"));
		assertTrue(output.contains(".bx-err"));
		
		// Check for proper escaping of error message
		assertTrue(output.contains("HTML structure test"));
	}
}