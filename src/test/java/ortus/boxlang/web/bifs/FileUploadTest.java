
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.web.util.KeyDictionary;

public class FileUploadTest extends ortus.boxlang.web.util.BaseWebTest {

	ArrayList<ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload>	mockUploads;
	public final String													TEST_WEBROOT	= Path.of( "src/test/resources/webroot" ).toAbsolutePath().toString();
	// Test Constants
	final Key															result			= new Key( "result" );
	final String														testURLImage	= "https://ortus-public.s3.amazonaws.com/logos/ortus-medium.jpg";
	final String														tmpDirectory	= "src/test/resources/tmp/FileUpload";
	final String														testUpload		= tmpDirectory + "/test.jpg";

	final String[]														testFields		= new String[] { "file1", "file2", "file3" };

	@BeforeAll
	public void setUpTempFileSystem() throws MalformedURLException, IOException {
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory, true, null );
		}
		if ( !FileSystemUtil.exists( testUpload ) ) {
			BufferedInputStream urlStream = new BufferedInputStream( URI.create( testURLImage ).toURL().openStream() );
			FileSystemUtil.write( testUpload, urlStream.readAllBytes(), true );
		}
	}

	@AfterAll
	public void teardown() {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
	}

	@BeforeEach
	public void setupUploads() throws IOException, URISyntaxException {
		if ( !FileSystemUtil.exists( testUpload ) ) {
			BufferedInputStream urlStream = new BufferedInputStream( URI.create( testURLImage ).toURL().openStream() );
			FileSystemUtil.write( testUpload, urlStream.readAllBytes(), true );
		}

		mockUploads = new ArrayList<ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload>();

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
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct fileInfo = variables.getAsStruct( result );

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
		    context );

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

	@DisplayName( "It tests the BIF FileUpload with explicitly allowed extensions" )
	@Test
	@Disabled
	public void testBifFileSecurity() {
		variables.put( Key.of( "filefield" ), testFields[ 0 ] );
		variables.put( Key.directory, Path.of( tmpDirectory ).toAbsolutePath().toString() );

		// Test with strict mode off
		runtime.executeSource(
		    """
		            result = FileUpload(
		            	filefield = filefield,
		            	destination = directory,
		          nameconflict = "makeunique",
		       allowedExtensions = "png",
		    strict=false
		            );
		            """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct fileInfo = variables.getAsStruct( result );

		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( Path.of( tmpDirectory, testFields[ 0 ] + ".jpg" ).toAbsolutePath().toString() );
		assertFalse( fileInfo.getAsBoolean( KeyDictionary.fileWasSaved ) );

		// Test with strict mode on
		assertThrows( BoxRuntimeException.class, () -> runtime.executeSource(
		    """
		            result = FileUpload(
		            	filefield = filefield,
		            	destination = directory,
		          nameconflict = "makeunique",
		       allowedExtensions = "png",
		    strict=true
		            );
		            """,
		    context )
		);

		mockUploads.clear();
		// Now test with a server defined disallow
		Stream.of( testFields ).forEach( field -> {
			Path fieldFile = Path.of( tmpDirectory, field + ".exe" );
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

		assertThrows( BoxRuntimeException.class, () -> runtime.executeSource(
		    """
		            result = FileUpload(
		            	filefield = filefield,
		            	destination = directory,
		          nameconflict = "makeunique",
		    strict=true
		            );
		            """,
		    context )
		);

		// Now test server-level disallowed with strict off
		// We have to change the file field because the error above will have deleted the disallowed file
		variables.put( Key.of( "filefield" ), testFields[ 1 ] );
		runtime.executeSource(
		    """
		            result = FileUpload(
		            	filefield = filefield,
		            	destination = directory,
		          nameconflict = "makeunique",
		       allowedExtensions = "exe",
		    strict=false
		            );
		            """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		fileInfo = variables.getAsStruct( result );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( Path.of( tmpDirectory, testFields[ 1 ] + ".exe" ).toAbsolutePath().toString() );
		assertTrue( fileInfo.getAsBoolean( KeyDictionary.fileWasSaved ) );

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
		    context );

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
