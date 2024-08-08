
/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
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
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxCookie;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.util.KeyDictionary;

public class FileUploadTest {

	static BoxRuntime			runtime;
	IBoxContext					context;
	IScope						variables;
	// Web Assets for mocking
	public WebRequestBoxContext	webContext;
	public IBoxHTTPExchange		mockExchange;
	public static final String	TEST_WEBROOT	= Path.of( "src/test/resources/webroot" ).toAbsolutePath().toString();
	// Test Constants
	static Key					result			= new Key( "result" );
	static String				testURLImage	= "https://ortus-public.s3.amazonaws.com/logos/ortus-medium.jpg";
	static String				tmpDirectory	= "src/test/resources/tmp/FileUpload";
	static String				testUpload		= tmpDirectory + "/test.jpg";

	static String[]				testFields		= new String[] { "file1", "file2", "file3" };

	@BeforeAll
	public static void setUp() throws MalformedURLException, IOException {
		runtime = BoxRuntime.getInstance( true );
		System.out.println( "Temp Directory Exists " + FileSystemUtil.exists( tmpDirectory ) );
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory, true, null );
		}
		if ( !FileSystemUtil.exists( testUpload ) ) {
			BufferedInputStream urlStream = new BufferedInputStream( URI.create( testURLImage ).toURL().openStream() );
			FileSystemUtil.write( testUpload, urlStream.readAllBytes(), true );
		}
	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
	}

	@BeforeEach
	public void setupEach() throws IOException {
		if ( !FileSystemUtil.exists( testUpload ) ) {
			BufferedInputStream urlStream = new BufferedInputStream( URI.create( testURLImage ).toURL().openStream() );
			FileSystemUtil.write( testUpload, urlStream.readAllBytes(), true );
		}

		// Mock a connection
		mockExchange = Mockito.mock( IBoxHTTPExchange.class );
		ArrayList<ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload> mockUploads = new ArrayList<ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload>();

		Stream.of( testFields ).forEach( field -> {
			Path fieldFile = Path.of( tmpDirectory, field + ".jpg" );
			try {
				Files.copy( Path.of( testUpload ), fieldFile, StandardCopyOption.REPLACE_EXISTING );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
			mockUploads.add( new ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload( Key.of( field ), fieldFile, fieldFile.getFileName().toString() ) );
		} );

		ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload[] uploadsArray = mockUploads
		    .toArray( new ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload[ 0 ] );

		when( mockExchange.getUploadData() ).thenReturn( uploadsArray );
		when( mockExchange.getRequestCookies() ).thenReturn( new BoxCookie[ 0 ] );
		when( mockExchange.getRequestHeaderMap() ).thenReturn( new HashMap<String, String[]>() );
		// when( mockExchange.getRequestHeader( String name ) ).thenReturn( null );

		// Create the mock contexts
		context		= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		webContext	= new WebRequestBoxContext( context, mockExchange, TEST_WEBROOT );
		variables	= webContext.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF FileUpload" )
	@Test
	public void testBifFileUpload() {
		variables.put( Key.of( "filefield" ), testFields[ 1 ] );
		variables.put( Key.directory, Path.of( tmpDirectory ).toAbsolutePath().toString() );
		runtime.executeSource(
		    """
		      result = FileUpload(
		      	filefield = filefield,
		      	destination = directory,
		    nameconflict = "makeunique"
		      );
		      """,
		    webContext );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct fileInfo = variables.getAsStruct( result );

		// System.out.println( fileInfo.asString() );

		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isInstanceOf( String.class );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isNotEmpty();
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( Path.of( tmpDirectory, testFields[ 1 ] + ".jpg" ).toAbsolutePath().toString() );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
		assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image/jpeg" );
		assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
		assertThat( fileInfo.get( KeyDictionary.fileSize ) ).isEqualTo( fileInfo.get( KeyDictionary.oldFileSize ) );

	}

	@DisplayName( "It tests the BIF FileUpload without a FileField" )
	@Test
	public void testBifFileUploadNoField() {
		variables.put( Key.directory, Path.of( tmpDirectory ).toAbsolutePath().toString() );
		runtime.executeSource(
		    """
		      result = FileUpload(
		      	destination = directory,
		    nameconflict = "makeunique"
		      );
		      """,
		    webContext );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct fileInfo = variables.getAsStruct( result );

		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isInstanceOf( String.class );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isNotEmpty();
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( Path.of( tmpDirectory, testFields[ 0 ] + ".jpg" ).toAbsolutePath().toString() );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
		assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image/jpeg" );
		assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
		assertThat( fileInfo.get( KeyDictionary.fileSize ) ).isEqualTo( fileInfo.get( KeyDictionary.oldFileSize ) );

	}

	@DisplayName( "It tests the BIF FileUploadAll" )
	@Test
	public void testBifFileUploadAll() {
		variables.put( Key.of( "filefield" ), testFields[ 1 ] );
		variables.put( Key.directory, Path.of( tmpDirectory ).toAbsolutePath().toString() );
		runtime.executeSource(
		    """
		        result = FileUploadAll(
		        	destination = directory,
		    nameconflict = "makeunique"
		        );
		        """,
		    webContext );

		assertThat( variables.get( result ) ).isInstanceOf( Array.class );

		variables.getAsArray( result ).stream().map( StructCaster::cast ).forEach( fileInfo -> {
			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isInstanceOf( String.class );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isNotEmpty();
			assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
			    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
			assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
			assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image/jpeg" );
			assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
			assertThat( fileInfo.get( KeyDictionary.fileSize ) ).isEqualTo( fileInfo.get( KeyDictionary.oldFileSize ) );
		} );

	}

}
