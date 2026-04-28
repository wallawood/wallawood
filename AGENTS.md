# AGENTS.md — Gemini Protocol Server Library

## Project Overview

A Java 21 library that implements the Gemini protocol (gemini://geminiprotocol.net/docs/protocol-specification.gmi) as a server framework. The goal is to give Java developers a Spring Boot-like experience for building Gemini servers — annotation-driven controllers, classpath scanning, convention over configuration.

This is a **library**, not an application. No `main()` method. Users depend on it and write controllers.

## Design Philosophy

- **Spring Boot ergonomics, Gemini simplicity.** The API should feel familiar to Spring developers but stay true to Gemini's minimalist protocol. No over-engineering.
- **Convention over configuration.** Sensible defaults everywhere. Static files live in `src/main/resources/static/`. MIME type for gemtext is `text/gemini; charset=utf-8`. Port 1965 is the Gemini default.
- **Performance-first for Netty.** The response class writes directly to Netty's pooled `ByteBuf` — no intermediate byte array copies. Lazy serialization at send time.
- **Minimal public API surface.** Internal machinery (Spring scanner, path matcher, route registry) is package-private or in the `internal` package and shaded via Shadow plugin. Users interact with `GeminiServer`, `GeminiResponse`, `@GeminiController`, and Jakarta annotations.
- **Immutability and thread safety.** `GeminiResponse` is immutable. `RouteRegistry` is immutable after construction. Controllers are singletons shared across Netty event loop threads (documented in `@GeminiController` javadoc).

## Architecture

```
User's App
  └── @GeminiController classes with @Path methods
        │
        ▼
GeminiServer.start(MyApp.class, 1965)
  │
  ├── RouteScanner (Spring ClassPathScanningCandidateComponentProvider)
  │     └── Finds @GeminiController classes in the anchor class's package
  │     └── Instantiates controllers via no-arg constructor
  │     └── Builds RouteRegistry from @Path annotations
  │
  ├── RouteRegistry (Spring PathPatternParser)
  │     └── Maps URI patterns to HandlerMethod (controller + Method)
  │     └── Supports path variables: /users/{id}
  │     └── match() returns MatchedRoute with extracted path variables
  │
  ├── StaticResourceResolver
  │     └── Classpath lookup under /static/
  │     └── Directory paths auto-resolve to index.gmi
  │     └── MIME type inference (.gmi, .txt, .png, etc.)
  │     └── Path traversal protection
  │
  └── Reactor Netty (TLS, TCP) [TODO]
        └── Request pipeline: static → dynamic → 51 Not Found
```

## Request Pipeline (Planned)

```
Incoming Gemini request (URI over TLS)
  1. StaticResourceResolver.resolve(path) → if found, serve and close
  2. RouteRegistry.match(path) → if matched, invoke handler method
  3. Neither → GeminiResponse.notFound()
```

Static resources intentionally clobber dynamic routes for the same path. This aligns with Gemini's document-first philosophy.

## Key Design Decisions

### GeminiResponse
- Immutable container with factory methods for every Gemini status code (10–62).
- No eager byte serialization. `toByteBuf(ByteBufAllocator)` writes directly to Netty's pooled buffers at send time.
- Convenience `success(String body)` and `success(byte[] body)` default to `text/gemini; charset=utf-8`.
- `toString()` follows Spring's `HttpEntity` convention: `<20 Success,text/gemini; charset=utf-8,1234 bytes>`.
- `StatusCodes` enum is package-private. Users never see it — they use `GeminiResponse.notFound()` etc.

### @GeminiController
- Required marker annotation for controller classes. Explicit opt-in, not implicit.
- Class-level `@Path` is optional for route prefixing.
- Method-level `@Path` is required for route registration.
- Controllers are singletons — documented thread-safety requirement.

### Jakarta Annotations (JAX-RS)
Using Jakarta WS RS annotations (`@Path`, `@PathParam`, `@QueryParam`, `@DefaultValue`) rather than inventing custom ones. This is familiar to Java developers and avoids reinventing the wheel.

**Not using** from JAX-RS (irrelevant to Gemini):
- `@GET`/`@POST`/etc — Gemini has no HTTP methods
- `@Consumes` — Gemini requests have no body
- `@HeaderParam`, `@CookieParam`, `@FormParam` — Gemini has no headers, cookies, or forms

**Planned support:**
- `@PathParam` — path variables
- `@QueryParam` — query string (Gemini's mechanism for user input from 1x responses)
- `@DefaultValue` — default values for params
- `@Produces` — MIME type declaration (default `text/gemini`)
- `@Context` — inject request URI, client certificate info

### Spring as a Shaded Utility
Spring is used purely as a library, not a framework:
- `ClassPathScanningCandidateComponentProvider` for controller discovery
- `PathPatternParser` / `PathPattern` for URI matching
- No `ApplicationContext`, no DI, no lifecycle management
- Everything is shaded via Shadow plugin so users never see Spring on their classpath

### Static File Serving
- Convention: `src/main/resources/static/` on the classpath
- Auto index: directory paths resolve to `index.gmi`
- MIME inference: `.gmi`/`.gemini` → gemtext, JDK guessing for known types, `application/octet-stream` fallback
- Path traversal (`..`) rejected with 59 Bad Request
- Takes precedence over dynamic routes

## Build & Tooling

- **Java 21** via sdkman (`.sdkmanrc` pins `java=21.0.6-tem`)
- **Gradle 8.5** (upgraded from 7.5.1 for Java 21 support)
- **Shadow plugin** `com.gradleup.shadow:8.3.6` — shades Spring and Micrometer into `org.server.gemini.internal.*`
- **JUnit 5** for testing
- **Group ID:** `org.server.gemini`
- **Package structure:** `org.server.gemini` (public API), `org.server.gemini.internal` (internals)

## What's Next (TODO)

1. **Handler method invocation** — resolve `@PathParam`, `@QueryParam`, `@DefaultValue` from the matched route and inject them into handler method parameters.
2. **Return type handling** — support returning `GeminiResponse` directly, and possibly plain `String` (auto-wrapped as gemtext success).
3. **Reactor Netty integration** — TLS server, request parsing, response writing via `toByteBuf()`, connection close after response.
4. **Client certificate support** — `@Context` injection for TLS client cert info (needed for 6x status codes).
5. **`@Produces` support** — MIME type declaration on handler methods.
6. **Error handling** — global exception handler, mapping uncaught exceptions to 40/50 responses.

## Developer Preferences

- Prefers concise, minimal code. No unnecessary abstractions or defensive code beyond what's needed.
- Wants javadocs that reference the actual Gemini protocol spec behavior, not generic descriptions.
- Values performance — chose lazy `ByteBuf` serialization over eager `byte[]` after discussing tradeoffs.
- Likes Spring Boot conventions but wants a clean, independent library — not a Spring module.
- Prefers to discuss design before implementation. Asks "am I on the right track?" and wants honest feedback.
- Appreciates when the agent explains tradeoffs and makes recommendations rather than just executing.
- Tests are important — expects thorough test coverage for every class.
- Strips comments from production code, relies on javadocs instead.
- Uses sdkman for Java version management, scoped to the repo.
