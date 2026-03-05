package ortus.boxlang.web.handlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

import ortus.boxlang.web.util.BaseWebTest;

// Integration tests for WebErrorHandler
// Tests the complete error page generation flow with placeholders and templates

@DisplayName("WebErrorHandler Integration Tests")
public class WebErrorHandlerIntegrationTests extends BaseWebTest {

    
    // ---- Full end-to-end error page generation tests ----
     
    @Nested
    @DisplayName("Complete Error Page Generation")
    class ErrorPageGenerationTests {

        @DisplayName("should generate complete error page with all sections")
        @Test
        public void testCompleteErrorPageGeneration() {
            Exception ex = new Exception("Test error with <special> characters");

            String result = WebErrorHandler.buildErrorPage(ex);

            assertThat(result).contains("Tag Context");
            assertThat(result).contains("Stack Trace");
            assertThat(result).contains("&lt;special&gt;");
            assertThat(result).doesNotContain("{{VERSION_INFO}}");
            assertThat(result).doesNotContain("{{ERROR_CONTENT}}");
        }

        @DisplayName("should handle nested exceptions with Caused By section")
        @Test
        public void testNestedExceptionsInErrorPage() {
            Exception rootCause = new Exception("Root cause error");
            Exception mainException = new Exception("Main error", rootCause);

            String result = WebErrorHandler.buildErrorPage(mainException);

            assertThat(result).contains("Main error");
            assertThat(result).contains("Caused By:");
        }
    }

    // ---- Tests for edge cases ----
     
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @DisplayName("should handle error message with mixed content")
        @Test
        public void testMixedContentInErrorMessage() {
            Exception ex = new Exception("Error <script> and special chars & quotes");

            String result = WebErrorHandler.buildErrorPage(ex);

            assertThat(result).contains("&lt;script&gt;");
            assertThat(result).isNotNull();
        }

        @DisplayName("should handle very long error messages")
        @Test
        public void testLongErrorMessage() {
            String longMessage = "Error: ".repeat(1000);
            Exception ex = new Exception(longMessage);

            String result = WebErrorHandler.buildErrorPage(ex);

            assertThat(result).isNotNull();
            assertThat(result.length()).isGreaterThan(100);
        }
    }


    // ---- Tests for placeholder replacement and template integration ----
    
    @Nested
    @DisplayName("Template Integration")
    class TemplateIntegrationTests {

        @DisplayName("should replace placeholders with actual content")
        @Test
        public void testPlaceholderReplacement() {
            Exception ex = new Exception("Test error message");

            String result = WebErrorHandler.buildErrorPage(ex);

            assertThat(result).doesNotContain("{{VERSION_INFO}}");
            assertThat(result).doesNotContain("{{ERROR_CONTENT}}");
            assertThat(result).contains("Test error message");
        }

        @DisplayName("should include version info in error page")
        @Test
        public void testVersionInfoIncluded() {
            Exception ex = new Exception("Test error");

            String result = WebErrorHandler.buildErrorPage(ex);

            assertThat(result).contains("<small>v");
        }
    }
}
