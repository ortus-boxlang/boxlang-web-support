package ortus.boxlang.web.exchange;

import static com.google.common.truth.Truth.assertThat;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

public class WhitespaceManagingPrintWriterTest {

	@Test
	public void preservesContentWhitespaceAndRemovesOnlyEmptyRepeatedLines() {
		StringWriter output = new StringWriter();
		try ( WhitespaceManagingPrintWriter writer = new WhitespaceManagingPrintWriter( output, true ) ) {
			writer.write(
			    "<select\nname=\"test\"\n\n\n\tonchange=\"\n\tif (!this.disabled)\n\t{\n\t\tSetBaseRt();\n\n\n\t}\n\">\n\n\nbrad\n" );
			writer.flush();
		}

		String result = output.toString();
		assertThat( result ).isEqualTo( "<select\nname=\"test\"\nonchange=\"\nif (!this.disabled)\n{\nSetBaseRt();\n}\n\">\nbrad" );
	}

	@Test
	public void preservesWhitespaceInsidePreformattedTags() {
		StringWriter output = new StringWriter();
		try ( WhitespaceManagingPrintWriter writer = new WhitespaceManagingPrintWriter( output, true ) ) {
			writer.write( "<script>\n\n\tvar value = 1;\n\n\n</script>\n" );
			writer.flush();
		}

		assertThat( output.toString() ).isEqualTo( "<script>\n\n\tvar value = 1;\n\n\n</script>" );
	}

	@Test
	public void disabledWhitespaceManagementWritesInputUnchanged() {
		String			input	= "  first\n\n\tsecond\r\n";
		StringWriter	output	= new StringWriter();
		try ( WhitespaceManagingPrintWriter writer = new WhitespaceManagingPrintWriter( output, false ) ) {
			writer.write( input );
		}

		assertThat( output.toString() ).isEqualTo( input );
	}

	@Test
	public void trimsLeadingAndTrailingWhitespaceFromSimpleResponse() {
		StringWriter output = new StringWriter();
		try ( WhitespaceManagingPrintWriter writer = new WhitespaceManagingPrintWriter( output, true ) ) {
			writer.write( "\ntest\n" );
		}

		assertThat( output.toString() ).isEqualTo( "test" );
	}

	@Test
	public void preservesContentWhitespaceInProvidedWebPage() {
		StringWriter output = new StringWriter();
		try ( WhitespaceManagingPrintWriter writer = new WhitespaceManagingPrintWriter( output, true ) ) {
			writer.write( """
			              <html>
			              	<head>\t\t\t\t\t\t\t\t
			              	</head>  \t\t\t\t\t
			              	<body>
			              <cfoutput>
			              <cfset foo = \"brad\" >
			              \n
			              \n
			              \n
			              	<select
			              name=\"test\"
			              	id=\"test\"
			              	\n
			              	onchange=\"
			              	if (!this.disabled)
			              	{
			              		var sdf = document.getElementById('sdf');
			              	\n
			              		var sdf = this.form.sdf.value;
			              		if (sdf)
			              			sdf.innerHTML = sdf;
			              	}
			              	\">
			              \n
			              				#foo#
			              </cfoutput>
			              </body>
			              </html>
			              """ );
			writer.flush();
		}

		String result = output.toString();
		for ( String line : result.split( "\\R" ) ) {
			if ( !line.isEmpty() ) {
				assertThat( line ).doesNotMatch( "^[ \\t].*" );
			}
		}
		assertThat( result ).contains( "<select\nname=\"test\"\nid=\"test\"\nonchange=\"\nif (!this.disabled)\n{\nvar sdf = document.getElementById('sdf');" );
		assertThat( result ).contains( "if (sdf)\nsdf.innerHTML = sdf;\n}\n\">\n#foo#\n</cfoutput>" );
	}

}