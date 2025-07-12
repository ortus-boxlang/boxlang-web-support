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
package ortus.boxlang.web.util;

import ortus.boxlang.runtime.scopes.Key;

public class KeyDictionary {

	public static final Key	bx_template_path	= Key.of( "bx_template_path" );
	public static final Key	html				= Key.of( "html" );
	public static final Key	htmlHead			= Key.of( "htmlHead" );
	public static final Key	htmlParse			= Key.of( "htmlParse" );
	public static final Key	htmlFooter			= Key.of( "htmlFooter" );
	public static final Key	onRequestEnd		= Key.of( "onRequestEnd" );
	public static final Key	fileName			= Key.of( "fileName" );
	public static final Key	disposition			= Key.of( "disposition" );
	public static final Key	safeList			= Key.of( "safeList" );
	public static final Key	success				= Key.of( "success" );

	// File Upload keys
	public static final Key	upload				= Key.of( "upload" );
	public static final Key	uploadAll			= Key.of( "uploadAll" );
	public static final Key	fileUpload			= Key.of( "fileUpload" );
	public static final Key	fileUploadAll		= Key.of( "fileUploadAll" );
	public static final Key	allowedExtensions	= Key.of( "allowedExtensions" );
	public static final Key	attemptedServerFile	= Key.of( "attemptedServerFile" );
	public static final Key	clientDirectory		= Key.of( "clientDirectory" );
	public static final Key	clientFile			= Key.of( "clientFile" );
	public static final Key	clientFileExt		= Key.of( "clientFileExt" );
	public static final Key	clientFileName		= Key.of( "clientFileName" );
	public static final Key	contentSubType		= Key.of( "contentSubType" );
	public static final Key	contentType			= Key.of( "contentType" );
	public static final Key	dateLastAccessed	= Key.of( "dateLastAccessed" );
	public static final Key	fileExisted			= Key.of( "fileExisted" );
	public static final Key	fileSize			= Key.of( "fileSize" );
	public static final Key	fileWasAppended		= Key.of( "fileWasAppended" );
	public static final Key	fileWasOverwritten	= Key.of( "fileWasOverwritten" );
	public static final Key	fileWasRenamed		= Key.of( "fileWasRenamed" );
	public static final Key	fileWasSaved		= Key.of( "fileWasSaved" );
	public static final Key	oldFileSize			= Key.of( "oldFileSize" );
	public static final Key	serverDirectory		= Key.of( "serverDirectory" );
	public static final Key	serverFile			= Key.of( "serverFile" );
	public static final Key	serverFileExt		= Key.of( "serverFileExt" );
	public static final Key	serverFileName		= Key.of( "serverFileName" );
	public static final Key	timeCreated			= Key.of( "timeCreated" );
	public static final Key	timeLastModified	= Key.of( "timeLastModified" );

	// Session Cookie Settings
	public static final Key	sessionCookie		= Key.of( "sessionCookie" );
	public static final Key	secure				= Key.of( "secure" );
	public static final Key	httpOnly			= Key.of( "httponly" );
	public static final Key	sameSite			= Key.of( "sameSite" );
	public static final Key	sameSiteMode		= Key.of( "sameSiteMode" );
	public static final Key	disableUpdate		= Key.of( "disableUpdate" );
	public static final Key	encodevalue			= Key.of( "encodevalue" );

}
