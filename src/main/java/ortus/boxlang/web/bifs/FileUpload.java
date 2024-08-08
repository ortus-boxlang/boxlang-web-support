
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import java.util.stream.Stream;

import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.util.KeyDictionary;

@BoxBIF
@BoxBIF( alias = "FileUploadAll" )

public class FileUpload extends BIF {

	/**
	 * Constructor
	 */
	public FileUpload() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.destination ),
		    new Argument( false, "string", Key.accept ),
		    new Argument( false, "string", Key.nameconflict, "error", Set.of( Validator.valueOneOf( "error", "skip", "overwrite", "makeunique" ) ) ),
		    new Argument( false, "string", KeyDictionary.allowedExtensions ),
		    new Argument( false, "string", Key.filefield ),
		    new Argument( false, "string", Key.strict )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key						action			= Key.of( arguments.getAsString( Key.action ) );
		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );
		String					variable		= arguments.getAsString( Key.variable );
		Key						bifMethodKey	= arguments.getAsKey( BIF.__functionName );

		if ( variable == null && arguments.containsKey( Key.result ) ) {
			variable = arguments.getAsString( Key.result );
		}

		if ( requestContext == null ) {
			throw new BoxRuntimeException(
			    String.format( "The specified file action [%s] is is not valid in a non-web runtime", action.getName() ) );
		}

		IBoxHTTPExchange				exchange	= requestContext.getHTTPExchange();

		IBoxHTTPExchange.FileUpload[]	uploads		= exchange.getUploadData();

		if ( uploads == null ) {
			throw new BoxRuntimeException( "No file uploads were found in the request" );
		} else if ( bifMethodKey.equals( KeyDictionary.fileUpload ) ) {
			Key							fieldKey	= Key.of( arguments.getAsString( Key.filefield ) );
			IBoxHTTPExchange.FileUpload	upload		= null;
			if ( fieldKey == null ) {
				upload = uploads[ 0 ];
			} else {
				upload = Stream.of( uploads ).filter( u -> u.formFieldName().equals( fieldKey ) ).findFirst().orElse( null );
				if ( upload == null ) {
					throw new BoxRuntimeException( "The specified file field [" + fieldKey.getName() + "] was not found in the upload data" );
				}
			}

			return uploadFile( upload, arguments, context );

		} else {
			return Stream.of( uploads ).map( upload -> uploadFile( upload, arguments, context ) ).collect( BLCollector.toArray() );
		}
	}

	private IStruct uploadFile( IBoxHTTPExchange.FileUpload upload, IStruct arguments, IBoxContext context ) {
		String	destination		= arguments.getAsString( Key.destination );
		Path	destinationPath	= null;
		Boolean	createPath		= false;

		if ( !Path.of( destination ).isAbsolute() ) {
			destinationPath	= Path.of( FileSystemUtil.getTempDirectory(), destination );
			createPath		= true;
		} else {
			destinationPath = FileSystemUtil.expandPath( context, destination ).absolutePath();
		}

		if ( !Files.isDirectory( destinationPath ) ) {
			throw new BoxRuntimeException( "The specified destination path [" + destination + "] is not a directory" );
		}

		if ( !Files.exists( destinationPath ) && createPath ) {
			try {
				Files.createDirectories( destinationPath );
			} catch ( IOException e ) {
				throw new BoxIOException( "The specified destination path [" + destination + "] could not be created", e );
			}
		}
		if ( !Files.exists( destinationPath ) ) {
			throw new BoxRuntimeException( "The specified destination path [" + destination + "] does not exist" );
		}

		String	fileName		= upload.originalFileName();
		String	extension		= Parser.getFileExtension( fileName ).get();
		Path	filePath		= destinationPath.resolve( fileName );
		String	nameConflict	= arguments.getAsString( Key.nameconflict );

		IStruct	uploadRecord	= newUploadRecord();
		uploadRecord.put( KeyDictionary.clientDirectory, destinationPath.toString() );
		uploadRecord.put( KeyDictionary.clientFile, filePath.toString() );
		uploadRecord.put( KeyDictionary.clientFileExt, extension );
		uploadRecord.put( KeyDictionary.clientFileName, fileName );
		uploadRecord.put( KeyDictionary.attemptedServerFile, filePath.toString() );
		try {
			uploadRecord.put( KeyDictionary.fileSize, Files.size( upload.tmpPath() ) );
		} catch ( IOException e ) {
			throw new BoxIOException( "The size of the uploaded file [" + fileName + "] could not be determined", e );
		}

		DateTime operationDate = new DateTime();

		uploadRecord.put( KeyDictionary.timeCreated, operationDate );

		if ( Files.exists( filePath ) ) {
			uploadRecord.put( KeyDictionary.fileExisted, true );
			try {
				uploadRecord.put( KeyDictionary.timeCreated, new DateTime( ( ( FileTime ) Files.getAttribute( filePath, "creationTime" ) ).toInstant() ) );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			try {
				uploadRecord.put( KeyDictionary.oldFileSize, Files.size( filePath ) );
			} catch ( IOException e ) {
				throw new BoxIOException( "The size of the original file [" + fileName + "] could not be determined", e );
			}
			switch ( nameConflict ) {
				case "error" :
					throw new BoxRuntimeException( "The file [" + fileName + "] already exists in the destination path [" + destination + "]" );
				case "skip" :
					return uploadRecord;
				case "overwrite" :
					try {
						Files.delete( filePath );
						uploadRecord.put( KeyDictionary.fileWasOverwritten, true );
					} catch ( IOException e ) {
						throw new BoxIOException( "The file [" + fileName + "] could not be deleted from the destination path [" + destination + "]", e );
					}
					break;
				case "makeunique" :
					String baseName = fileName.substring( 0, fileName.length() - extension.length() - 1 );
					fileName = baseName + "_" + System.currentTimeMillis() + "." + extension;
					filePath = destinationPath.resolve( fileName );
					uploadRecord.put( KeyDictionary.fileWasRenamed, true );
					break;
			}
		}

		try {
			Files.move( upload.tmpPath(), filePath );
			String mimeType = Files.probeContentType( filePath );
			if ( mimeType == null ) {
				mimeType = "application/octet-stream";
			}
			uploadRecord.put( KeyDictionary.contentType, mimeType );
			uploadRecord.put( KeyDictionary.contentSubType, ListUtil.asList( mimeType, "/" ).get( 1 ) );
			uploadRecord.put( KeyDictionary.timeLastModified, operationDate );
			uploadRecord.put( KeyDictionary.dateLastAccessed, operationDate );
			uploadRecord.put( KeyDictionary.serverFile, filePath.toString() );
			uploadRecord.put( KeyDictionary.serverFileExt, extension );
			uploadRecord.put( KeyDictionary.serverFileName, fileName );
			uploadRecord.put( KeyDictionary.serverDirectory, filePath.getParent().toString() );
			uploadRecord.put( KeyDictionary.fileSize, Files.size( filePath ) );
			uploadRecord.put( KeyDictionary.fileWasSaved, true );
		} catch ( IOException e ) {
			throw new BoxIOException( "The uploaded file [" + fileName + "] could not be saved to the destination path [" + destination + "]", e );
		}

		return uploadRecord;
	}

	/**
	 * Returns an empty struct containing the keys used to track file uploads
	 *
	 * @return
	 */
	private IStruct newUploadRecord() {
		return Struct.of(
		    KeyDictionary.attemptedServerFile, null, // Initial name ColdFusion used when attempting to save a file
		    KeyDictionary.clientDirectory, null, // Directory location of the file uploaded from the client's system
		    KeyDictionary.clientFile, null, // Name of the file uploaded from the client's system
		    KeyDictionary.clientFileExt, null, // Extension of the uploaded file on the client system (without a period)
		    KeyDictionary.clientFileName, null, // Name of the uploaded file on the client system (without an extension)
		    KeyDictionary.contentSubType, null, // MIME content subtype of the saved file
		    KeyDictionary.contentType, null, // MIME content type of the saved file
		    KeyDictionary.dateLastAccessed, null, // Date and time the uploaded file was last accessed
		    KeyDictionary.fileExisted, false, // Whether the file existed with the same path (yes or no)
		    KeyDictionary.fileSize, null, // Size of the uploaded file in bytes
		    KeyDictionary.fileWasAppended, false, // Whether ColdFusion appended uploaded file to a file (yes or no)
		    KeyDictionary.fileWasOverwritten, false, // Whether ColdFusion overwrote a file (yes or no)
		    KeyDictionary.fileWasRenamed, false, // Whether uploaded file renamed to avoid a name conflict (yes or no)
		    KeyDictionary.fileWasSaved, false, // Whether ColdFusion saves a file (yes or no)
		    KeyDictionary.oldFileSize, null, // Size of a file that was overwritten in the file upload operation
		    KeyDictionary.serverDirectory, null, // Directory of the file saved on the server
		    KeyDictionary.serverFile, null, // Filename of the file saved on the server
		    KeyDictionary.serverFileExt, null, // Extension of the uploaded file on the server (without a period)
		    KeyDictionary.serverFileName, null, // Name of the uploaded file on the server (without an extension)
		    KeyDictionary.timeCreated, null, // Time the uploaded file was created
		    KeyDictionary.timeLastModified, null // Date and time of the last modification to the uploaded file
		);
	}

}
