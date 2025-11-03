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

### Server-Sent Events (SSE) Pattern
For real-time server-to-client streaming:
```java
@BoxBIF
public class MyStreamingBIF extends BIF {
    public Object _invoke(IBoxContext context, ArgumentsScope arguments) {
        // 1. Get web context and exchange
        WebRequestBoxContext requestContext = context.getParentOfType(WebRequestBoxContext.class);
        IBoxHTTPExchange exchange = requestContext.getHTTPExchange();

        // 2. Set SSE headers
        exchange.setResponseHeader("Content-Type", "text/event-stream");
        exchange.setResponseHeader("Cache-Control", "no-cache");
        exchange.setResponseHeader("Connection", "keep-alive");

        // Optional: Set CORS headers if needed
        exchange.setResponseHeader("Access-Control-Allow-Origin", "*");
        exchange.setResponseHeader("Access-Control-Allow-Credentials", "true");

        // 3. Clear buffer to prevent corruption
        context.clearBuffer();

        // 4. Create SSEEmitter helper (handles formatting, keep-alive, errors)
        SSEEmitter emitter = new SSEEmitter(exchange, retry, keepAliveInterval, context);

        // 5. For async execution, use io-tasks executor with context preservation
        if (async) {
            runtime.getExecutorService().getExecutor("io-tasks").submit(
                () -> {
                    try {
                        callback.invoke(context, new Object[]{emitter});
                    } finally {
                        emitter.cleanup();
                    }
                }
            );
            return null; // Returns immediately
        }

        // 6. Synchronous execution (blocks until complete)
        callback.invoke(context, new Object[]{emitter});

        // 7. Throw AbortException to prevent further output
        throw new AbortException();
    }
}
```

**SSEEmitter Responsibilities**:
- Format messages per SSE spec (`data: ...\n\n`)
- Auto-serialize complex data to JSON via `JSONUtil.getJSONBuilder().asString(data)`
- Handle keep-alive comments via scheduled-tasks executor
- Detect client disconnects (IOException on flush)
- Log errors to `runtime.getLoggingService().APPLICATION_LOGGER`
- Clean up resources (cancel scheduled tasks)
- **CORS Support**: SSE BIF provides optional `cors` argument for cross-origin requests (e.g., `cors="*"` or `cors="https://app.example.com"`)

**SSEEmitter Implementation Details**:
- Implements `AutoCloseable` for try-with-resources support in Java callers
- Use `AtomicBoolean` for thread-safe closed state
- Synchronize writer access for concurrent operations
- Use `scheduledExecutor.scheduledExecutor().scheduleAtFixedRate()` for keep-alive
- Store `ScheduledFuture<?>` for task cancellation in cleanup
- Handle multi-line data by splitting on any line ending (CRLF `\r\n`, LF `\n`, CR `\r`) and prefixing each line with `data:`
- Use `IsSimpleValue.isSimpleValue()` to detect complex types for JSON serialization
- Truncate debug log data to 100 chars to prevent log flooding
- **Future Enhancement**: IBoxHTTPExchange could support onComplete/onClose listeners for automatic cleanup if handler exits unexpectedly

### BoxLang-Java Interop Patterns
**CRITICAL**: When passing Java objects to BoxLang closures:
- **Named arguments NOT supported**: BoxLang cannot call Java methods with named arguments
- **Use positional arguments only**: `emit.send(data, event, id)` ✅ NOT `emit.send(data="x", event="y")` ❌
- **Error message**: "Methods on Java objects cannot be called with named arguments"
- **Testing**: Always test with `runtime.executeSource()` to catch interop issues

### Testing Patterns
- Extend `BaseWebTest` for web-enabled tests
- Mock `IBoxHTTPExchange` for HTTP operations
- Use `when(mockExchange.getResponseWriter()).thenReturn(printWriter)` to capture output
- Test output in BoxLang syntax: `runtime.executeSource("...", context)`
- Expect `AbortException` for BIFs that should terminate output
- Use `Thread.sleep()` after async operations to allow completion
- Test edge cases: multi-line data, arrays, structs, closed streams, buffer clearing

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

## Executor Services
BoxLang provides specialized thread executors via `runtime.getAsyncService().getExecutor(name)`:
- **`io-tasks`**: Virtual thread executor for I/O-bound operations (SSE, file operations, HTTP calls)
- **`scheduled-tasks`**: ScheduledExecutorService for periodic tasks (keep-alive, polling)
- **Context preservation**: Wrap callbacks with `ThreadBoxContext.runInContext()` when needed

## Logging Best Practices
- Use `runtime.getLoggingService().APPLICATION_LOGGER` for application-level logs
- **Debug logging**: Log at DEBUG level for detailed operation traces (enable via config)
- **Error logging**: Use `logger.error(message, exception)` with stack traces
- **Data truncation**: Truncate large payloads in debug logs (e.g., `data.substring(0, 100) + "..."`)
- **Client disconnects**: Log at DEBUG (expected), not ERROR
- **Log formatting**: Include operation context (e.g., "SSE sending data: ...")

## Security Notes
- Path traversal protection in WebRequestExecutor (blocks `../`, `..;`, etc.)
- File operations use `FileSystemUtil.expandPath()` for safe path resolution
- HTTP headers and status codes validated through exchange interface
