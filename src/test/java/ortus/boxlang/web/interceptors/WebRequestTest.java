package ortus.boxlang.web.interceptors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

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
		assertDoesNotThrow( () -> runtime.executeSource(
		    """
		    getBoxRuntime().getInterceptorService().announce( "writeToBrowser", interceptData );
		    """,
		    context )
		);

	}
}
