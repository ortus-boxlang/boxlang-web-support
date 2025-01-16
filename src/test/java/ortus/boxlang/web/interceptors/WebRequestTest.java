package ortus.boxlang.web.interceptors;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbortException;

public class WebRequestTest extends ortus.boxlang.web.util.BaseWebTest {

	static Key result = new Key( "result" );

	@DisplayName( "Tests writeToBrowser with an abort directive" )
	@Test
	@Disabled
	public void testWriteToBrowserAbort() {
		variables.put(
		    Key.of( "interceptData" ),
		    Struct.of(
		        Key.context, context,
		        Key.content, "foo",
		        Key.mimetype, "application/pdf",
		        Key.of( "filename" ), "foo.txt",
		        Key.reset, true,
		        Key.abort, true
		    )
		);
		assertThrows( AbortException.class, () -> runtime.executeSource(
		    """
		    getBoxRuntime().getInterceptorService().announce( "writeToBrowser", interceptData );
		    """,
		    context ) );
	}
}
