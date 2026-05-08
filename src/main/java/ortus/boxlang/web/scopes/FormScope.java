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
package ortus.boxlang.web.scopes;

import java.util.Arrays;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.web.context.WebRequestBoxContext;

/**
 * Form scope implementation in BoxLang
 */
public class FormScope extends BaseScope {

	public static Key				fieldNames	= Key.of( "fieldNames" );
	private WebRequestBoxContext	context;

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key			name		= Key.of( "form" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public FormScope( WebRequestBoxContext context ) {
		super( FormScope.name );
		this.context = context;
		context.getHTTPExchange().getRequestFormMap().forEach( ( key, value ) -> {
			// Convention for foo[]=brad foo[]=luis which creates array instead of comma delimited string.
			if ( key.endsWith( "[]" ) ) {
				// leave empty elements when making an array. (CF compat)
				this.put( Key.of( key.substring( 0, key.length() - 2 ) ), new Array( value ) );
			} else {
				// Remove empty elements when making a list (CF compat)
				this.put( Key.of( key ), Arrays.stream( value ).filter( s -> s != null && !s.isEmpty() ).collect( Collectors.joining( "," ) ) );
			}
		} );

		// Only for POST requests
		if ( context.getHTTPExchange().getRequestMethod().equalsIgnoreCase( "POST" ) ) {
			// add form.fieldNames from our internal keys
			this.put( fieldNames, Arrays.stream( this.keySet().toArray() ).map( Object::toString ).collect( Collectors.joining( "," ) ) );
		}

	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This method is purely for compat with ACF, to match an undocumented method that people would use to get the client file name of uplaoded files
	 * before
	 * processing them with fileUpload() or the file component. ONLY use this method for compat with legacy ACF code.
	 * I can probably shoehorn this into the compat module if I need as some sort of member method on the `FormScope`, but for now just sticking it here.
	 * 
	 * @return an array of FormPart representing the uploaded files
	 */
	public FormPart[] getPartsArray() {
		var uploadData = context.getHTTPExchange().getUploadData();
		if ( uploadData == null || uploadData.length == 0 ) {
			return new FormPart[ 0 ];
		}
		FormPart[] parts = new FormPart[ uploadData.length ];
		for ( int i = 0; i < uploadData.length; i++ ) {
			parts[ i ] = new FormPart( uploadData[ i ].originalFileName() );
		}
		return parts;
	}

	/**
	 * I don't really know what methods exist on this legacy compat object, but I know these minimally are needed for legacy code.
	 */
	public static record FormPart( String fileName ) {

		/**
		 * We're only representing file parts, so always true
		 * 
		 * @return true
		 */
		public boolean isFile() {
			return true;
		}

		/**
		 * We're only representing file parts, so always "file"
		 * 
		 * @return "file"
		 */
		public String getName() {
			return "file";
		}

		/**
		 * Get the original file name of the uploaded file
		 * 
		 * @return the original file name
		 */
		public String getFileName() {
			return fileName;
		}
	}
}
