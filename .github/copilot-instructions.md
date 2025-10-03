# BoxLang Web Support Package

## Architecture Overview

This is a **BoxLang web runtime extension** that provides HTTP/web capabilities through a plugin architecture. It extends BoxLang's core runtime with web-specific components, BIFs (Built-in Functions), and contexts.

### Core Components

- **`WebRequestExecutor`**: Main entry point that handles HTTP request lifecycle, manages contexts, and delegates to application listeners
- **`IBoxHTTPExchange`**: Abstract interface for HTTP request/response operations, allows different servlet implementations
- **`WebRequestBoxContext`**: Extends BoxLang's RequestBoxContext with web-specific scopes (URL, Form, CGI, Cookie, Request)
- **Web Components**: BoxLang components with `@BoxComponent` annotation (Content, Header, Cookie, etc.)
- **Web BIFs**: Functions with `@BoxBIF` annotation for HTTP operations (GetHTTPRequestData, Location, FileUpload, etc.)
- **Interceptors**: Event-driven hooks for web request processing

### Plugin Discovery Pattern

Components and BIFs are auto-discovered via Java SPI (Service Provider Interface):
- **Service files**: Generated in `build/resources/main/META-INF/services/`
- **Gradle plugin**: `com.github.harbby.gradle.serviceloader` handles auto-generation
- **Registration**: Runtime automatically loads classes listed in service files

## Development Patterns

### Creating Web Components
```java
@BoxComponent(allowsBody = true)
public class MyComponent extends Component {
    public MyComponent() {
        super();
        declaredAttributes = new Attribute[] {
            new Attribute(Key.myAttr, "string", Set.of(Validator.NON_EMPTY))
        };
    }
    
    public BodyResult _invoke(IBoxContext context, IStruct attributes, 
                              ComponentBody body, IStruct executionState) {
        WebRequestBoxContext requestContext = context.getParentOfType(WebRequestBoxContext.class);
        IBoxHTTPExchange exchange = requestContext.getHTTPExchange();
        // Component logic here
        return DEFAULT_RETURN;
    }
}
```

### Creating Web BIFs
```java
@BoxBIF
public class MyBIF extends BIF {
    public Object _invoke(IBoxContext context, ArgumentsScope arguments) {
        WebRequestBoxContext requestContext = context.getParentOfType(WebRequestBoxContext.class);
        IBoxHTTPExchange exchange = requestContext.getHTTPExchange();
        // BIF logic here
        return result;
    }
}
```

### HTTP Response Handling Critical Pattern
**IMPORTANT**: When sending files or binary data that should terminate output:
1. Call `context.clearBuffer()` before sending response
2. Use `exchange.sendResponseFile()` or `exchange.sendResponseBinary()`
3. **Always throw `new AbortException()`** to prevent additional content appending
4. Consider setting an abort flag in context if servlet doesn't respect AbortException

### Testing Patterns
- Extend `BaseWebTest` for web-enabled tests
- Mock `IBoxHTTPExchange` for HTTP operations
- Use `WebRequestBoxContext` for request-scoped testing
- Test output in BoxLang syntax: `runtime.executeSource("...", context)`

## Key Directories

- **`src/main/java/ortus/boxlang/web/`**: Core web functionality
  - `bifs/`: Web-specific built-in functions
  - `components/`: Web components (Content, Header, Cookie, etc.)
  - `context/`: WebRequestBoxContext and related classes  
  - `exchange/`: HTTP abstraction layer interfaces
  - `interceptors/`: Event-driven request processing hooks
  - `scopes/`: Web-specific scopes (URL, Form, CGI, etc.)
- **`src/test/java/ortus/boxlang/web/`**: Mirror structure for tests
- **`build/resources/main/META-INF/services/`**: Auto-generated service discovery files

## Build & Development

- **Build**: `./gradlew build` (includes shadow JAR creation)
- **Tests**: `./gradlew test` 
- **Local Development**: Can reference local BoxLang build via `../boxlang/build/libs/`
- **Versioning**: Automatic `-snapshot` suffix for development branch
- **Code Style**: Spotless plugin enforces formatting (`./gradlew spotlessApply`)

## Context Navigation Pattern
Always navigate up the context hierarchy to get web context:
```java
WebRequestBoxContext requestContext = context.getParentOfType(WebRequestBoxContext.class);
IBoxHTTPExchange exchange = requestContext.getHTTPExchange();
```

## Security Notes
- Path traversal protection in WebRequestExecutor (blocks `../`, `..;`, etc.)
- File operations use `FileSystemUtil.expandPath()` for safe path resolution
- HTTP headers and status codes validated through exchange interface
