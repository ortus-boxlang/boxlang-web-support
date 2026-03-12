package ortus.boxlang.web.handlers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution( ExecutionMode.SAME_THREAD )
public class WebErrorHandlerTest extends ortus.boxlang.web.util.BaseWebTest {

	static final String	WEBROOT_PATH	= Path.of( "src/test/resources/webroot" ).toAbsolutePath().toString();
	static final String	CUSTOM_TEMPLATE	= "/customError.bxm";
	static final String	BAD_TEMPLATE	= "/badError.bxm";
	static final String	INVALID_PATH	= "/nonexistent/path/that/does/not/exist/error.bxm";

	@BeforeAll
	public void createTemplates() throws IOException {
		Files.createDirectories( Path.of( WEBROOT_PATH ) );

		// Working custom template
		Files.writeString( Path.of( WEBROOT_PATH + CUSTOM_TEMPLATE ),
		    "<bx:output>CUSTOM_ERROR_PAGE:#request.error.getMessage()#</bx:output>" );

		// Broken Custom Template
		Files.writeString( Path.of( WEBROOT_PATH + BAD_TEMPLATE ), "<bx:output>#thisVarDoesNotExist.willCauseAnError#</bx:output>" );
	}

	@BeforeEach
	public void resetConfig() {
		// Reset template path
		runtime.getConfiguration().globalErrorTemplate = "";
	}

	@DisplayName( "Falls back to default error page if no template is configured" )
	@Test
	public void testNoTemplateConfigured() {
		runtime.getConfiguration().globalErrorTemplate = "";

		RuntimeException e = new RuntimeException( "No Custom Template" );

		assertDoesNotThrow( () -> WebErrorHandler.handleError( ( e ), mockExchange, context, null, null ) );
	}

	@DisplayName( "Falls back on default error page if configured path does not exist" )
	@Test
	public void testInvalidTemplatePath() {
		runtime.getConfiguration().globalErrorTemplate = INVALID_PATH;

		RuntimeException e = new RuntimeException( "Path does not exist" );

		assertDoesNotThrow( () -> WebErrorHandler.handleError( e, mockExchange, context, null, null ) );
	}

	@DisplayName( "Error passed to template" )
	@Test
	public void testWorkingCustomTemplate() {
		runtime.getConfiguration().globalErrorTemplate = CUSTOM_TEMPLATE;

		RuntimeException e = new RuntimeException( "Data Check" );

		WebErrorHandler.handleError( e, mockExchange, context, null, null );

		String output = context.getBuffer().toString();
		assertThat( output ).contains( "CUSTOM_ERROR_PAGE" );
		assertThat( output ).contains( "Data Check" );
	}

	@DisplayName( "Falls back on default error page if custom template throws error" )
	@Test
	public void testTemplateExecutionError() {
		runtime.getConfiguration().globalErrorTemplate = BAD_TEMPLATE;

		RuntimeException e = new RuntimeException( "Template execution failure" );

		assertDoesNotThrow( () -> WebErrorHandler.handleError( e, mockExchange, context, null, null ) );
	}

	@DisplayName( "HTTP 500 status" )
	@Test
	public void testResponseStatus() {
		runtime.getConfiguration().globalErrorTemplate = "";

		RuntimeException e = new RuntimeException( "500 Status Check Error" );

		WebErrorHandler.handleError( e, mockExchange, context, null, null );

		verify( mockExchange ).setResponseStatus( 500 );
	}
}
