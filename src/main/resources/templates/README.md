# BoxLang Error Page Templates

This directory contains HTML templates for BoxLang error pages.

## Template System

The error page template system allows for easy customization and maintenance of error page HTML without modifying Java code.

### Default Template

**File:** `error-page.html`

This template is used by `WebErrorHandler` to generate error pages for web requests.

### Template Variables

The template uses simple `{{VARIABLE}}` placeholders that are replaced at runtime:

- `{{VERSION_INFO}}` - BoxLang version information (shown only in debug mode)
- `{{ERROR_CONTENT}}` - Main error messages, exception details, and error hierarchy
- `{{DEBUG_CONTENT}}` - Tag context and stack trace information (shown only in debug mode)

### Usage

Templates are loaded automatically by the `ErrorPageTemplate` class. No additional configuration is required.

```java
// Example usage in Java code
ErrorPageTemplate template = ErrorPageTemplate.create();
template.setVariable("VERSION_INFO", "<small>v1.0.0</small>");
template.setVariable("ERROR_CONTENT", "<div>Error message</div>");
template.setVariable("DEBUG_CONTENT", "<div>Debug info</div>");
String html = template.render();
```

### Customization

To customize the error page:

1. Edit `error-page.html` directly
2. Modify CSS styles within the `<style>` section
3. Update HTML structure as needed
4. Add new template variables by modifying `WebErrorHandler.java`

### Fallback Template

If the main template cannot be loaded, the system automatically falls back to a minimal HTML structure to ensure error pages are always displayed.

### Features

- **Responsive Design**: Error page adapts to different screen sizes
- **Dark Mode Support**: Automatic dark mode detection via CSS media queries
- **Interactive Elements**: Collapsible code sections and debug panels
- **Accessibility**: Proper ARIA labels and semantic HTML
- **Modern Styling**: CSS custom properties and flexbox layout