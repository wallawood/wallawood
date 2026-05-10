# Wallawood

A Java library for building [Gemini protocol](https://geminiprotocol.net) servers.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.wallawood/wallawood)](https://central.sonatype.com/artifact/io.github.wallawood/wallawood)

## Quickstart

**1. Add the dependency**

Gradle:
```groovy
dependencies {
    implementation 'io.github.wallawood:wallawood:0.8.1'
}
```

Maven:
```xml
<dependency>
    <groupId>io.github.wallawood</groupId>
    <artifactId>wallawood</artifactId>
    <version>0.8.1</version>
</dependency>
```

**2. Create a static page**

Create `src/main/resources/static/index.gmi`:
```
# Hello, Gemini!

Welcome to my capsule.
```

**3. Start the server**

```java
public class App {
    public static void main(String[] args) {
        WallawoodServer.start(App.class);
    }
}
```

Run the app and point any Gemini client at `gemini://localhost` — your page is live. On first startup a self-signed TLS certificate is generated and saved to `./gemini-certs/` for reuse.

## Dynamic routes

```java
@GeminiController
public class HelloController {

    @Path("/hello/{name}")
    public GeminiResponse greet(@PathParam("name") String name) {
        return GeminiResponse.success("# Hello, " + name + "!");
    }
}
```

Static resources are checked first; unmatched requests fall through to controllers.

## Configuration

Zero-config defaults work out of the box. For production, supply an `application.properties` file in the working directory:

```properties
wallawood.hostname=example.com
wallawood.port=1965
wallawood.cert.path=cert.pem
wallawood.key.path=key.pem
wallawood.static.directories=file:./content
```

Or configure programmatically:

```java
var config = WallawoodConfig.builder()
    .hostname("example.com")
    .certificate(Path.of("cert.pem"), Path.of("key.pem"))
    .staticDirectories(List.of("file:./content"))
    .build();

WallawoodServer.start(App.class, config);
```

## Annotations

| Annotation | Description |
|---|---|
| `@GeminiController` | Marks a class as a route handler |
| `@Path` | Maps a method to a URL path (supports `{param}` segments) |
| `@PathParam` | Binds a path segment to a method parameter |
| `@QueryString` | Injects the raw query string |
| `@QueryParam` | Injects a named query parameter |
| `@Context` | Injects a request-scoped object (e.g. `X509Certificate`, custom types) |
| `@RequireInput` | Returns a Gemini input prompt if the query string is absent |
| `@RequireSensitiveInput` | Like `@RequireInput` but uses sensitive input (status 11) |
| `@RequireCertificate` | Requires a client TLS certificate |
| `@RequireAuthorized` | Requires `grant.isAuthorized() == true` |
| `@RequireClearance` | Requires `grant.level() >= level` |
| `@RequireScopes` | Requires the grant to contain all specified scope strings |
| `@Preprocessor` | Marks a `RequestInterceptor` to run before route handlers |
| `@Component` | Marks a class for automatic instantiation and injection |
| `@GeminiExceptionHandler` | Marks a method to handle a specific exception type |

## Requirements

- Java 21+
