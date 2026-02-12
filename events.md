# Core Interception Events

This document lists the core web interception points and the structure of the `data` struct passed to interceptors.

### onWebExecutorRequest
**Description:** Fired before the web executor processes an incoming request; allows interceptors to modify the request string or inspect the exchange/context/app listener.
**Data Structure:**
| Key | Type | Description |
| :--- | :--- | :--- |
| `updatedRequest` | `IStruct` | Mutable struct where interceptors can set a new `requestString` (see nested key below). |
| `updatedRequest.requestString` | `String` | (nested) If set, replaces the request URI to be processed. |
| `context` | `WebRequestBoxContext` | The current web request context. |
| `appListener` | `BaseApplicationListener` | The application listener handling the request. |
| `requestString` | `String` | The original request URI string. |
| `exchange` | `IBoxHTTPExchange` | The low-level HTTP exchange for the request. |

### writeToBrowser
**Description:** Requests that content be written directly to the HTTP response (may abort the request flow).
**Data Structure:**
| Key | Type | Description |
| :--- | :--- | :--- |
| `context` | `IBoxContext` | The execution context (used to obtain `WebRequestBoxContext`). |
| `content` | `String` or `byte[]` or `Object` | The payload to send to the client (string or binary). |
| `mimetype` | `String` | Response MIME type (defaults to `text/html`). |
| `fileName` | `String` | Optional filename — when present response is sent as attachment (content-disposition). |
| `reset` | `Boolean` | If `true`, clears the context buffer before writing. |
| `abort` | `Boolean` | If `true`, an `AbortException` is thrown after sending (terminates further processing). |
| `success` | `Boolean` (output) | Set to `true` on success (written back into `data`). |

### onFileComponentAction
**Description:** Intercepts file component actions (e.g., uploads) so the runtime can route them to the appropriate BIF handler.
**Data Structure:**
| Key | Type | Description |
| :--- | :--- | :--- |
| `attributes` | `IStruct` | Component attributes; must include `action` (e.g., `"upload"` or `"uploadAll"`). |
| `context` | `IBoxContext` | The execution context for the component invocation. |
| `response` | `Object` (output) | Result produced by invoking the file-upload BIF; written into `data.response`. |

### onComponentInvocation
**Description:** Hook invoked around component calls (used here to handle cache directives for `Cache` components).
**Data Structure:**
| Key | Type | Description |
| :--- | :--- | :--- |
| `component` | `Component` | The `Component` instance being invoked. |
| `context` | `IBoxContext` | The execution context. |
| `attributes` | `IStruct` | Component attributes (e.g., `action`, `timespan`). |
| `body` | `Component.ComponentBody` | Component body callback (may be `null`). |
| `result` | `Object` | Optional pre-existing result; interceptor may set/override this (e.g., cache directive string). |

### onConfigurationLoad
**Description:** Fired when the runtime configuration is loaded; allows the web runtime to add or modify defaults.

**Data Structure:**
| Key | Type | Description |
| :--- | :--- | :--- |
| `config` | `Configuration` | The runtime `Configuration` object being loaded (mutable). |

---

If you want these entries expanded with example payloads or source links, tell me 