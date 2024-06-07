package ortus.boxlang.web.scopes;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.web.util.BaseWebTest;

public class CGIScopeTest extends BaseWebTest {

	public CGIScope cgiScope;

	@BeforeEach
	public void setupEach() {
		cgiScope = new CGIScope( mockExchange );
	}

	@DisplayName( "It can be created" )
	@Test
	public void testCreateCGIScope() {
		assertThat( cgiScope ).isNotNull();
	}

	@DisplayName( "It has known dump keys and in order alphabetically" )
	@Test
	public void testDumpKeys() {
		var dumpKeys = cgiScope.getDumpKeys();
		assertThat( dumpKeys ).isNotNull();
		assertThat( dumpKeys ).isNotEmpty();
	}

}
