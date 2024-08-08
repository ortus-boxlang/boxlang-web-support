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
package ortus.boxlang.web.interceptors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.Component.ComponentBody;
import ortus.boxlang.runtime.components.cache.Cache;
import ortus.boxlang.runtime.components.cache.Cache.CacheAction;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;
import ortus.boxlang.web.util.KeyDictionary;

/**
 * Web request based interceptions
 */
public class WebRequest extends BaseInterceptor {

	/**
	 * The runtime instance
	 */
	protected BoxRuntime		runtime				= BoxRuntime.getInstance();

	/**
	 * The component service helper
	 */
	protected ComponentService	componentService	= BoxRuntime.getInstance().getComponentService();

	/**
	 * Writes content to the browser
	 *
	 * @param data The struct of data determining how the content should be written
	 *
	 * @data.context The context in which the interception is being made
	 *
	 * @data.content The content to be written to the browser
	 *
	 * @data.mimetype The MIME type of the content, defaults to text/html
	 *
	 * @data.fileName An optional filename of the file to be written. When provided the content is written as an attachment
	 *
	 * @data.reset A flag to determine if the buffer should be reset before writing the content
	 *
	 * @data.abort A flag to determine if the request should be aborted after writing the content
	 */
	@InterceptionPoint
	public void writeToBrowser( IStruct data ) {
		String				disposition	= "inline";
		RequestBoxContext	context		= ( RequestBoxContext ) data.get( Key.context );
		if ( context == null ) {
			throw new BoxRuntimeException( "A context is required in the intercept data in order to announce this interception" );
		}
		Object content = data.get( Key.content );
		if ( content == null ) {
			throw new BoxRuntimeException( "A request to write to the browser was announced, but no content to write was provided." );
		}
		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );
		IBoxHTTPExchange		exchange		= requestContext.getHTTPExchange();
		String					fileName		= data.getAsString( KeyDictionary.fileName );
		if ( fileName != null ) {
			disposition = "attachment";
		}
		String	mimeType	= StringCaster.cast( data.getOrDefault( Key.mimetype, "text/html" ) );
		Boolean	reset		= BooleanCaster.cast( data.getOrDefault( Key.reset, false ) );
		Boolean	abort		= BooleanCaster.cast( data.getOrDefault( Key.abort, false ) );

		if ( reset ) {
			context.clearBuffer();
		}

		byte[] contentBytes;
		if ( content instanceof byte[] barr ) {
			contentBytes = barr;
		} else {
			contentBytes = StringCaster.cast( content ).getBytes();
		}

		exchange.setResponseHeader( "content-type", mimeType );
		if ( disposition == "attachment" ) {
			exchange.setResponseHeader( "content-disposition", disposition + "; filename=" + fileName );
		}
		exchange.sendResponseBinary( contentBytes );

		if ( abort ) {
			throw new AbortException();
		} else {
			data.put( KeyDictionary.success, true );
		}

	}

	/**
	 * Listens for the file component actions around web uploaads
	 *
	 * @param data The data to be intercepted
	 */
	@InterceptionPoint
	public void onFileComponentAction( IStruct data ) {
		IStruct	attributes	= data.getAsStruct( Key.attributes );
		Key		action		= Key.of( attributes.getAsString( Key.action ) );

		if ( !action.equals( KeyDictionary.upload ) && !action.equals( KeyDictionary.uploadAll ) ) {
			return;
		}

		IBoxContext	context		= ( IBoxContext ) data.get( Key.context );
		Key			BIFMethod	= null;
		if ( action.equals( KeyDictionary.upload ) ) {
			BIFMethod = KeyDictionary.fileUpload;
		} else {
			BIFMethod = KeyDictionary.fileUploadAll;
		}

		attributes.put( BIF.__functionName, BIFMethod );

		data.put(
		    Key.response,
		    runtime.getFunctionService().getGlobalFunction( BIFMethod ).invoke( context, attributes, false, BIFMethod )
		);

	}

	/**
	 * Listens for the file component actions around web uploaads
	 *
	 * @param data The data to be intercepted
	 */
	@InterceptionPoint
	public void onComponentInvocation( IStruct data ) {
		Component component = ( Component ) data.get( Key.component );
		if ( component == null || data.get( Key.result ) != null ) {
			return;
		}
		if ( component instanceof Cache ) {
			IBoxContext			context		= ( IBoxContext ) data.get( Key.context );
			IStruct				attributes	= data.getAsStruct( Key.attributes );
			ComponentBody		body		= ( ComponentBody ) data.get( Key.body );
			Cache.CacheAction	cacheAction	= Cache.CacheAction.fromString( attributes.getAsString( Key.action ) );
			Double				timespan	= DoubleCaster.cast( attributes.get( Key.timespan ) );

			if ( context.getParentOfType( WebRequestBoxContext.class ) == null ) {
				throw new BoxRuntimeException(
				    String.format( "The specified cache action [%s] is is not valid in a non-web runtime", cacheAction.toString().toLowerCase() ) );
			} else {
				String cacheDirective = null;
				if ( cacheAction.equals( CacheAction.SERVERCACHE ) ) {
					cacheDirective = timespan == null ? "server" : "s-max-age=" + DoubleCaster.cast( timespan * Cache.secondsInDay ).intValue();
				} else if ( cacheAction.equals( CacheAction.CLIENTCACHE ) ) {
					cacheDirective = timespan == null ? "private" : "max-age=" + DoubleCaster.cast( timespan * Cache.secondsInDay ).intValue();
				}
				if ( cacheDirective != null ) {
					componentService.getComponent( Key.header ).invoke(
					    context,
					    Struct.of(
					        Key._NAME, "Cache-Control",
					        Key.value, cacheDirective
					    ),
					    body
					);
					data.put( Key.result, cacheDirective );
				}
			}
		}

	}

}
