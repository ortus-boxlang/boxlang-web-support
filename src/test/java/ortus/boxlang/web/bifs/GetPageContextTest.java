package ortus.boxlang.web.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.web.util.BaseWebTest;

public class GetPageContextTest extends BaseWebTest {

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@DisplayName( "test get page context" )
	@Test
	public void testGetPageContext() {
		// @formatter:off
		runtime.executeSource(
		    """
		    	result = getPageContext();
		    """,
		    webContext );
		// @formatter:on

		// Check the result
		var result = variables.get( BaseWebTest.result );
		assertThat( result ).isInstanceOf( GetPageContext.PageContext.class );
	}

}
