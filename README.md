# Riptide: Client-side response routing

[![Tidal wave](docs/wave.jpg)](http://pixabay.com/en/wave-water-sea-tsunami-giant-wave-11061/)

[![Build Status](https://img.shields.io/travis/zalando/riptide/master.svg)](https://travis-ci.org/zalando/riptide)
[![Coverage Status](https://img.shields.io/coveralls/zalando/riptide/master.svg)](https://coveralls.io/r/zalando/riptide)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/org.zalando/riptide-core/badge.svg)](http://www.javadoc.io/doc/org.zalando/riptide-core)
[![Release](https://img.shields.io/github/release/zalando/riptide.svg)](https://github.com/zalando/riptide/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-core.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-core)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando/riptide/master/LICENSE)

> **Riptide** noun, /ˈrɪp.taɪd/: strong flow of water away from the shore

*Riptide* is a library that implements ***client-side response routing***.  It tries to fill the gap between the HTTP
protocol and Java. Riptide allows users to leverage the power of HTTP with its unique API.

- **Technology stack**: Based on `spring-web` and uses the same foundation as Spring's RestTemplate.
- **Status**:  Actively maintained and used in production.
- Riptide is unique in the way that it doesn't abstract HTTP away, but rather embrace it!

## Example

Usage typically looks like this:

```java
http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
    .dispatch(series(),
        on(SUCCESSFUL).call(listOf(User.class), users -> 
            users.forEach(System.out::println)));
```

Feel free to compare this e.g. to [Feign](https://github.com/Netflix/feign#basics) or
[Retrofit](https://github.com/square/retrofit/blob/master/samples/src/main/java/com/example/retrofit/SimpleService.java).

## Features

- full access to the underlying HTTP client
- encourages to write more resilient clients, by forcing you to consider
  - fallbacks
  - content negotiation
  - robust error handling
- elegant syntax
- type-safe
- asynchronous by default
- bulkhead pattern through isolated thread and connection pools
- [Failsafe integration](riptide-failsafe)
- [Hystrix integration](riptide-hystrix)
- [`application/problem+json` support](riptide-problem)
- [streaming](riptide-stream)

## Origin

Most modern clients try to adapt HTTP to a single-return paradigm as shown in the following example. Even though this
may be perfectly suitable for most applications it takes away a lot of the power that comes with HTTP. It's not easy to
support multiple different return values, i.e. distinct happy cases. Access to response headers or manual content
negotiation are also harder to do.
 
```java
@GET
@Path("/repos/{org}/{repo}/contributors")
List<User> getContributors(@PathParam String org, @PathParam String repo);
```
Riptide tries to counter this by providing a different approach to leverage the power of HTTP.

## Concepts

It allows to dispatch HTTP responses very easily to different handler methods based on any characteristic of the
response, including but not limited to status code, status family and content type. The way this works is intentionally
very similar to server-side request routing where any request that reaches a web application is usually routed to the
correct handler based on any combination of the following criteria: URI including query and path parameters, method,
`Accept` and `Content-Type` header. Instead of routing requests to handler methods on the server what *Riptide* does
is the exact opposite: routing responses to handler methods on the client side.

![Routing Tree](https://docs.google.com/drawings/d/1BRTXVtmwIMJti1l5cQMrZsfKnTfBElTB8pDSxVBQbIQ/pub?w=888&h=691)

### Route

> A Route is either a user-supplied **callback or** a nested **[routing tree](#routing-tree)**. Following a route will
  execute the callback or traverse the routing tree respectively.

```java
on(SUCCESSFUL).call(response ->
    System.out.println(response.getHeaders().getLocation()))
```

### Routing Tree

> A Routing Tree is a route that is represented as the combination of a **[navigator](#navigator) and** a set of 
  **[bindings](#binding)**.

```java
on(SUCCESSFUL).dispatch(contentType(),
    on(APPLICATION_JSON).call(..),
    on(APPLICATION_XML).call(..))
```

### Navigator

> A Navigator **chooses among** the **[bindings](#binding)** of a [routing tree](#routing-tree). 
  The act of **traversing a [routing tree](#routing-tree)** by choosing a binding and following its associated route is
  called **nested dispatch**.

| Navigator                                                                                              | Aspect               |
|--------------------------------------------------------------------------------------------------------|----------------------|
| [Navigators.series()](riptide-core/src/main/java/org/zalando/riptide/SeriesNavigator.java)             | Class of status code |
| [Navigators.status()](riptide-core/src/main/java/org/zalando/riptide/StatusNavigator.java)             | Status               |
| [Navigators.statusCode()](riptide-core/src/main/java/org/zalando/riptide/StatusCodeNavigator.java)     | Status code          |
| [Navigators.reasonPhrase()](riptide-core/src/main/java/org/zalando/riptide/ReasonPhraseNavigator.java) | Reason Phrase        |
| [Navigators.contentType()](riptide-core/src/main/java/org/zalando/riptide/ContentTypeNavigator.java)   | Content-Type header  |

### Binding

> A Binding **binds an attribute to a [route](#route)**. It represents a choice to the [navigator](#navigator) which
  route to follow.

| Route                                  | Syntax                                              |
|----------------------------------------|-----------------------------------------------------|
| `ThrowingRunnable`                     | `on(..).call(ThrowingRunnable)`                     |
| `ThrowingConsumer<ClientHttpResponse>` | `on(..).call(ThrowingConsumer<ClientHttpResponse>)` |
| `ThrowingConsumer<T>`                  | `on(..).call(Class<T>, ThrowingConsumer<T>)`        |
| `ThrowingConsumer<T>`                  | `on(..).call(TypeToken<T>, ThrowingConsumer<T>)`    |
| `RoutingTree`                          | `on(..).dispatch(..)`                               |

## Dependencies

- Spring 4.1.0 or higher

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-core</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

Additional modules/artifacts of Riptide always share the same version number.

## Configuration

```java
Http.builder()
    .baseUrl("https://api.github.com")
    .requestFactory(new HttpComponentsClientHttpRequestFactory())
    .converter(new MappingJackson2HttpMessageConverter())
    .converter(new Jaxb2RootElementHttpMessageConverter())
    .plugin(new OriginalStackTracePlugin())
    .build();
```

Since all properties are optional the following code is the bare minimum:

```java
Http.builder().build();
```

This defaults to:
- no base URL
- `SimpleClientHttpRequestFactory` (based on `java.net.HttpURLConnection`)
- same list of converters as `new RestTemplate()`
- `OriginalStackTracePlugin` which preserves stack traces when executing requests asynchronously

Integration of your typical Spring Boot Application with Riptide, [Logbook](https://github.com/zalando/logbook) and
[Tracer](https://github.com/zalando/tracer) can be greatly simplified by using 
[**Put it to REST!**](https://github.com/zalando-incubator/put-it-to-rest). Go check it out!

## Usage

A full-blown request may contain any of the following aspects: HTTP method, request URI, query parameters,
headers and a body:

```java
http.post("/sales-order")
    .queryParam("async", "false")
    .contentType(CART)
    .accept(SALES_ORDER)
    .header("Client-IP", "127.0.0.1")
    .body(cart)
    .dispatch(series(),
        on(SUCCESSFUL).dispatch(contentType(),
            on(SALES_ORDER).call(this::persistLocationHeader),
        on(CLIENT_ERROR).dispatch(status(),
            on(CONFLICT).call(this::retry),
            on(PRECONDITION_FAILED).call(this::readAgainAndRetry),
            anyStatus().call(problemHandling())),
        on(SERVER_ERROR).dispatch(status(),
            on(SERVICE_UNAVAILABLE).call(this::scheduleRetryLater))))
    .join();
```

Riptide the the following HTTP methods: `get`, `head`, `post`, `put`, `patch`, `delete`, `options` and `trace`
respectively. Query parameters can either be provided individually using `queryParam(String, String)` or multiple at 
once with `queryParams(Multimap<String, String>)`.

The following operations are applied to URI Templates (`get(String, Object...)`) and URIs (`get(URI)`) respectively:

**URI Template**
- parameter expansion, e.g `/{id}` (see [`UriTemplate.expand`](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/util/UriTemplate.html#expand-java.lang.Object...-))
- encoding

**URI**
- none, used *as is*
- expected to be already encoded

**Both**
- after respective transformation
- resolved against Base URL (if present)
- Query String (merged with existing)
- Normalization

The following table shows some examples how URIs are resolved against Base URLs, 
based on the chosen resolution strategy:

| Base URL                   | Resolution | URI / URI Template        | Result                        |
|----------------------------|------------|---------------------------|-------------------------------|
|`https://example.com`|`RFC`|`null`|`https://example.com`|
|`https://example.com/`|`RFC`|`null`|`https://example.com/`|
|`https://example.com`|`RFC`|(empty)|`https://example.com`|
|`https://example.com/`|`RFC`|(empty)|`https://example.com/`|
|`https://example.com`|`RFC`|`/`|`https://example.com/`|
|`https://example.com/`|`RFC`|`/`|`https://example.com/`|
|`https://example.com`|`RFC`|`https://example.org/foo`|`https://example.org/foo`|
|`https://example.com`|`RFC`|`/foo/bar`|`https://example.com/foo/bar`|
|`https://example.com`|`RFC`|`foo/bar`|`https://example.com/foo/bar`|
|`https://example.com/api`|`RFC`|`/foo/bar`|`https://example.com/foo/bar`|
|`https://example.com/api`|`RFC`|`foo/bar`|`https://example.com/foo/bar`|
|`https://example.com/api/`|`RFC`|`/foo/bar`|`https://example.com/foo/bar`|
|`https://example.com/api/`|`RFC`|`foo/bar`|`https://example.com/api/foo/bar`|
|`null`|`RFC`|`https://example.com/foo`|`https://example.com/foo`|
|`/foo`|`RFC`|`/`|Exception|
|`null`|`RFC`|`null`|Exception|
|`null`|`RFC`|`/foo`|Exception|
|`null`|`RFC`|`foo`|Exception|
|`https://example.com/api`|`APPEND`|`/foo/bar`|`https://example.com/api/foo/bar`|
|`https://example.com/api`|`APPEND`|`foo/bar`|`https://example.com/api/foo/bar`|
|`https://example.com/api/`|`APPEND`|`/foo/bar`|`https://example.com/api/foo/bar`|
|`https://example.com/api/`|`APPEND`|`foo/bar`|`https://example.com/api/foo/bar`|

The `Content-Type`- and `Accept`-header have type-safe methods in addition to the generic support that is
`header(String, String)` and `headers(HttpHeaders)`.

The callbacks can have the following signatures:

```java
private void persistLocationHeader(ClientHttpResponse response)
private void retry();
private void propagate(ThrowableProblem problem);
```

### Futures

Riptide will return a `CompletableFuture<Void>`. That means you can choose to chain transformations/callbacks or block
on it.

If you need proper return values take a look at [Riptide: Capture](riptide-capture).

#### Exceptions

The only special custom exception you may get is `NoRouteException`, if and only if there was no matching condition and
no wildcard condition either.

## Getting help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../../issues).

## Getting involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change.
For more details check the [contribution guidelines](CONTRIBUTING.md).

## Credits and references

- [URL routing](http://littledev.nl/?p=99)
