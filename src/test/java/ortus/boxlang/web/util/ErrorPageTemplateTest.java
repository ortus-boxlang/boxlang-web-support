package ortus.boxlang.web.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for the ErrorPageTemplate class
 */
public class ErrorPageTemplateTest {

	@Test
	@DisplayName("Can create template instance")
	public void testCreateTemplate() {
		ErrorPageTemplate template = ErrorPageTemplate.create();
		assertNotNull(template);
	}

	@Test
	@DisplayName("Can set and render variables")
	public void testSetAndRenderVariables() {
		ErrorPageTemplate template = ErrorPageTemplate.create();
		template.setVariable("VERSION_INFO", "<small>v1.0.0</small>");
		template.setVariable("ERROR_CONTENT", "<div>Test error</div>");
		template.setVariable("DEBUG_CONTENT", "<div>Debug info</div>");
		
		String result = template.render();
		
		assertNotNull(result);
		assertTrue(result.contains("<small>v1.0.0</small>"));
		assertTrue(result.contains("<div>Test error</div>"));
		assertTrue(result.contains("<div>Debug info</div>"));
		assertTrue(result.contains("BoxLang Error"));
	}

	@Test
	@DisplayName("Template removes unreplaced placeholders")
	public void testUnreplacedPlaceholders() {
		ErrorPageTemplate template = ErrorPageTemplate.create();
		template.setVariable("ERROR_CONTENT", "<div>Test error</div>");
		// Don't set VERSION_INFO or DEBUG_CONTENT
		
		String result = template.render();
		
		assertNotNull(result);
		assertTrue(result.contains("<div>Test error</div>"));
		// Should not contain unreplaced placeholders
		assertFalse(result.contains("{{VERSION_INFO}}"));
		assertFalse(result.contains("{{DEBUG_CONTENT}}"));
	}

	@Test
	@DisplayName("Template handles null values")
	public void testNullValues() {
		ErrorPageTemplate template = ErrorPageTemplate.create();
		template.setVariable("ERROR_CONTENT", null);
		template.setVariable("DEBUG_CONTENT", "");
		
		String result = template.render();
		
		assertNotNull(result);
		// Should not crash and should produce valid HTML
		assertTrue(result.contains("<!DOCTYPE html>"));
	}

	@Test
	@DisplayName("Method chaining works")
	public void testMethodChaining() {
		String result = ErrorPageTemplate.create()
			.setVariable("ERROR_CONTENT", "<div>Chained error</div>")
			.setVariable("DEBUG_CONTENT", "<div>Chained debug</div>")
			.render();
		
		assertNotNull(result);
		assertTrue(result.contains("<div>Chained error</div>"));
		assertTrue(result.contains("<div>Chained debug</div>"));
	}

	@Test
	@DisplayName("Fallback template works when main template is missing")
	public void testFallbackTemplate() {
		ErrorPageTemplate template = ErrorPageTemplate.create("/non-existent-template.html");
		template.setVariable("ERROR_CONTENT", "<div>Fallback test</div>");
		
		String result = template.render();
		
		assertNotNull(result);
		assertTrue(result.contains("<div>Fallback test</div>"));
		assertTrue(result.contains("BoxLang Error"));
		// Should be using fallback template
		assertTrue(result.contains("class=\"error\""));
	}
}