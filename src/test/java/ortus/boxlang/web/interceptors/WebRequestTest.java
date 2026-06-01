package ortus.boxlang.web.interceptors;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.web.WebRequestExecutor;

public class WebRequestTest extends ortus.boxlang.web.util.BaseWebTest {

	static Key result = new Key( "result" );

	@DisplayName( "Tests writeToBrowser with an abort directive" )
	@Test
	public void testWriteToBrowserAbort() {
		variables.put(
		    Key.of( "interceptData" ),
		    Struct.of(
		        Key.context, context,
		        Key.content, "foo",
		        Key.mimetype, "text/plain",
		        Key.of( "filename" ), "foo.txt",
		        Key.reset, true,
		        Key.abort, true
		    )
		);

		// @formatter:off
		runtime.executeSource(
		    """
		      getBoxRuntime().getInterceptorService().announce( "writeToBrowser", interceptData )
		   	  result = "foo"
		      """,
		    context );
		// @formatter:on

		// This should have never fired. We can't trap for an abort exception, because the executeSource traps it.
		assertThat( variables.get( result ) ).isEqualTo( null );
	}

	@DisplayName( "Tests writeToBrowser does not overwrite an existing content disposition header" )
	@Test
	public void testWriteToBrowserKeepsExistingContentDispositionHeader() {
		String existingDisposition = "attachment; filename=existing.txt";
		mockExchange.setResponseHeader( WebRequestExecutor.CONTENT_DISPOSITION_HEADER, existingDisposition );

		variables.put(
		    Key.of( "interceptData" ),
		    Struct.of(
		        Key.context, context,
		        Key.content, "foo",
		        Key.mimetype, "text/plain",
		        Key.reset, true
		    )
		);

		// @formatter:off
		runtime.executeSource(
		    """
		      getBoxRuntime().getInterceptorService().announce( "writeToBrowser", interceptData )
		      """,
		    context );
		// @formatter:on

		assertThat( mockExchange.getResponseHeader( WebRequestExecutor.CONTENT_DISPOSITION_HEADER ) ).isEqualTo( existingDisposition );
	}
}
