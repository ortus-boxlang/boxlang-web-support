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
package ortus.boxlang.web.exchange;

import java.io.PrintWriter;
import java.io.Writer;

public class WhitespaceManagingPrintWriter extends PrintWriter {

	private boolean			enable						= true;  // Flag to toggle behavior on/off
	private boolean			preserveWhitespace			= false;  // Flag to control whitespace compression
	private StringBuilder	tagBuffer					= new StringBuilder();
	private boolean			inTag						= false;  // Flag to indicate whether we are inside a tag
	private boolean			closeTag					= false;  // Flag to track if it's a closing tag (starts with '/')
	private boolean			firstTagChar				= true;  // Flag to track if we are processing the first tag character
	private boolean			previousCharWasWhitespace	= false;  // Flag to track if the previous character was whitespace

	// Constructor with an additional parameter to toggle whitespace compression
	public WhitespaceManagingPrintWriter( Writer out, boolean enable ) {
		super( out );
		this.enable = enable;
		// System.out.println( "Whitespace compression is " + ( enable ? "enabled" : "disabled" ) );
	}

	@Override
	public void write( int c ) {
		processChar( ( char ) c );
	}

	@Override
	public void write( char[] buf, int off, int len ) {
		for ( int i = off; i < off + len; i++ ) {
			processChar( buf[ i ] );
		}
	}

	@Override
	public void write( String s, int off, int len ) {
		for ( int i = off; i < off + len; i++ ) {
			processChar( s.charAt( i ) );
		}
	}

	private void processChar( char ch ) {
		// System.out.println( "Processing character: " + ( ch + "" ) + "(" + ( int ) ch + ") enable is: " + enable + " inTag is: " + inTag
		// + " preserveWhitespace is: " + preserveWhitespace
		// + " closeTag is: " + closeTag + " firstTagChar is: " + firstTagChar + " previousCharWasWhitespace is: " + previousCharWasWhitespace );
		// If whitespace compression is off, just write the character as-is
		if ( !enable ) {
			super.write( ch );  // Directly write the character to the underlying writer
			return;
		}

		boolean isWS = Character.isWhitespace( ch );

		// If we're inside a tag, handle it
		if ( inTag ) {
			// If it's a closing tag, handle it
			if ( firstTagChar && ch == '/' ) {
				closeTag = true;
				super.write( ch );  // Write the '/' for the closing tag
				previousCharWasWhitespace = isWS;
				return;
			}

			// Handle the tag name (only lowercase the characters immediately)
			if ( firstTagChar && !isWS && ch != '>' ) {
				// If the first character isn't 'p', 'c', 't', or 's', abort the tag
				if ( ! ( ch == 'p' || ch == 'c' || ch == 't' || ch == 's' ) ) {
					inTag = false;  // Stop tracking the tag
					super.write( ch );
					previousCharWasWhitespace = isWS;
					return;
				}
			}

			if ( !isWS && ch != '>' ) {
				tagBuffer.append( Character.toLowerCase( ch ) );  // Lowercase the tag name immediately
			}
			firstTagChar = false;

			// Stop tracking when we hit '>' or a space
			if ( isWS || ch == '>' ) {
				// Immediately handle tags based on the tag name
				String tagName = tagBuffer.toString();
				// System.out.println( "Tag name: " + tagName );

				// Handle opening <pre>, <code>, <textarea>, or <script> tags
				if ( "pre".equals( tagName ) || "code".equals( tagName ) || "textarea".equals( tagName ) || "script".equals( tagName ) ) {
					preserveWhitespace = !closeTag;
				}

				inTag = false;  // Stop processing this tag
			}

			super.write( ch );  // Write the regular character to the underlying writer
		} else {

			// We're starting a tag
			if ( ch == '<' ) {
				inTag			= true;
				closeTag		= false;
				firstTagChar	= true;
				tagBuffer.setLength( 0 );  // Clear the tag buffer for new tag
				super.write( ch );  // Write the '<' character directly
				previousCharWasWhitespace = isWS;
				return;
			}

			// Handle characters outside of tags (regular content)
			if ( !preserveWhitespace ) {
				// Collapse extra whitespace into one space
				if ( isWS && previousCharWasWhitespace ) {
					previousCharWasWhitespace = isWS;
					return;  // Skip extra whitespace
				}
			}

			super.write( ch );  // Write the regular character to the underlying writer
		}
		previousCharWasWhitespace = isWS;
	}

	@Override
	public void flush() {
		super.flush();  // Only flush the underlying buffer when explicitly called
	}

	@Override
	public void close() {
		flush();  // Ensure flushing before closing
		super.close();
	}

	/**
	 * Get the current state of the whitespace compression flag
	 */
	public boolean isWhitespaceCompressionEnabled() {
		return enable;
	}

	/**
	 * Set the whitespace compression flag
	 */
	public void setWhitespaceCompressionEnabled( boolean enable ) {
		this.enable = enable;
	}
}
