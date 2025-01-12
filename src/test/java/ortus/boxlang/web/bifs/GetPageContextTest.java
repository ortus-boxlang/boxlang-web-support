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
		    context );
		// @formatter:on

		// Check the result
		assertThat( variables.get( result ) ).isInstanceOf( GetPageContext.PageContext.class );
	}

	@DisplayName( "test buffer clearing" )
	@Test
	public void testPageContextBufferOps() {
		// @formatter:off
		runtime.executeSource(
		    """
		    	getPageContext().getOut().clearBuffer();
		    	getPageContext().clearBuffer();
				getPageContext().reset();
		    """,
		    context );
		// @formatter:on
	}

}
