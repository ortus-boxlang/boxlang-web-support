
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

import static org.junit.Assert.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class FileCopyTest extends ortus.boxlang.web.util.BaseWebTest {

	static String sourceFile = "src/test/resources/tmp/fileCopyTest/source.txt";

	@DisplayName( "It tests the BIF FileCopy security in the default web context" )
	@Test
	public void testBifSecurity() {
		variables.put( Key.of( "source" ), Path.of( sourceFile ).toAbsolutePath().toString() );

		assertThrows(
		    BoxRuntimeException.class,
		    () -> runtime.executeSource(
		        """
		        fileCopy( source, "blah.exe" );
		        """,
		        context )
		);
	}

}
