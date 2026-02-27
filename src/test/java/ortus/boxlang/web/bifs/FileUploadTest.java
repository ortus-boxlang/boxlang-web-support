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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.web.util.KeyDictionary;

@Execution( ExecutionMode.SAME_THREAD )
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
		if ( FileSystemUtil.exists( Path.of( TEST_WEBROOT, "myRelativeFile.jpg" ).toString() ) ) {
			FileSystemUtil.deleteFile( Path.of( TEST_WEBROOT, "myRelativeFile.jpg" ).toString() );
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
			mockUploads.add( new ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload( Key.of( field ), fieldFile,
			    fieldFile.getFileName().toString() ) );
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
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).endsWith( ".jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( testFields[ 1 ] + ".jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
		assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.get( KeyDictionary.clientFileName ) ).isEqualTo( testFields[ 1 ] );
		assertThat( fileInfo.get( KeyDictionary.serverFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "." );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.mimeType ) ).isEqualTo( "image/jpeg" );
		assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image" );
		assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
		assertThat( fileInfo.get( KeyDictionary.fileSize ) ).isEqualTo( fileInfo.get( KeyDictionary.oldFileSize ) );

	}

	@DisplayName( "It tests the BIF FileUpload will accept a full path as the destination" )
	@Test
	public void testBifFileUploadWithFileDestination() {
		variables.put( Key.of( "filefield" ), testFields[ 1 ] );
		variables.put( Key.directory, Path.of( tmpDirectory, "myfile.jpg" ).toAbsolutePath().toString() );
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
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isEqualTo( "myfile.jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( testFields[ 1 ] + ".jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
		assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.get( KeyDictionary.clientFileName ) ).isEqualTo( "file2" );
		assertThat( fileInfo.get( KeyDictionary.serverFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "." );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.mimeType ) ).isEqualTo( "image/jpeg" );
		assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image" );
		assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
		assertThat( fileInfo.get( KeyDictionary.oldFileSize ) ).isNull();

	}

	@DisplayName( "It tests the BIF FileUpload will accept a full path as the destination and will allow the FORM. prefix in compat" )
	@Test
	public void testBifFileUploadWithFileDestinationCompat() {
		try {
			FileUpload.allowPrefixedFileFields = true;
			variables.put( Key.of( "filefield" ), "FORM." + testFields[ 1 ] );
			variables.put( Key.directory, Path.of( tmpDirectory, "myfile-foo.jpg" ).toAbsolutePath().toString() );
			runtime.executeSource(
			    """
			      result = FileUpload(
			      	filefield = filefield,
			      	destination = directory,
			    nameconflict = "overwrite"
			      );
			      """,
			    context );

			assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

			IStruct fileInfo = variables.getAsStruct( result );

			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isInstanceOf( String.class );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isNotEmpty();
			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "/" );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "\\" );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isEqualTo( "myfile-foo.jpg" );
			assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
			    .isEqualTo( testFields[ 1 ] + ".jpg" );
			assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
			    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
			assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
			assertThat( fileInfo.get( KeyDictionary.clientFileName ) ).isEqualTo( "file2" );
			assertThat( fileInfo.get( KeyDictionary.serverFileExt ) ).isEqualTo( "jpg" );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "." );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "/" );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "\\" );
			assertThat( fileInfo.getAsString( KeyDictionary.mimeType ) ).isEqualTo( "image/jpeg" );
			assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image" );
			assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
			assertThat( fileInfo.get( KeyDictionary.oldFileSize ) ).isNull();
		} finally {
			FileUpload.allowPrefixedFileFields = false;
		}

	}

	@DisplayName( "It tests the BIF FileUpload will create paths in the temp directory if it is part of an absolute path" )
	@Test
	public void testsAbsoluteTempDirectory() {
		variables.put( Key.of( "filefield" ), testFields[ 1 ] );
		variables.put( Key.directory, Path.of( FileSystemUtil.getTempDirectory(), "foo/bar/myfile.jpg" ).toAbsolutePath().toString() );
		runtime.executeSource(
		    """
		      result = FileUpload(
		      	filefield = filefield,
		      	destination = directory,
		    nameconflict = "overwrite"
		      );
		      """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct fileInfo = variables.getAsStruct( result );

		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isInstanceOf( String.class );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isNotEmpty();
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isEqualTo( "myfile.jpg" );
		// We use contains here because of possible symlinks on Unix
		assertThat( fileInfo.getAsString( KeyDictionary.serverDirectory ) )
		    .contains( Path.of( FileSystemUtil.getTempDirectory(), "foo/bar" ).toAbsolutePath().toString() );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( testFields[ 1 ] + ".jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
		assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.get( KeyDictionary.clientFileName ) ).isEqualTo( "file2" );
		assertThat( fileInfo.get( KeyDictionary.serverFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "." );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.mimeType ) ).isEqualTo( "image/jpeg" );
		assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image" );
		assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );

	}

	@DisplayName( "It tests the BIF FileUpload will create paths in the temp directory without an absolute file path" )
	@Test
	public void testTempDirectoryPathCreation() {
		variables.put( Key.of( "filefield" ), testFields[ 1 ] );
		variables.put( Key.directory, FileSystemUtil.getTempDirectory() + "//foo/baz" );
		runtime.executeSource(
		    """
		        result = FileUpload(
		    directory,
		    filefield,
		    "*",
		    "makeunique"
		        );
		        """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct fileInfo = variables.getAsStruct( result );

		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isInstanceOf( String.class );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isNotEmpty();
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverDirectory ) )
		    .contains( Path.of( FileSystemUtil.getTempDirectory(), "foo/baz" ).toAbsolutePath().toString() );
		assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.get( KeyDictionary.serverFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "." );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.mimeType ) ).isEqualTo( "image/jpeg" );
		assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image" );
		assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
	}

	@DisplayName( "It tests the BIF FileUpload with a relative path containing a filename" )
	@Test
	public void testsTemplateRelativePath() {
		variables.put( Key.of( "filefield" ), testFields[ 1 ] );
		variables.put( Key.directory, "./myRelativeFile.jpg" );
		runtime.executeSource(
		    """
		      result = FileUpload(
		      	filefield = filefield,
		      	destination = directory,
		    nameconflict = "overwrite"
		      );
		      """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct fileInfo = variables.getAsStruct( result );

		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isInstanceOf( String.class );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isNotEmpty();
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).isEqualTo( "myRelativeFile.jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverDirectory ) )
		    .isEqualTo( TEST_WEBROOT );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( testFields[ 1 ] + ".jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
		assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.get( KeyDictionary.clientFileName ) ).isEqualTo( "file2" );
		assertThat( fileInfo.get( KeyDictionary.serverFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "." );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.mimeType ) ).isEqualTo( "image/jpeg" );
		assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image" );
		assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
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
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).endsWith( ".jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( testFields[ 0 ] + ".jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
		assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.get( KeyDictionary.clientFileName ) ).isEqualTo( testFields[ 0 ] );
		assertThat( fileInfo.get( KeyDictionary.serverFileExt ) ).isEqualTo( "jpg" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "." );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "/" );
		assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "\\" );
		assertThat( fileInfo.getAsString( KeyDictionary.mimeType ) ).isEqualTo( "image/jpeg" );
		assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image" );
		assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
		assertThat( fileInfo.get( KeyDictionary.fileSize ) ).isEqualTo( fileInfo.get( KeyDictionary.oldFileSize ) );

	}

	@DisplayName( "It tests the BIF FileUpload with explicitly allowed extensions" )
	@Test
	public void testBifFileSecurity() {
		variables.put( Key.of( "filefield" ), testFields[ 0 ] );
		variables.put( Key.directory, Path.of( tmpDirectory ).toAbsolutePath().toString() );

		// Test with strict mode off
		//@formatter:off
		runtime.executeSource(
		    """
			result = FileUpload(
				filefield = filefield,
				accept = "image/png",
				destination = directory,
				nameconflict = "makeunique",
				allowedExtensions = "png",
				strict=false
			);
			""",
		    context );
		//@formatter:on
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct fileInfo = variables.getAsStruct( result );

		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( testFields[ 0 ] + ".jpg" );
		assertFalse( fileInfo.getAsBoolean( KeyDictionary.fileWasSaved ) );

		// Test with strict mode on
		// We are mocking the servlet behavior here by appending `.tmp` to uploaded file to ensure our security checks are looking at the original file name
		assertThrows( BoxRuntimeException.class, () -> runtime.executeSource(
		    """
		            result = FileUpload(
		            	filefield = filefield,
		            	destination = directory,
		          nameconflict = "makeunique",
		       allowedExtensions = "png,tmp",
		    strict=true
		            );
		            """,
		    context ) );

		mockUploads.clear();
		// Now test with a server defined disallow
		Stream.of( testFields ).forEach( field -> {
			Path fieldFile = Path.of( tmpDirectory, field + ".exe" + ".tmp" );
			try {
				Files.copy( Path.of( testUpload ), fieldFile, StandardCopyOption.REPLACE_EXISTING );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
			mockUploads.add( new ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload( Key.of( field ), fieldFile,
			    fieldFile.getFileName().toString().replace( ".tmp", "" ) ) );
		} );

		ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload[] uploadsArray = mockUploads
		    .toArray( new ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload[ 0 ] );

		when( mockExchange.getUploadData() ).thenReturn( uploadsArray );

		// TODO: Commented for now until we figure out the file extensions config
		// assertThrows( BoxRuntimeException.class, () -> runtime.executeSource(
		// """
		// result = FileUpload(
		// filefield = filefield,
		// destination = directory,
		// nameconflict = "makeunique",
		// strict=true
		// );
		// """,
		// context )
		// );

		// Now test server-level disallowed with strict off
		// We have to change the file field because the error above will have deleted
		// the disallowed file
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
		    .isEqualTo( testFields[ 1 ] + ".exe" );
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
			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "/" );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).doesNotContain( "\\" );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFile ) ).endsWith( ".jpg" );
			assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
			    .isNotEqualTo( fileInfo.getAsString( KeyDictionary.serverFile ) );
			assertThat( fileInfo.get( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
			assertThat( fileInfo.getAsString( KeyDictionary.clientFileName ) ).doesNotContain( "." );
			assertThat( fileInfo.getAsString( KeyDictionary.clientFileName ) ).isAnyOf( testFields[ 0 ], testFields[ 1 ], testFields[ 2 ] );
			assertThat( fileInfo.get( KeyDictionary.serverFileExt ) ).isEqualTo( "jpg" );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "." );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "/" );
			assertThat( fileInfo.getAsString( KeyDictionary.serverFileName ) ).doesNotContain( "\\" );
			assertThat( fileInfo.get( KeyDictionary.contentType ) ).isEqualTo( "image" );
			assertThat( fileInfo.get( KeyDictionary.contentSubType ) ).isEqualTo( "jpeg" );
			assertThat( fileInfo.get( KeyDictionary.fileSize ) ).isEqualTo( fileInfo.get( KeyDictionary.oldFileSize ) );
		} );

	}

	@DisplayName( "It tests the BIF FileUpload with blocked extensions" )
	@Test
	public void testBifFileUploadBlockedExtensions() {
		// Reset uploads to ensure clean state
		setupUploadsForTest();

		variables.put( Key.of( "filefield" ), testFields[ 0 ] );
		variables.put( Key.directory, Path.of( tmpDirectory ).toAbsolutePath().toString() );

		// Make sure our upload works with explicitly allowed extensions first
		runtime.executeSource(
		    """
		              result = FileUpload(
		              filefield = filefield,
		              	destination = directory,
		            nameconflict = "makeunique",
		    accept="image/jpg",
		         allowedExtensions = ".jpg",
		      strict=true
		              );
		              """,
		    context );

		IStruct fileInfo = variables.getAsStruct( result );

		assertThat( fileInfo.getAsString( KeyDictionary.clientFileExt ) ).isEqualTo( "jpg" );
		assertTrue( fileInfo.getAsBoolean( KeyDictionary.fileWasSaved ) );

		variables.put( Key.of( "filefield" ), testFields[ 0 ] );
		variables.put( Key.directory, Path.of( tmpDirectory ).toAbsolutePath().toString() );

		// Reset uploads again for next test
		setupUploadsForTest();

		// Test blocking jpg files with strict mode off
		runtime.executeSource(
		    """
		            result = FileUpload(
		            	filefield = filefield,
		            	destination = directory,
		          nameconflict = "overwrite",
		       blockedExtensions = "jpg",
		    strict=false
		            );
		            """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		fileInfo = variables.getAsStruct( result );

		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( testFields[ 0 ] + ".jpg" );
		assertFalse( fileInfo.getAsBoolean( KeyDictionary.fileWasSaved ) );

		// Reset uploads again for next test
		setupUploadsForTest();

		// Test blocking jpg files with strict mode on - should throw exception
		assertThrows( BoxRuntimeException.class, () -> runtime.executeSource(
		    """
		            result = FileUpload(
		            	filefield = filefield,
		            	destination = directory,
		          nameconflict = "makeunique",
		       blockedExtensions = "jpg",
		    strict=true
		            );
		            """,
		    context ) );

		// Reset uploads again for next test
		setupUploadsForTest();

		// Test blocking other extensions (not jpg) - should allow jpg upload
		runtime.executeSource(
		    """
		            result = FileUpload(
		            	filefield = filefield,
		            	destination = directory,
		          nameconflict = "makeunique",
		       blockedExtensions = "exe,pdf,doc",
		    strict=false
		            );
		            """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		fileInfo = variables.getAsStruct( result );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( testFields[ 0 ] + ".jpg" );
		assertTrue( fileInfo.getAsBoolean( KeyDictionary.fileWasSaved ) );

		// Reset uploads again for next test
		setupUploadsForTest();

		// Test combination of allowedExtensions and blockedExtensions
		// Allow jpg and png, but block jpg - should result in no file saved
		runtime.executeSource(
		    """
		            result = FileUpload(
		            	filefield = filefield,
		            	destination = directory,
		          nameconflict = "makeunique",
		       allowedExtensions = "jpg,png",
		       blockedExtensions = "jpg",
		    strict=false
		            );
		            """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		fileInfo = variables.getAsStruct( result );
		assertThat( fileInfo.getAsString( KeyDictionary.clientFile ) )
		    .isEqualTo( testFields[ 0 ] + ".jpg" );
		assertFalse( fileInfo.getAsBoolean( KeyDictionary.fileWasSaved ) );
	}

	/**
	 * Helper method to setup clean uploads for each test scenario
	 */
	private void setupUploadsForTest() {
		try {
			mockUploads.clear();

			Stream.of( testFields ).forEach( field -> {
				Path fieldFile = Path.of( tmpDirectory, field + ".jpg" );
				try {
					Files.copy( Path.of( testUpload ), fieldFile, StandardCopyOption.REPLACE_EXISTING );
				} catch ( IOException e ) {
					throw new BoxIOException( e );
				}
				mockUploads.add( new ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload( Key.of( field ), fieldFile,
				    fieldFile.getFileName().toString() ) );
			} );

			ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload[] uploadsArray = mockUploads
			    .toArray( new ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload[ 0 ] );

			when( mockExchange.getUploadData() ).thenReturn( uploadsArray );
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to setup uploads for test", e );
		}
	}

}
