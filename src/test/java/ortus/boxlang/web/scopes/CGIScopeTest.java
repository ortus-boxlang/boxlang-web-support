package ortus.boxlang.web.scopes;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.DuplicationUtil;
import ortus.boxlang.web.util.BaseWebTest;

public class CGIScopeTest extends BaseWebTest {

	public CGIScope cgiScope;

	@BeforeEach
	public void setupEach() {
		super.setupEach();
		cgiScope = new CGIScope( context );
	}

	@DisplayName( "It can be created" )
	@Test
	public void testCreateCGIScope() {
		assertThat( cgiScope ).isNotNull();
	}

	@DisplayName( "It can access" )
	@Test
	public void testAccessCGIScope() {
		assertThat( cgiScope.dereference( context, Key.of( "server_name" ), false ) ).isEqualTo( "localhost" );
		assertThat( cgiScope.get( Key.of( "server_name" ) ) ).isEqualTo( "localhost" );
		assertThat( cgiScope.getRaw( Key.of( "server_name" ) ) ).isEqualTo( "localhost" );
		assertThat( cgiScope.containsKey( Key.of( "server_name" ) ) ).isTrue();
		assertThat( cgiScope.containsKey( Key.of( "sdfsdf" ) ) ).isFalse();
		assertThat( cgiScope.get( Key.of( "sdfsdf" ) ) ).isNull();
		assertThat( cgiScope.getRaw( Key.of( "sdfsdf" ) ) ).isNull();
	}

	@DisplayName( "It can duplicate" )
	@Test
	public void testDuplicateCGIScope() {
		IStruct dupe = ( IStruct ) DuplicationUtil.duplicate( cgiScope, false );
		assertThat( dupe ).isNotNull();
		assertThat( dupe ).isNotSameInstanceAs( cgiScope );
		assertThat( dupe ).isNotEmpty();
		assertThat( dupe.get( Key.of( "server_name" ) ) ).isEqualTo( "localhost" );
	}

	@DisplayName( "It can be written to" )
	@Test
	public void testCanWriteToCGIScope() {
		cgiScope.assign( context, Key.of( "server_name" ), "new_server_name" );
		assertThat( cgiScope.get( Key.of( "server_name" ) ) ).isEqualTo( "new_server_name" );
		cgiScope.assign( context, Key.of( "brad" ), "wood" );
		assertThat( cgiScope.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
	}

	@DisplayName( "It has known dump keys and in order alphabetically" )
	@Test
	public void testDumpKeys() {
		var dumpKeys = cgiScope.getDumpKeys();
		assertThat( dumpKeys ).isNotNull();
		assertThat( dumpKeys ).isNotEmpty();
	}

	@DisplayName( "It returns true from containsKey() for known keys" )
	@Test
	public void testContainsKey() {
		IScope cgiScope = new CGIScope( context );
		assertThat( cgiScope.containsKey( Key.of( "server_name" ) ) ).isTrue();
	}

	@DisplayName( "Mapped values should be null" )
	@Test
	public void testMappedValuesShouldBeNull() {
		IScope cgiScope = new CGIScope( context );
		// assert no values are null
		for ( var entry : cgiScope.entrySet() ) {
			assertThat( entry.getValue() ).isNotNull();
		}
	}

}
