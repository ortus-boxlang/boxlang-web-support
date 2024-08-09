package ortus.boxlang.web.components;

import static com.google.common.truth.Truth.assertThat;
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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.web.util.KeyDictionary;

public class FileTest extends ortus.boxlang.web.util.BaseWebTest {

	ArrayList<ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload>	mockUploads		= new ArrayList<ortus.boxlang.web.exchange.IBoxHTTPExchange.FileUpload>();;
	public final String													TEST_WEBROOT	= Path.of( "src/test/resources/webroot" ).toAbsolutePath().toString();
	// Test Constants
	Key																	result			= new Key( "result" );
	String																testURLImage	= "https://ortus-public.s3.amazonaws.com/logos/ortus-medium.jpg";
	String																tmpDirectory	= "src/test/resources/tmp/FileComponentTests";
	String																testUpload		= tmpDirectory + "/test.jpg";

	String[]															testFields		= new String[] { "file1", "file2", "file3" };

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

		mockUploads.clear();

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

	@DisplayName( "It tests the file component with upload" )
	@Test
	public void testComponentUpload() {
		variables.put( Key.of( "filefield" ), testFields[ 0 ] );
		variables.put( Key.directory, Path.of( tmpDirectory ).toAbsolutePath().toString() );
		// @formatter:off
		runtime.executeSource(
		    """
		    <cffile action="upload" filefield="#filefield#" destination="#directory#" nameconflict="makeunique" variable="result"/>
		         """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:off

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

	@DisplayName( "It tests the file component with uploadAll" )
	@Test
	public void testComponentUploadAll() {
		mockUploads.stream().forEach( upload -> {
			assertTrue( Files.exists( upload.tmpPath() ) );
		} );
		variables.put( Key.directory, Path.of( tmpDirectory ).toAbsolutePath().toString() );
		// @formatter:off
		runtime.executeSource(
		    """
		    <cffile action="uploadAll" destination="#directory#" nameconflict="makeunique" variable="result"/>
		         """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:off

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
