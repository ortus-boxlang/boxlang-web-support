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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple template processor for error pages.
 * Provides a lightweight templating system that separates HTML presentation
 * from Java logic while maintaining minimal dependencies.
 */
public class ErrorPageTemplate {
	
	private static final String DEFAULT_TEMPLATE_PATH = "/templates/error-page.html";
	private String templateContent;
	private Map<String, String> variables = new HashMap<>();
	
	/**
	 * Constructor that loads the default error page template
	 */
	public ErrorPageTemplate() {
		loadTemplate(DEFAULT_TEMPLATE_PATH);
	}
	
	/**
	 * Constructor that loads a custom template
	 * 
	 * @param templatePath The path to the template file in resources
	 */
	public ErrorPageTemplate(String templatePath) {
		loadTemplate(templatePath);
	}
	
	/**
	 * Load template content from resources
	 * 
	 * @param templatePath The path to the template file
	 */
	private void loadTemplate(String templatePath) {
		try (InputStream inputStream = getClass().getResourceAsStream(templatePath)) {
			if (inputStream == null) {
				// Fallback to a minimal HTML structure if template is not found
				templateContent = createFallbackTemplate();
				return;
			}
			templateContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			// Use fallback template on any loading error
			templateContent = createFallbackTemplate();
		}
	}
	
	/**
	 * Create a minimal fallback template when the main template can't be loaded
	 * 
	 * @return A basic HTML error page template
	 */
	private String createFallbackTemplate() {
		return """
			<!DOCTYPE html>
			<html>
			<head>
				<title>BoxLang Error</title>
				<style>
					body { font-family: Arial, sans-serif; margin: 40px; }
					.error { background: #f8f8f8; border-left: 3px solid #d33; padding: 20px; }
					h1 { color: #d33; }
				</style>
			</head>
			<body>
				<div class="error">
					<h1>BoxLang Error {{VERSION_INFO}}</h1>
					{{ERROR_CONTENT}}
					{{DEBUG_CONTENT}}
				</div>
			</body>
			</html>
		""";
	}
	
	/**
	 * Set a template variable
	 * 
	 * @param key The variable name (without {{ }} brackets)
	 * @param value The value to substitute
	 * @return This template instance for method chaining
	 */
	public ErrorPageTemplate setVariable(String key, String value) {
		variables.put(key, value != null ? value : "");
		return this;
	}
	
	/**
	 * Set multiple template variables
	 * 
	 * @param vars A map of variable names to values
	 * @return This template instance for method chaining
	 */
	public ErrorPageTemplate setVariables(Map<String, String> vars) {
		if (vars != null) {
			variables.putAll(vars);
		}
		return this;
	}
	
	/**
	 * Render the template with all set variables
	 * 
	 * @return The rendered HTML content
	 */
	public String render() {
		String result = templateContent;
		
		// Replace all template variables
		for (Map.Entry<String, String> entry : variables.entrySet()) {
			String placeholder = "{{" + entry.getKey() + "}}";
			result = result.replace(placeholder, entry.getValue());
		}
		
		// Clean up any remaining unreplaced placeholders
		result = result.replaceAll("\\{\\{[^}]+\\}\\}", "");
		
		return result;
	}
	
	/**
	 * Create a new template instance with the default template
	 * 
	 * @return A new ErrorPageTemplate instance
	 */
	public static ErrorPageTemplate create() {
		return new ErrorPageTemplate();
	}
	
	/**
	 * Create a new template instance with a custom template
	 * 
	 * @param templatePath The path to the template file in resources
	 * @return A new ErrorPageTemplate instance
	 */
	public static ErrorPageTemplate create(String templatePath) {
		return new ErrorPageTemplate(templatePath);
	}
}