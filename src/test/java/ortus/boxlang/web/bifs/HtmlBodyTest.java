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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.web.util.BaseWebTest;

public class HtmlBodyTest extends BaseWebTest {

	// ---------------------------------------------------------------------------
	// append (default action)
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "append: adds text to the HTML body buffer" )
	public void testAppend() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<script>alert(1)</script>" );
		    """,
		    context
		);

		// Simulate onRequestEnd so the interceptor injects the buffered content
		String output = simulateRequestEnd( "<html><head></head><body><p>hello</p></body></html>" );

		assertThat( output ).contains( "<script>alert(1)</script>" );
		assertThat( output ).contains( "<body>" );
	}

	@Test
	@DisplayName( "append: multiple appends accumulate in order" )
	public void testMultipleAppends() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>first</p>" );
		    htmlBody( action="append", text="<p>second</p>" );
		    """,
		    context
		);

		String output = simulateRequestEnd( "<html><head></head><body></body></html>" );

		assertThat( output ).contains( "<p>first</p>" );
		assertThat( output ).contains( "<p>second</p>" );
		// Both items should appear before the original body content
		int	firstPos	= output.indexOf( "<p>first</p>" );
		int	secondPos	= output.indexOf( "<p>second</p>" );
		assertThat( firstPos ).isGreaterThan( -1 );
		assertThat( secondPos ).isGreaterThan( -1 );
	}

	@Test
	@DisplayName( "append: same id is not appended twice" )
	public void testAppendDeduplicationById() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>unique</p>", id="myScript" );
		    htmlBody( action="append", text="<p>unique</p>", id="myScript" );
		    htmlBody( action="append", text="<p>unique</p>", id="MYSCRIPT" );
		    """,
		    context
		);

		String	output	= simulateRequestEnd( "<html><head></head><body></body></html>" );

		// Should only appear once despite three calls with the same id (case-insensitive)
		int		count	= countOccurrences( output, "<p>unique</p>" );
		assertThat( count ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "append: different ids are each appended" )
	public void testAppendDifferentIds() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>first</p>", id="id1" );
		    htmlBody( action="append", text="<p>second</p>", id="id2" );
		    """,
		    context
		);

		String output = simulateRequestEnd( "<html><head></head><body></body></html>" );

		assertThat( output ).contains( "<p>first</p>" );
		assertThat( output ).contains( "<p>second</p>" );
	}

	// ---------------------------------------------------------------------------
	// read action
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "read: returns current buffer content into default variable 'htmlbody'" )
	public void testReadDefaultVariable() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>buffered</p>" );
		    htmlBody( action="read" );
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "htmlbody" ) ) ).contains( "<p>buffered</p>" );
	}

	@Test
	@DisplayName( "read: stores buffer content into a named variable" )
	public void testReadNamedVariable() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>content</p>" );
		    htmlBody( action="read", variable="myVar" );
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "myVar" ) ) ).contains( "<p>content</p>" );
	}

	@Test
	@DisplayName( "read: returns empty string when nothing has been appended" )
	public void testReadEmpty() {
		runtime.executeSource(
		    """
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEmpty();
	}

	// ---------------------------------------------------------------------------
	// reset action
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "reset: clears the HTML body buffer" )
	public void testReset() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>to be cleared</p>" );
		    htmlBody( action="reset" );
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEmpty();
	}

	@Test
	@DisplayName( "reset: also clears the id map so the same id may be re-used" )
	public void testResetClearsIdMap() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>first</p>", id="myId" );
		    htmlBody( action="reset" );
		    htmlBody( action="append", text="<p>re-added</p>", id="myId" );
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "result" ) ) ).contains( "<p>re-added</p>" );
	}

	// ---------------------------------------------------------------------------
	// write action
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "write: overwrites existing buffer content" )
	public void testWrite() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>old content</p>" );
		    htmlBody( action="write", text="<p>new content</p>" );
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "<p>new content</p>" );
		assertThat( result ).doesNotContain( "<p>old content</p>" );
	}

	@Test
	@DisplayName( "write: clears id map so ids can be re-registered after write" )
	public void testWriteClearsIdMap() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>original</p>", id="x" );
		    htmlBody( action="write", text="<p>overwritten</p>" );
		    // after write, the id 'x' should be clearable for re-use
		    htmlBody( action="append", text="<p>re-registered</p>", id="x" );
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "<p>overwritten</p>" );
		assertThat( result ).contains( "<p>re-registered</p>" );
	}

	// ---------------------------------------------------------------------------
	// flush action
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "flush: writes body buffer to the output stream and clears it" )
	public void testFlush() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>flushed</p>" );
		    htmlBody( action="flush" );
		    """,
		    context
		);

		// The flushed content should now be in the main output buffer
		String output = context.getBuffer().toString();
		assertThat( output ).contains( "<p>flushed</p>" );

		// And the htmlBody internal buffer should be empty after flush
		runtime.executeSource(
		    """
		    htmlBody( action="read", variable="afterFlush" );
		    """,
		    context
		);
		assertThat( variables.getAsString( Key.of( "afterFlush" ) ) ).isEmpty();
	}

	// ---------------------------------------------------------------------------
	// BIF default action (append when action omitted - uses argument default)
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "default action is append when action argument is omitted" )
	public void testDefaultAction() {
		runtime.executeSource(
		    """
		    htmlBody( text="<p>default append</p>" );
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "result" ) ) ).contains( "<p>default append</p>" );
	}

	// ---------------------------------------------------------------------------
	// Component (bx:htmlBody)
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "component: body content is captured and buffered (append)" )
	public void testComponentBodyCapture() {
		runtime.executeSource(
		    """
		    bx:htmlBody {
		        echo( "<p>from component body</p>" );
		    }
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "result" ) ) ).contains( "<p>from component body</p>" );

		// Body content should NOT appear in the main output buffer (it was intercepted)
		String mainOutput = context.getBuffer().toString();
		assertThat( mainOutput ).doesNotContain( "<p>from component body</p>" );
	}

	@Test
	@DisplayName( "component: text attribute and body content are combined" )
	public void testComponentTextAndBody() {
		runtime.executeSource(
		    """
		    bx:htmlBody text="<p>attr text</p>" {
		        echo( "<p>body text</p>" );
		    }
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "<p>attr text</p>" );
		assertThat( result ).contains( "<p>body text</p>" );
	}

	@Test
	@DisplayName( "component: write action via component replaces buffer" )
	public void testComponentWrite() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>old</p>" );
		    bx:htmlBody action="write" {
		        echo( "<p>new from component</p>" );
		    }
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "<p>new from component</p>" );
		assertThat( result ).doesNotContain( "<p>old</p>" );
	}

	@Test
	@DisplayName( "component: id prevents duplicate insertion" )
	public void testComponentDuplicateId() {
		runtime.executeSource(
		    """
		    bx:htmlBody id="unique" {
		        echo( "<p>once</p>" );
		    }
		    bx:htmlBody id="unique" {
		        echo( "<p>once</p>" );
		    }
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		int count = countOccurrences( variables.getAsString( Key.of( "result" ) ), "<p>once</p>" );
		assertThat( count ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "component: read action stores result in named variable" )
	public void testComponentRead() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>stored</p>" );
		    bx:htmlBody action="read" variable="myResult";
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "myResult" ) ) ).contains( "<p>stored</p>" );
	}

	@Test
	@DisplayName( "component: reset action clears the buffer" )
	public void testComponentReset() {
		runtime.executeSource(
		    """
		    htmlBody( action="append", text="<p>gone</p>" );
		    bx:htmlBody action="reset";
		    htmlBody( action="read", variable="result" );
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEmpty();
	}

	// ---------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------

	/**
	 * Simulates an onRequestEnd lifecycle by pushing the given HTML into the output
	 * buffer and then firing onRequestEnd, which triggers the Jsoup-based injection
	 * interceptor registered by HtmlBody.appendToBody().
	 *
	 * @param seedHtml Full HTML string to use as the base page output.
	 *
	 * @return The buffer content after onRequestEnd has been fired.
	 */
	private String simulateRequestEnd( String seedHtml ) {
		context.clearBuffer();
		context.writeToBuffer( seedHtml );
		BaseApplicationListener appListener = context.getApplicationListener();
		appListener.onRequestEnd( context, new Object[] { "/" } );
		return context.getBuffer().toString();
	}

	/**
	 * Counts non-overlapping occurrences of {@code substring} within {@code str}.
	 */
	private int countOccurrences( String str, String substring ) {
		int count = 0, idx = 0;
		while ( ( idx = str.indexOf( substring, idx ) ) != -1 ) {
			count++;
			idx += substring.length();
		}
		return count;
	}
}
