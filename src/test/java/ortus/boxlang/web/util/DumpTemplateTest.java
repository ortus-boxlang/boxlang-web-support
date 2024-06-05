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

import static com.google.common.truth.Truth.assertThat;

import java.io.PrintStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class DumpTemplateTest extends BaseWebTest {

	@DisplayName( "Test it" )
	@Test
	void testIt() {
		ByteArrayOutputStream	out			= new ByteArrayOutputStream();
		PrintStream				printStream	= new PrintStream( out );

		System.out.println( " print " + printStream );

		webContext.setOut( printStream );

		FileSystemUtil.write( ".dumptest.html",
		    StringCaster.cast( runtime.executeSource( StringCaster.cast( FileSystemUtil.read( "src/test/resources/www/index.cfm" ) ), webContext,
		        BoxSourceType.CFTEMPLATE ) ) );
		assertThat( true ).isTrue();
	}

}