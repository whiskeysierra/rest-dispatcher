# Riptide: Spring Boot Starter

[![Leafs](../docs/leafs.jpg)](https://pixabay.com/en/leaf-green-foliage-green-leaves-1001679/)

[![Javadoc](https://www.javadoc.io/badge/org.zalando/riptide-spring-boot-starter.svg)](http://www.javadoc.io/doc/org.zalando/riptide-spring-boot-starter)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/riptide-spring-boot-starter.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/riptide-spring-boot-starter)

*Riptide: Spring Boot Starter* is a library that seamlessly integrates various HTTP client-side tools in the
easiest and convenient way possible. It solves a recurring problem of bootstrapping and wiring different libraries
together whenever interaction with a remote service is required. Spinning up new clients couldn't get any easier!

- **Technology stack**: Spring Boot
- **Status**:  Beta

## Example

```yaml
riptide.clients:
  example:
    base-url: http://example.com
    connections:
      connect-timeout: 150 milliseconds
      socket-timeout: 100 milliseconds
      time-to-live: 30 seconds
      max-per-route: 16
    retry:
      enabled: true
      fixed-delay: 50 milliseconds
      max-retries: 5
    circuit-breaker:
      enabled: true
      failure-threshold: 3 out of 5
      delay: 30 seconds
      success-threshold: 5 out of 5
    caching:
      enabled: true
      shared: false
      max-cache-entries: 1000
    tracing:
      enabled: true
      tags:
        peer.service: example
```

```java
@Autowired
@Qualifier("example")
private Http example;
```

## Features

- Seamless integration of:
  - [Riptide](https://github.com/zalando/riptide)
  - [Logbook](https://github.com/zalando/logbook)
  - [Tracer](https://github.com/zalando/tracer)
  - [Jackson 2](https://github.com/FasterXML/jackson)
  - [HttpClient](https://hc.apache.org/httpcomponents-client-ga/index.html)
  - [Failsafe](https://github.com/jhalterman/failsafe) via [Riptide: Failsafe](../riptide-failsafe)
  - [Micrometer](https://micrometer.io) via [Riptide: Micrometer](../riptide-micrometer)
  - SOAP via [Riptide: SOAP](../riptide-soap)
- [Spring Boot](http://projects.spring.io/spring-boot/) Auto Configuration
- Automatically integrates and supports:
  - Transient fault detection via [Riptide: Faults](../riptide-faults)
  - HTTP JSON Streaming via [Riptide: Stream](../riptide-stream)
  - Timeouts via [Riptide: Failsafe](../riptide-failsafe)
  - Platform IAM OAuth tokens via [Riptide: Auth](../riptide-auth)
- SSL certificate pinning
- Sensible defaults

## Dependencies

- Java 8
- Spring Boot 2
- Riptide
  - Core
  - (Apache) HTTP Client
  - Compression
  - Backup (optional)
  - Failsafe (optional)
  - Faults (optional)
  - Logbook (optional)
  - Micrometer (optional)
  - Timeouts (optional)
  - SOAP (optional)
- Tracer (optional)

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-spring-boot-starter</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

### Optional Dependencies

You will need to add declare the following dependencies, in order to enable some integrations and/or features:

#### [Failsafe](../riptide-failsafe) integration

Required for `retry`, `circuit-breaker`, `backup-request` and `timeout` support. Timeout is not to be confused with `connect-timeout` and `socket-timeout`, those are supported out of the box.

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-failsafe</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

#### [Transient Fault](../riptide-faults) detection

Required when `transient-fault-detection` is enabled.

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-faults</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

#### [SOAP](../riptide-soap) support

Required when `soap` is enabled.

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-soap</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

#### [Logbook](https://github.com/zalando/logbook) integration

Required when `logging` is enabled.

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-logbook</artifactId>
    <version>${riptide.version}</version>
</dependency>
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>logbook-spring-boot-autoconfigure</artifactId>
    <version>${logbook.version}</version>
</dependency>
```

#### [Tracer](https://github.com/zalando/tracer) integration

Required when `tracing` is enabled.

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>tracer-spring-boot-autoconfigure</artifactId>
    <version>${tracer.version}</version>
</dependency>
```

#### Authorization support

Required for `auth` support.

Registers a special AuthorizationProvider that built for Zalando's Platform IAM which provides
OAuth2 tokens as files in a mounted directory. See 
[Zalando Platform IAM Integration](https://kubernetes-on-aws.readthedocs.io/en/latest/user-guide/zalando-iam.html) for more details.

Registering a custom `AuthorizationPlugin` or `AuthorizationProvider` will override the default. Setting `auth.enabled: true` is still required in that case.

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-auth</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

#### [Micrometer](../riptide-micrometer) integration

Required when `metrics` is enabled. 

Will activate *Micrometer* metrics support for:

- requests
- thread pools
- connection pools
- retries
- circuit breaker

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-micrometer</artifactId>
    <version>${riptide.version}</version>
</dependency>
```

Spring Boot 1.x applications also require:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-spring-legacy</artifactId>
    <version>${micrometer.version}</version>
</dependency>
```

Please be aware that Micrometer, by default, doesn't expose to `/metrics`.
Consult [#401](https://github.com/zalando/riptide/issues/401) for details how to bypass this.

#### Caching

Required when `caching` is configured:

```xml
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient-cache</artifactId>
    <version>${httpclient.version}</version>
</dependency>
```

#### Tracing

Required when `tracing` is configured:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>riptide-opentracing</artifactId>
    <version>${riptide.version}</version>
</dependency>
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-concurrent</artifactId>
    <version>${opentracing-concurrent.version}</version>
</dependency>
```

## Configuration

You can now define new clients and override default configuration in your `application.yml`:

```yaml
riptide:
  defaults:
    auth:
      credentials-directory: /secrets
    tracing:
      enabled: true
      tags:
        account: ${CDP_TARGET_INFRASTRUCTURE_ACCOUNT}
        zone: ${CDP_TARGET_REGION}
        artifact_version: ${CDP_BUILD_VERSION}
        deployment_id: ${CDP_DEPLOYMENT_ID}
  clients:
    example:
      base-url: http://example.com
      connections:
        connect-timeout: 150 milliseconds
        socket-timeout: 100 milliseconds
        time-to-live: 30 seconds
        max-per-route: 16
      threads:
        min-size: 4
        max-size: 16
        keep-alive: 1 minnute
        queue-size: 0
      auth:
        enabled: true
      transient-fault-detection.enabled: true
      stack-trace-preservation.enabled: true
      retry:
        enabled: true
        fixed-delay: 50 milliseconds
        max-retries: 5
        max-duration: 2 second
        jitter: 25 milliseconds
      circuit-breaker:
        enabled: true
        failure-threshold: 3 out of 5
        delay: 30 seconds
        success-threshold: 5 out of 5
      backup-request:
        enabled: true
        delay: 75 milliseconds
      timeouts:
        enabled: true
        global: 500 milliseconds
      caching:
        enabled: true
        shared: true
        directory: /var/cache/http
        max-object-size: 8192 # kilobytes
        max-cache-entries: 1000
        heuristic:
          enabled: true
          coefficient: 0.1
          default-life-time: 10 minutes
      tracing:
        tags:
          peer.service: example
        propagate-flow-id: true
```

Clients are identified by a *Client ID*, for instance `example` in the sample above. You can have as many clients as you want.

### Reference

For a complete overview of available properties, they type and default value please refer to the following table:

| Configuration                           | Data type      | Default / Comment                                |
|-----------------------------------------|----------------|--------------------------------------------------|
| `riptide`                               |                |                                                  |
| `├── defaults`                          |                |                                                  |
| `│   ├── auth`                          |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   └── credentials-directory`     | `Path`         | `/meta/credentials`                              |
| `│   ├── backup-request`                |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   └── delay`                     | `TimeSpan`     | no delay                                         |
| `│   ├── caching`                       |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   ├── shared`                    | `boolean`      | `false`                                          |
| `│   │   ├── directory`                 | `String`       | none, *in-memory* caching by default             |
| `│   │   ├── max-object-size`           | `int`          | `8192`                                           |
| `│   │   ├── max-cache-entries`         | `int`          | `1000`                                           |
| `│   │   └── heuristic`                 |                | If max age was not specified by the server       |
| `│   │       ├── enabled`               | `boolean`      | `false`                                          |
| `│   │       ├── coefficient`           | `double`       | `0.1`                                            |
| `│   │       └── default-life-time`     | `TimeSpan`     | `0 seconds`, disabled                            |
| `│   ├── certificate-pinning`           |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   └── keystore`                  |                |                                                  |
| `│   │       ├── path`                  | `Path`         | none                                             |
| `│   │       └── password`              | `String`       | none                                             |
| `│   ├── chaos`                         |                |                                                  |
| `│   │   ├── latency`                   |                |                                                  |
| `│   │   │   ├── enabled`               | `boolean`      | `false`                                          |
| `│   │   │   ├── probability`           | `double`       | `0.01`                                           |
| `│   │   │   └── delay`                 | `TimeSpan`     | `1 second`                                       |
| `│   │   ├── exceptions`                |                |                                                  |
| `│   │   │   ├── enabled`               | `boolean`      | `false`                                          |
| `│   │   │   └── probability`           | `double`       | `0.001`                                          |
| `│   │   └── error-responses`           |                |                                                  |
| `│   │       ├── enabled`               | `boolean`      | `false`                                          |
| `│   │       ├── probability`           | `double`       | `0.001`                                          |
| `│   │       └── status-codes`          | `int[]`        | `[500, 503]`                                     |
| `│   ├── circuit-breaker`               |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   ├── failure-threshold`         | `Ratio`        | none                                             |
| `│   │   ├── delay`                     | `TimeSpan`     | no delay                                         |
| `│   │   └── success-threshold`         | `Ratio`        | `failure-threshold`                              |
| `│   ├── connections`                   |                |                                                  |
| `│   │   ├── lease-request-timeout`     | `TimeSpan`     | `1 second`                                       |
| `│   │   ├── connect-timeout`           | `TimeSpan`     | `5 seconds`                                      |
| `│   │   ├── socket-timeout`            | `TimeSpan`     | `5 seconds`                                      |
| `│   │   ├── time-to-live`              | `TimeSpan`     | `30 seconds`                                     |
| `│   │   ├── max-per-route`             | `int`          | `20`                                             |
| `│   │   ├── max-total`                 | `int`          | `20` (or at least `max-per-route`)               |
| `│   │   └── mode`                      | `String`       | `streaming` (alternative is `buffering`)         |
| `│   ├── logging`                       |                |                                                  |
| `│   │   └── enabled`                   | `boolean`      | `false`                                          |
| `│   ├── metrics`                       |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   └── tags`                      | `Map`          | none                                             |
| `│   ├── request-compression`           |                |                                                  |
| `│   │   └── enabled`                   | `boolean`      | `false`                                          |
| `│   ├── retry`                         |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   ├── fixed-delay`               | `TimeSpan`     | none, mutually exclusive to `backoff`            |
| `│   │   ├── backoff`                   |                | none, mutually exclusive to `fixed-delay`        |
| `│   │   │   ├── enabled`               | `boolean`      | `false`                                          |
| `│   │   │   ├── delay`                 | `TimeSpan`     | none, requires `backoff.max-delay`               |
| `│   │   │   ├── max-delay`             | `TimeSpan`     | none, requires `backoff.delay`                   |
| `│   │   │   └── delay-factor`          | `double`       | `2.0`                                            |
| `│   │   ├── max-retries`               | `int`          | none                                             |
| `│   │   ├── max-duration`              | `TimeSpan`     | `5 seconds`                                      |
| `│   │   ├── jitter-factor`             | `double`       | none, mutually exclusive to `jitter`             |
| `│   │   └── jitter`                    | `TimeSpan`     | none, mutually exclusive to `jitter-factor`      |
| `│   ├── soap`                          |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   └── protocol`                  | `String`       | `1.1` (possible other value: `1.2`)              |
| `│   ├── stack-trace-preservation`      |                |                                                  |
| `│   │   └── enabled`                   | `boolean`      | `true`                                           |
| `│   ├── threads`                       |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `true`                                           |
| `│   │   ├── min-size`                  | `int`          | `1`                                              |
| `│   │   ├── max-size`                  | `int`          | same as `connections.max-total`                  |
| `│   │   ├── keep-alive`                | `TimeSpan`     | `1 minute`                                       |
| `│   │   └── queue-size`                | `int`          | `0` (no queue)                                   |
| `│   ├── timeouts`                      |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   └── global`                    | `TimeSpan`     | none                                             |
| `│   ├── tracing`                       |                |                                                  |
| `│   │   ├── enabled`                   | `boolean`      | `false`                                          |
| `│   │   ├── tags`                      | `Map`          | none                                             |
| `│   │   └── propagate-flow-id`         | `boolean`      | `false`                                          |
| `│   ├── transient-fault-detection`     |                |                                                  |
| `│   │   └── enabled`                   | `boolean`      | `false`                                          |
| `│   └── url-resolution`                | `String`       | `rfc`                                            |
| `└── clients`                           |                |                                                  |
| `    └── <id>`                          | `String`       |                                                  |
| `        ├── backup-request`            |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   └── delay`                 | `TimeSpan`     | see `defaults`                                   |
| `        ├── base-url`                  | `URI`          | none                                             |
| `        ├── caching`                   |                | see `defaults`                                   |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   ├── shared`                | `boolean`      | see `defaults`                                   |
| `        │   ├── directory`             | `String`       | see `defaults`                                   |
| `        │   ├── max-object-size`       | `int`          | see `defaults`                                   |
| `        │   ├── max-cache-entries`     | `int`          | see `defaults`                                   |
| `        │   └── heuristic`             |                |                                                  |
| `        │       ├── enabled`           | `boolean`      | see `defaults`                                   |
| `        │       ├── coefficient`       | `double`       | see `defaults`                                   |
| `        │       └── default-life-time` | `TimeSpan`     | see `defaults`                                   |
| `        ├── certificate-pinning`       |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   └── keystore`              |                |                                                  |
| `        │       ├── path`              | `Path`         | see `defaults`                                   |
| `        │       └── password`          | `String`       | see `defaults`                                   |
| `        ├── chaos`                     |                |                                                  |
| `        │   ├── latency`               |                |                                                  |
| `        │   │   ├── enabled`           | `boolean`      | see `defaults`                                   |
| `        │   │   ├── probability`       | `double`       | see `defaults`                                   |
| `        │   │   └── delay`             | `TimeSpan`     | see `defaults`                                   |
| `        │   ├── exceptions`            |                |                                                  |
| `        │   │   ├── enabled`           | `boolean`      | see `defaults`                                   |
| `        │   │   └── probability`       | `double`       | see `defaults`                                   |
| `        │   └── error-responses`       |                |                                                  |
| `        │       ├── enabled`           | `boolean`      | see `defaults`                                   |
| `        │       ├── probability`       | `double`       | see `defaults`                                   |
| `        │       └── status-codes`      | `int[]`        | see `defaults`                                   |
| `        ├── circuit-breaker`           |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   ├── failure-threshold`     | `Ratio`        | see `defaults`                                   |
| `        │   ├── delay`                 | `TimeSpan`     | see `defaults`                                   |
| `        │   └── success-threshold`     | `Ratio`        | see `defaults`                                   |
| `        ├── connections`               |                |                                                  |
| `        │   ├── lease-request-timeout` | `TimeSpan`     | see `defaults`                                   |
| `        │   ├── connect-timeout`       | `TimeSpan`     | see `defaults`                                   |
| `        │   ├── socket-timeout`        | `TimeSpan`     | see `defaults`                                   |
| `        │   ├── time-to-live`          | `TimeSpan`     | see `defaults`                                   |
| `        │   ├── max-per-route`         | `int`          | see `defaults`                                   |
| `        │   └── max-total`             | `int`          | see `defaults`                                   |
| `        ├── logging`                   |                |                                                  |
| `        │   └── enabled`               | `boolean`      | see `defaults`                                   |
| `        ├── metrics`                   |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   └── tags`                  | `Map         ` | see `defaults`                                   |
| `        ├── auth`                      |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   └── credentials-directory` | `Path`         | see `defaults`                                   |
| `        ├── request-compression`       |                |                                                  |
| `        │   └── enabled`               | `boolean`      | see `defaults`                                   |
| `        ├── retry`                     |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   ├── fixed-delay`           | `TimeSpan`     | see `defaults`                                   |
| `        │   ├── backoff`               |                |                                                  |
| `        │   │   ├── enabled`           | `boolean`      | see `defaults`                                   |
| `        │   │   ├── delay`             | `TimeSpan`     | see `defaults`                                   |
| `        │   │   ├── max-delay`         | `TimeSpan`     | see `defaults`                                   |
| `        │   │   └── delay-factor`      | `double`       | see `defaults`                                   |
| `        │   ├── max-retries`           | `int`          | see `defaults`                                   |
| `        │   ├── max-duration`          | `TimeSpan`     | see `defaults`                                   |
| `        │   ├── jitter-factor`         | `double`       | see `defaults`                                   |
| `        │   └── jitter`                | `TimeSpan`     | see `defaults`                                   |
| `        ├── soap`                      |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   └── protocol`              | `String`       | see `defaults`                                   |
| `        ├── stack-trace-preservation`  |                |                                                  |
| `        │   └── enabled`               | `boolean`      | see `defaults`                                   |
| `        ├── threads`                   |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   ├── min-size`              | `int`          | see `defaults`                                   |
| `        │   ├── max-size`              | `int`          | see `defaults`                                   |
| `        │   ├── keep-alive`            | `TimeSpan`     | see `defaults`                                   |
| `        │   └── queue-size`            | `int`          | see `defaults`                                   |
| `        ├── timeouts`                  |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   └── global`                | `TimeSpan`     | see `defaults`                                   |
| `        ├── tracing`                   |                |                                                  |
| `        │   ├── enabled`               | `boolean`      | see `defaults`                                   |
| `        │   ├── tags`                  | `Map`          | see `defaults`                                   |
| `        │   └── propagate-flow-id`     | `boolean`      | see `defaults`                                   |
| `        ├── transient-fault-detection` |                |                                                  |
| `        │   └── enabled`               | `boolean`      | see `defaults`                                   |
| `        └── url-resolution`            | `String`       | see `defaults`                                   |

**Beware** that starting with Spring Boot 1.5.x the property resolution for environment variables changes and
properties like `REST_CLIENTS_EXAMPLE_BASEURL` no longer work. As an alternative applications can use the 
[`SPRING_APPLICATION_JSON`](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html):

```bash
export SPRING_APPLICATION_JSON='{
   "riptide.clients.example.base-url": ".."
}'
```

## Usage

After configuring your clients, as shown in the last section, you can now easily inject them:

```java
@Autowired
@Qualifier("example")
private Http example;
```

All beans that are created for each client use the *Client ID*, in this case `example`, as their qualifier.

Besides `Http`, you can also alternatively inject any of the following types per client directly:
- `RestOperations`
- `AsyncRestOperations`
- `ClientHttpRequestFactory`
- `HttpClient`
- `ClientHttpMessageConverters`

### Certificate Pinning

A client can be configured to only connect to trusted hosts (see
[Certificate Pinning](https://www.owasp.org/index.php/Certificate_and_Public_Key_Pinning)) by configuring the `certificate-pinning` key. Use
`keystore.path` to refer to a *JKS*  keystore on the classpath/filesystem and (optionally) specify the passphrase via `keystore.password`.

You can generate a keystore using the [JDK's keytool](http://docs.oracle.com/javase/7/docs/technotes/tools/#security):

```bash
./keytool -importcert -file some-cert.crt -keystore my.keystore -alias "<some-alias>"
```

### Customization

For every client that is defined in your configuration the following beans will be created and wired.

**Legend**
- *green*: managed beans
- *blue*: optionally managed beans
- *yellow*: managed singleton beans, i.e. shared across clients
- *red*: mandatory dependency
- *grey*: optional dependency

#### Bean Graph

[![Client Dependency Graph](../docs/graph.png)](../docs/graph.png)

#### Plugins

[![Client Dependency Graph](../docs/plugins.png)](../docs/plugins.png)

#### Converters

[![Client Dependency Graph](../docs/converters.png)](../docs/converters.png)

#### Interceptors

[![Client Dependency Graph](../docs/interceptors.png)](../docs/interceptors.png)

Every single bean in the graph can optionally be replaced by your own, custom version of it. Beans can only be
overridden by name, **not** by type. As an example, the following code would add XML support to the `example` client:

```java
@Bean
@Qualifier("example")
public ClientHttpMessageConverters exampleHttpMessageConverters() {
    return new ClientHttpMessageConverters(singletonList(new Jaxb2RootElementHttpMessageConverter()));
}
```

The following table shows all beans with their respective name (for the `example` client) and type:

| Bean Name                         | Bean Type                                     |
|-----------------------------------|-----------------------------------------------|
| `exampleHttp`                     | `Http`                                        |
| `exampleClientHttpRequestFactory` | `ClientHttpRequestFactory`                    |
| `exampleHttpMessageConverters`    | `ClientHttpMessageConverters`                 |
| `exampleHttpClient`               | `HttpClient`                                  |
| `exampleExecutorService`          | `ExecutorService`                             |
| `exampleFailsafePlugin`           | `FailsafePlugin`                              |
| `exampleMicrometerPlugin`         | `MicrometerPlugin`                            |
| `exampleOriginalStackTracePlugin` | `OriginalStackTracePlugin`                    |
| `examplePlugin`                   | `Plugin` (optional, additional custom plugin) |
| `exampleScheduledExecutorService` | `ScheduledExecutorService`                    |
| `exampleRetryPolicy`              | `RetryPolicy`                                 |
| `exampleCircuitBreaker`           | `CircuitBreaker`                              |
| `exampleRetryListener`            | `RetryListener`                               |
| `exampleFaultClassifier`          | `FaultClassifier`                             |
| `exampleCircuitBreakerListener`   | `CircuitBreakerListener`                      |
| `exampleAuthorizationProvider`    | `AuthorizationProvider`                       |

If you override a bean then all of its dependencies (see the [graph](#customization)), will **not** be registered,
unless required by some other bean.

In case you need more than one custom plugin, please use `Plugin.composite(Plugin...)`.

### Testing

Similar to Spring Boot's [`@RestClientTest`](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/autoconfigure/web/client/RestClientTest.html),
`@RiptideClientTest` is provided. This annotation allows for convenient testing of Riptide `Http` clients.

```java
@Component
public class GatewayService {

    @Autowired
    @Qualifier("example")
    private Http http;

    void remoteCall() {
        http.get("/bar").dispatch(status(), on(OK).call(pass())).join();
    }
}

@RunWith(SpringRunner.class)
@RiptideClientTest(GatewayService.class)
final class RiptideTest {

    @Autowired
    private GatewayService client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    public void shouldAutowireMockedHttp()  {
        server.expect(requestTo("https://example.com/bar")).andRespond(withSuccess());
        client.remoteCall();
        server.verify();
    }
}
```

**Beware** that all components of a client below and including `ClientHttpRequestFactory` are replaced by mocks. In addition, if enabled the `AuthorizationProvider` will default to a mock implementation for tests.

## Getting Help

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](../../issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](CONTRIBUTING.md).

## Alternatives

In case you don't want to use this Spring Boot Starter you always have the possibility to wire everything up by hand.
Feel free to take a look at [this example](src/test/java/org/zalando/riptide/spring/ManualConfiguration.java).
