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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
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
@BoxBIF( alias = "FileUploadAll", description = "Processes all file uploads from the request into the specified destination directory." )
public class FileUpload extends BIF {

	/**
	 * Constructor
	 */
	public FileUpload() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.destination ),
		    new Argument( false, "string", Key.filefield ),
		    new Argument( false, "string", Key.accept ),
		    new Argument( false, "string", Key.nameconflict, "error", Set.of( Validator.valueOneOf( "error", "skip", "overwrite", "makeunique" ) ) ),
		    new Argument( false, "boolean", Key.strict, true ),
		    new Argument( false, "any", KeyDictionary.allowedExtensions ),
		    new Argument( false, "any", KeyDictionary.blockedExtensions ),
		    // Still to implement
		    new Argument( false, "string", Key.attributes ),
		    new Argument( false, "string", Key.mode )
		};
	}

	/**
	 * Processes file uploads from the request into the specified destination directory.
	 *
	 * <ul>
	 * <li>If the destination is not an absolute path, it will be resolved relative to the system's temporary directory.</li>
	 * <li>If the file field is not specified, the first file upload in the request will be used.</li>
	 * <li>If the upload is not permitted by the server, application, or request file security settings, it will be rejected.</li>
	 * <li>If a file with the same name already exists in the destination directory, the action specified by the `nameconflict` argument will be taken:
	 * <ul>
	 * <li>`error`: The upload will be rejected if a file with the same name exists.</li>
	 * <li>`skip`: The existing file will not be overwritten.</li>
	 * <li>`overwrite`: The existing file will be replaced with the uploaded file.</li>
	 * <li>`makeunique`: The uploaded file will be renamed to make it unique (e.g., by appending a timestamp).</li>
	 * </ul>
	 * </li>
	 * <li>If the upload violates any security policies and `strict` is true, an exception will be thrown. If `strict` is false, the upload will be
	 * skipped
	 * if it violates security policies.</li>
	 * <li>The uploaded file's metadata will be returned in an IStruct, which includes:
	 * <ul>
	 * <li>`clientDirectory`: The directory location of the file uploaded from the client's system.</li>
	 * <li>`clientFile`: The name of the file uploaded from the client's system.</li>
	 * <li>`clientFileExt`: The extension of the uploaded file on the client system (without a period).</li>
	 * <li>`clientFileName`: The name of the uploaded file on the client system (without an extension).</li>
	 * <li>`contentType`: The MIME content type of the saved file.</li>
	 * <li>`contentSubType`: The MIME content subtype of the saved file.</li>
	 * <li>`dateLastAccessed`: The date and time the uploaded file was last accessed.</li>
	 * <li>`fileExisted`: Whether the file existed with the same path (yes or no).</li>
	 * <li>`fileSize`: The size of the uploaded file in bytes.</li>
	 * <li>`fileWasAppended`: Whether ColdFusion appended the uploaded file to a file (yes or no).</li>
	 * <li>`fileWasOverwritten`: Whether ColdFusion overwrote a file (yes or no).</li>
	 * <li>`fileWasRenamed`: Whether the uploaded file was renamed to avoid a name conflict (yes or no).</li>
	 * <li>`fileWasSaved`: Whether ColdFusion saved a file (yes or no).</li>
	 * <li>`oldFileSize`: The size of a file that was overwritten in the file upload operation.</li>
	 * <li>`serverDirectory`: The directory of the file saved on the server.</li>
	 * <li>`serverFile`: The filename of the file saved on the server.</li>
	 * <li>`serverFileExt`: The extension of the uploaded file on the server (without a period).</li>
	 * <li>`serverFileName`: The name of the uploaded file on the server (without an extension).</li>
	 * <li>`timeCreated`: The time the uploaded file was created.</li>
	 * <li>`timeLastModified`: The date and time of the last modification to the uploaded file.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.destination The destination directory for the uploaded files. If the path is not absolute, it will be resolved relative to the system's
	 *                       temporary directory.
	 *
	 * @argument.filefield The name of the file field to process. If not specified, the first file upload in the request will be used.
	 *
	 * @argument.accept The accepted MIME types for the uploaded files. This can be a comma-separated list of MIME types or a single string or an array.
	 *
	 * @argument.nameconflict The action to take when a file with the same name already exists in the destination directory. The default is "error", which
	 *                        means that the upload will be rejected if a file with the same name exists. Other options are "skip" (do not overwrite),
	 *                        "overwrite" (replace the existing file), and "makeunique" (rename the uploaded file to make it unique).
	 *
	 * @argument.strict Whether to strictly enforce the system specified upload security settings. The default is true, which means that the upload will
	 *                  be rejected if it violates any security policies.
	 *
	 * @argument.allowedExtensions The allowed file extensions for the uploaded files. This can be a comma-separated list of extensions or a single string
	 *                             or an array.
	 *
	 * @argument.blockedExtensions The blocked file extensions for the uploaded files. This can be a comma-separated list of extensions or a single string
	 *                             or an array.
	 *
	 * @throws BoxRuntimeException if no file uploads are found in the request or if the specified file field is not found.
	 * @throws BoxIOException      if there is an error creating directories or moving files.
	 * @throws IOException         if there is an error accessing the file system.
	 *
	 * @return An IStruct containing information about the uploaded file(s).
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		WebRequestBoxContext			requestContext	= context.getParentOfType( WebRequestBoxContext.class );
		Key								bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		IBoxHTTPExchange				exchange		= requestContext.getHTTPExchange();
		IBoxHTTPExchange.FileUpload[]	uploads			= exchange.getUploadData();

		// Validate the arguments
		if ( uploads == null ) {
			throw new BoxRuntimeException( "No file uploads were found in the request" );
		}

		// Single File Upload
		if ( bifMethodKey.equals( KeyDictionary.fileUpload ) ) {
			String field = arguments.getAsString( Key.filefield );
			// If no field is specified, use the first upload's form field name
			if ( field == null ) {
				field = uploads[ 0 ].formFieldName().getName();
			}
			Key							fieldKey	= Key.of( field );
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
		}

		// If we reach here then we are processing multiple file uploads
		return Stream.of( uploads ).map( upload -> uploadFile( upload, arguments, context ) ).collect( BLCollector.toArray() );
	}

	/**
	 * Processes a single file upload and saves it to the specified destination directory.
	 *
	 * @param upload    The file upload to process.
	 * @param arguments The arguments for the BIF.
	 * @param context   The context in which the BIF is being invoked.
	 *
	 * @return An IStruct containing information about the uploaded file.
	 */
	private IStruct uploadFile( IBoxHTTPExchange.FileUpload upload, IStruct arguments, IBoxContext context ) {
		String	destination				= arguments.getAsString( Key.destination );
		Path	destinationPath			= null;
		Boolean	createPath				= false;
		String	tempDirectory			= FileSystemUtil.getTempDirectory();
		boolean	isTemplateRelativePath	= destination.startsWith( "." );

		if ( !Path.of( destination ).isAbsolute() && !isTemplateRelativePath ) {
			// If the destination is not an absolute path, resolve it relative to the system's temporary directory
			destinationPath	= Path.of( tempDirectory, destination ).normalize();
			createPath		= true;
		} else {
			destinationPath	= FileSystemUtil.expandPath( context, destination ).absolutePath();
			// If our temp directory is passed in as the absolute path, we create the necessary directories
			createPath		= destinationPath.toString().startsWith( tempDirectory );
		}

		String fileName = upload.originalFileName();

		if ( !Files.isDirectory( destinationPath ) ) {
			if ( destinationPath.getFileName().toString().contains( "." ) ) {
				fileName		= destinationPath.getFileName().toString();
				destinationPath	= destinationPath.getParent();
			} else {
				throw new BoxRuntimeException( "The specified destination path [" + destination + "] is not a directory or a path to a file" );
			}
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

		String	extension		= Parser.getFileExtension( fileName ).get();
		Path	filePath		= destinationPath.resolve( fileName );
		String	nameConflict	= arguments.getAsString( Key.nameconflict ).toLowerCase();

		// Prepare the upload record
		IStruct	uploadRecord	= newUploadRecord();
		uploadRecord.put( KeyDictionary.clientDirectory, destinationPath.toString() );
		uploadRecord.put( KeyDictionary.clientFile, upload.originalFileName() );
		uploadRecord.put( KeyDictionary.clientFileExt, Parser.getFileExtension( upload.originalFileName() ).get() );
		uploadRecord.put( KeyDictionary.clientFileName, FilenameUtils.getBaseName( upload.originalFileName() ) );
		uploadRecord.put( KeyDictionary.attemptedServerFile, filePath.toString() );

		try {
			uploadRecord.put( KeyDictionary.fileSize, Files.size( upload.tmpPath() ) );
		} catch ( IOException e ) {
			throw new BoxIOException( "The size of the uploaded file [" + fileName + "] could not be determined", e );
		}

		Boolean uploadPermitted = processUploadSecurity( upload, arguments, context );

		if ( !uploadPermitted ) {
			try {
				// Delete the file since it's been marked as unsafe
				Files.delete( upload.tmpPath() );
			} catch ( IOException e ) {
				throw new BoxIOException( "The uploaded file [" + upload.tmpPath().toString() + "] was marked as unsafe but could not be deleted.", e );
			}

			// If strict mode is enabled, we throw an exception to notify that the settings are
			if ( arguments.getAsBoolean( Key.strict ) ) {
				throw new BoxRuntimeException(
				    "The the upload of file [" + fileName + "] is not permitted by the server, application or request file security settings" );
			} else {
				return uploadRecord;
			}
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

			// Handle name conflicts based on the specified action
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
			uploadRecord.put( KeyDictionary.serverFile, fileName );
			uploadRecord.put( KeyDictionary.serverFileExt, extension );
			uploadRecord.put( KeyDictionary.serverFileName, FilenameUtils.getBaseName( fileName ) );
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

	/**
	 * Determines if the upload is permitted based on the server and request level security settings
	 *
	 * @param upload
	 * @param arguments
	 * @param context
	 *
	 * @return
	 */
	@SuppressWarnings( "unchecked" )
	public boolean processUploadSecurity( IBoxHTTPExchange.FileUpload upload, IStruct arguments, IBoxContext context ) {
		// System and request level whitelist and blacklist settings
		IStruct	requestSettings				= context.getParentOfType( RequestBoxContext.class ).getSettings();
		String	uploadMimeType				= FileSystemUtil.getMimeType( upload.tmpPath().toString() );
		String	uploadExtension				= Parser.getFileExtension( upload.tmpPath().getFileName().toString() ).get().toLowerCase();
		String	allowedExtensions			= arguments.getAsString( KeyDictionary.allowedExtensions );
		String	blockedExtensions			= arguments.getAsString( KeyDictionary.blockedExtensions );
		String	allowedMimeTypes			= arguments.getAsString( Key.accept );
		Boolean	strict						= arguments.getAsBoolean( Key.strict );
		Boolean	hasApplicationPermission	= true;
		Boolean	hasRequestPermission		= true;

		if ( !StringUtils.isEmpty( allowedExtensions ) ) {
			hasRequestPermission = ListUtil.asList( allowedExtensions, ListUtil.DEFAULT_DELIMITER ).stream()
			    .map( StringCaster::cast )
			    .map( ext -> ext.startsWith( "." ) ? ext.substring( 1 ) : ext )
			    .anyMatch( ext -> ext.equals( "*" ) || ext.equalsIgnoreCase( uploadExtension ) );
		}

		if ( !StringUtils.isEmpty( blockedExtensions ) ) {
			hasRequestPermission = hasRequestPermission && ListUtil.asList( blockedExtensions, ListUtil.DEFAULT_DELIMITER ).stream()
			    .map( StringCaster::cast )
			    .map( ext -> ext.startsWith( "." ) ? ext.substring( 1 ) : ext )
			    .noneMatch( ext -> ext.equalsIgnoreCase( uploadExtension ) );
		}

		if ( !StringUtils.isEmpty( allowedMimeTypes ) && StringUtils.isEmpty( allowedExtensions ) && StringUtils.isEmpty( blockedExtensions ) ) {
			hasRequestPermission = ListUtil.asList( allowedMimeTypes, ListUtil.DEFAULT_DELIMITER ).stream()
			    .map( StringCaster::cast )
			    .anyMatch( mimeType -> {
				    if ( mimeType.equals( "*" ) || mimeType.equals( "*/*" ) ) {
					    return true;
				    } else if ( mimeType.endsWith( "/*" ) ) {
					    String typePrefix = mimeType.substring( 0, mimeType.length() - 2 ).toLowerCase();
					    return uploadMimeType.toLowerCase().startsWith( typePrefix + "/" );
				    } else {
					    return mimeType.equalsIgnoreCase( uploadMimeType );
				    }
			    } );
		}

		hasApplicationPermission = FileSystemUtil.isExtensionAllowed( context, uploadExtension );

		// If strict mode, both application and request permissions must be true
		if ( strict ) {
			return hasApplicationPermission && hasRequestPermission;
		}

		// If allowedExtensions, blockedExtensions, or allowedMimeTypes are specified, only check request permission
		if ( allowedExtensions != null || blockedExtensions != null || allowedMimeTypes != null ) {
			return hasRequestPermission;
		}

		// Otherwise, only check application permission
		return hasApplicationPermission;
	}

}
