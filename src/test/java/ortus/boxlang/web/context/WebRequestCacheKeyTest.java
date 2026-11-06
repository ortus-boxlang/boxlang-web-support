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
package ortus.boxlang.web.context;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.util.ResolvedFilePath;
import ortus.boxlang.web.util.BaseWebTest;
import ortus.boxlang.web.util.MockHTTPExchange;

/**
 * Covers {@link WebRequestBoxContext#getRequestCacheKey()}, which identifies a web request by the URL it
 * was addressed with rather than the template which ended up serving it.
 * <p>
 * This distinction is the whole point: several URLs can be rewritten onto one template, and they must not
 * share cache entries just because they share a file.
 */
public class WebRequestCacheKeyTest extends BaseWebTest {

	private static final String REQUEST_URL = "http://localhost:1443/products/detail.cfm";

	@BeforeEach
	public void setUpRequest() {
		// BaseWebTest.setupEach() has already built the context, so the URL can be swapped in here
		setRequestURL( REQUEST_URL );
		runtime.getCacheService().getDefaultCache().clearAll();
	}

	@DisplayName( "A web request is identified by the URL it was addressed with" )
	@Test
	public void testRequestCacheKeyIsTheRequestURL() {
		assertThat( context.getRequestCacheKey() ).isEqualTo( REQUEST_URL );
	}

	@DisplayName( "Two URLs served by the same template get separate cache entries" )
	@Test
	public void testCacheKeyIsScopedPerUrl() {
		String source = """
		                bx:cache action="cache" {
		                    writeOutput( "SHARED_BODY" );
		                }
		                     """;

		runtime.executeSource( source, context, BoxSourceType.BOXSCRIPT );
		var keys = runtime.getCacheService().getDefaultCache().getKeys();
		assertThat( keys.size() ).isEqualTo( 1 );
		String firstKey = keys.get( 0 ).toString();
		assertThat( firstKey ).startsWith( REQUEST_URL + "_pageid:" );

		// The same template, addressed by a different URL, must not reuse the entry
		runtime.getCacheService().getDefaultCache().clearAll();
		setRequestURL( "http://localhost:1443/blog/index.cfm" );
		runtime.executeSource( source, context, BoxSourceType.BOXSCRIPT );

		keys = runtime.getCacheService().getDefaultCache().getKeys();
		assertThat( keys.size() ).isEqualTo( 1 );
		String secondKey = keys.get( 0 ).toString();
		assertThat( secondKey ).startsWith( "http://localhost:1443/blog/index.cfm_pageid:" );
		// Same file, same position, different URL -- the URL is what keeps them apart
		assertThat( secondKey ).isNotEqualTo( firstKey );
	}

	@DisplayName( "It falls back to the template path once the exchange is discarded" )
	@Test
	public void testFallsBackToTemplatePathWithoutExchange() {
		// A shut down context has discarded its exchange, and threads must not touch it after that.
		// Asking for the key must degrade rather than throw.
		context.setHTTPExchange( null );

		ResolvedFilePath	baseTemplatePath	= context.getApplicationListener().getBaseTemplatePath();
		String				expected			= baseTemplatePath == null ? null : baseTemplatePath.absolutePath().toString();
		assertThat( context.getRequestCacheKey() ).isEqualTo( expected );
	}

	/**
	 * Point the mocked request at a given URL.
	 * <p>
	 * The exchange is held as an interface here, so the concrete mock has to be reached by cast.
	 *
	 * @param requestURL The URL this request should report it was addressed with
	 */
	private void setRequestURL( String requestURL ) {
		( ( MockHTTPExchange ) mockExchange ).setRequestURL( new StringBuffer( requestURL ) );
	}
}
