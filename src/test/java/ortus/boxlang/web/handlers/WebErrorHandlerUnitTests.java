package ortus.boxlang.web.handlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

import ortus.boxlang.web.util.BaseWebTest;

// Unit tests for WebErrorHandler helper methods
 
@DisplayName("WebErrorHandler Unit Tests")
public class WebErrorHandlerUnitTests extends BaseWebTest {

    
    // ----- Tests for HTML escaping functionality -----
    
    @Nested
    @DisplayName("HTML Escaping")
    class EscapeHTMLTests {

        @DisplayName("should escape < and > characters for XSS prevention")
        @Test
        public void testEscapeBasicHTMLCharacters() {
            String result = WebErrorHandler.escapeHTML("<script>alert('xss')</script>");
            
            assertThat(result).contains("&lt;script&gt;");
            assertThat(result).doesNotContain("<script>");
        }

        @DisplayName("should handle null input gracefully")
        @Test
        public void testEscapeNullInput() {
            String result = WebErrorHandler.escapeHTML(null);
            
            assertThat(result).isEmpty();
        }
    }

    // ---- Tests for whitespace preservation functionality ----
     
    @Nested
    @DisplayName("Whitespace Preservation")
    class PreserveWhitespaceTests {

        @DisplayName("should convert newlines to <br> tags")
        @Test
        public void testNewlineConversion() {
            String result = WebErrorHandler.preserveWhitespace("Line 1\nLine 2\nLine 3");
            
            assertThat(result).contains("<br>");
        }

        @DisplayName("should handle null input gracefully")
        @Test
        public void testPreserveWhitespaceNullInput() {
            String result = WebErrorHandler.preserveWhitespace(null);
            
            assertThat(result).isEmpty();
        }
    }

    
    // ---- Tests for build version info HTML generation ----

    @Nested
    @DisplayName("Version Info HTML")
    class BuildVersionInfoHTMLTests {

        @DisplayName("should return version info in debug mode")
        @Test
        public void testVersionInfoInDebugMode() {
            String result = WebErrorHandler.buildVersionInfoHTML(runtime);
            
            assertThat(result).startsWith("<small>v");
            assertThat(result).endsWith("</small>");
        }

        @DisplayName("should return non-empty version string")
        @Test
        public void testVersionInfoContent() {
            String result = WebErrorHandler.buildVersionInfoHTML(runtime);
            
            assertThat(result).isNotEmpty();
        }
    }

    
    // ---- Tests for build error content HTML generation ----
     
    @Nested
    @DisplayName("Error Content HTML Generation")
    class BuildErrorContentHTMLTests {

        @DisplayName("should include escaped error message")
        @Test
        public void testEscapedErrorMessage() {
            Exception ex = new Exception("Error <with> special chars");
            
            String result = WebErrorHandler.buildErrorContentHTML(ex, runtime);
            
            assertThat(result).contains("&lt;with&gt;");
            assertThat(result).isNotEmpty();
        }

        @DisplayName("should include nested exception handling")
        @Test
        public void testNestedExceptionHandling() {
            Exception cause = new Exception("Root cause error");
            Exception ex = new Exception("Outer exception", cause);
            
            String result = WebErrorHandler.buildErrorContentHTML(ex, runtime);
            
            assertThat(result).contains("Caused By:");
        }
    }



    
    // ---- Tests for placeholder replacement logic ----
     
    @Nested
    @DisplayName("Placeholder Replacement")
    class PlaceholderReplacementTests {

        @DisplayName("should replace {{VERSION_INFO}} placeholder")
        @Test
        public void testVersionInfoPlaceholderReplacement() {
            Exception ex = new Exception("Test error");
            
            String result = WebErrorHandler.buildErrorPage(ex);
            
            assertThat(result).doesNotContain("{{VERSION_INFO}}");
        }

        @DisplayName("should replace {{ERROR_CONTENT}} placeholder")
        @Test
        public void testErrorContentPlaceholderReplacement() {
            Exception ex = new Exception("Test error message");
            
            String result = WebErrorHandler.buildErrorPage(ex);
            
            assertThat(result).doesNotContain("{{ERROR_CONTENT}}");
            assertThat(result).contains("Test error message");
        }
    }


    // ---- Tests for security and XSS prevention ----
     
    @Nested
    @DisplayName("Security & XSS Prevention")
    class SecurityTests {

        @DisplayName("should prevent XSS via script tags")
        @Test
        public void testXSSScriptTagPrevention() {
            Exception ex = new Exception("<script>alert('xss')</script>");
            
            String result = WebErrorHandler.buildErrorPage(ex);
            
            assertThat(result).contains("&lt;script&gt;");
            assertThat(result).isNotEmpty();
        }

        @DisplayName("should prevent XSS via iframe injection")
        @Test
        public void testXSSIframePrevention() {
            Exception ex = new Exception("<iframe src='evil.com'></iframe>");
            
            String result = WebErrorHandler.buildErrorPage(ex);
            
            assertThat(result).contains("&lt;iframe");
            assertThat(result).isNotEmpty();
        }
    }
}
