package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.Timeout;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.conn.HttpClientConnectionManager;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.RestOperations;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.auth.AuthorizationPlugin;
import org.zalando.riptide.auth.AuthorizationProvider;
import org.zalando.riptide.auth.PlatformCredentialsAuthorizationProvider;
import org.zalando.riptide.autoconfigure.RiptideProperties.Chaos;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import org.zalando.riptide.chaos.ChaosPlugin;
import org.zalando.riptide.chaos.ErrorResponseInjection;
import org.zalando.riptide.chaos.ExceptionInjection;
import org.zalando.riptide.chaos.LatencyInjection;
import org.zalando.riptide.chaos.Probability;
import org.zalando.riptide.compatibility.AsyncHttpOperations;
import org.zalando.riptide.compatibility.HttpOperations;
import org.zalando.riptide.compression.RequestCompressionPlugin;
import org.zalando.riptide.failsafe.BackupRequest;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RequestPolicy;
import org.zalando.riptide.failsafe.TaskDecorator;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.httpclient.metrics.HttpConnectionPoolMetrics;
import org.zalando.riptide.logbook.LogbookPlugin;
import org.zalando.riptide.micrometer.MicrometerPlugin;
import org.zalando.riptide.micrometer.ThreadPoolMetrics;
import org.zalando.riptide.micrometer.tag.RetryTagGenerator;
import org.zalando.riptide.opentracing.OpenTracingPlugin;
import org.zalando.riptide.opentracing.TracedTaskDecorator;
import org.zalando.riptide.opentracing.span.SpanDecorator;
import org.zalando.riptide.soap.SOAPFaultHttpMessageConverter;
import org.zalando.riptide.soap.SOAPHttpMessageConverter;
import org.zalando.riptide.stream.Streams;

import javax.xml.soap.SOAPConstants;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.zalando.riptide.autoconfigure.Dependencies.ifPresent;
import static org.zalando.riptide.autoconfigure.Name.name;
import static org.zalando.riptide.autoconfigure.Registry.list;
import static org.zalando.riptide.autoconfigure.Registry.ref;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Auth;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Chaos.ErrorResponses;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Chaos.Exceptions;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Chaos.Latency;
import static org.zalando.riptide.autoconfigure.ValueConstants.LOGBOOK_REF;
import static org.zalando.riptide.autoconfigure.ValueConstants.METER_REGISTRY_REF;
import static org.zalando.riptide.autoconfigure.ValueConstants.TRACER_REF;

@Slf4j
@AllArgsConstructor
final class DefaultRiptideRegistrar implements RiptideRegistrar {

    private final Registry registry;
    private final RiptideProperties properties;

    @Override
    public void register() {
        properties.getClients().forEach((id, client) -> {
            registerHttp(id, client);
            registerHttpOperations(id, client);
            registerAsyncHttpOperations(id, client);
        });
    }

    private String registerHttp(final String id, final Client client) {
        return registry.registerIfAbsent(id, Http.class, () -> {
            log.debug("Client [{}]: Registering Http", id);

            return genericBeanDefinition(HttpFactory.class)
                    .setFactoryMethod("create")
                    .addConstructorArgValue(createExecutor(id, client))
                    .addConstructorArgReference(registerClientHttpRequestFactory(id, client))
                    .addConstructorArgValue(client.getBaseUrl())
                    .addConstructorArgValue(client.getUrlResolution())
                    .addConstructorArgValue(registerHttpMessageConverters(id, client))
                    .addConstructorArgValue(registerPlugins(id, client));
        });
    }

    private void registerHttpOperations(final String id, final Client client) {
        registry.registerIfAbsent(id, RestOperations.class, () ->
                genericBeanDefinition(HttpOperations.class)
                        .addConstructorArgReference(registerHttp(id, client)));
    }

    private void registerAsyncHttpOperations(final String id, final Client client) {
        registry.registerIfAbsent(id, AsyncRestOperations.class, () ->
                genericBeanDefinition(AsyncHttpOperations.class)
                        .addConstructorArgReference(registerHttp(id, client)));
    }

    private String registerClientHttpRequestFactory(final String id, final Client client) {
        return registry.registerIfAbsent(id, ClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering RestAsyncClientHttpRequestFactory", id);
            return genericBeanDefinition(ApacheClientHttpRequestFactory.class)
                    .addConstructorArgReference(registerHttpClient(id, client))
                    .addConstructorArgValue(client.getConnections().getMode());
        });
    }

    private Object createExecutor(final String id, final Client client) {
        if (client.getThreads().getEnabled()) {
            return ref(registerExecutor(id, client));
        }

        return null;
    }

    private String registerExecutor(final String id, final Client client) {
        final String executorId = registry.registerIfAbsent(id, ExecutorService.class, () ->
                genericBeanDefinition(ThreadPoolFactory.class)
                        .addConstructorArgValue(id)
                        .addConstructorArgValue(client.getThreads())
                        .setFactoryMethod("create")
                        .setDestroyMethodName("shutdown"));

        if (client.getMetrics().getEnabled()) {
            registry.registerIfAbsent(id, ThreadPoolMetrics.class, () ->
                    genericBeanDefinition(ThreadPoolMetrics.class)
                            .addConstructorArgReference(executorId)
                            .addConstructorArgValue("http.client.threads")
                            .addConstructorArgValue(ImmutableList.of(clientId(id))));
        }

        if (client.getTracing().getEnabled()) {
            return registry.registerIfAbsent(id, TracedExecutorService.class, () ->
                    genericBeanDefinition(TracedExecutorService.class)
                            .addConstructorArgReference(executorId)
                            .addConstructorArgValue(TRACER_REF));
        }

        return executorId;
    }

    private static final class HttpMessageConverters {


    }

    private BeanDefinition registerHttpMessageConverters(final String id, final Client client) {
        // we use the wrong type here since that's the easiest way to influence the name
        // we want exampleHttpMessageConverters, rather than exampleClientHttpMessageConverters

        final String convertersId = registry.registerIfAbsent(id, HttpMessageConverters.class, () -> {
            final List<Object> list = list();

            final String objectMapperId = findObjectMapper(id);

            if (client.getSoap().getEnabled()) {
                final Map<String, String> protocols = ImmutableMap.of(
                        "1.1", SOAPConstants.SOAP_1_1_PROTOCOL,
                        "1.2", SOAPConstants.SOAP_1_2_PROTOCOL
                );

                final String protocol = requireNonNull(protocols.get(client.getSoap().getProtocol()),
                        "Unsupported protocol: " + client.getSoap().getProtocol());

                log.debug("Client [{}]: Registering SOAPHttpMessageConverter", id);
                list.add(genericBeanDefinition(SOAPHttpMessageConverter.class)
                        .addConstructorArgValue(protocol)
                        .getBeanDefinition());

                log.debug("Client [{}]: Registering SOAPFaultHttpMessageConverter", id);
                list.add(genericBeanDefinition(SOAPFaultHttpMessageConverter.class)
                        .addConstructorArgValue(protocol)
                        .getBeanDefinition());
            }

            log.debug("Client [{}]: Registering MappingJackson2HttpMessageConverter referencing [{}]", id,
                    objectMapperId);
            list.add(genericBeanDefinition(MappingJackson2HttpMessageConverter.class)
                    .addConstructorArgReference(objectMapperId)
                    .getBeanDefinition());

            ifPresent("org.zalando.riptide.stream.Streams", () -> {
                log.debug("Client [{}]: Registering StreamConverter referencing [{}]", id, objectMapperId);
                list.add(genericBeanDefinition(Streams.class)
                        .setFactoryMethod("streamConverter")
                        .addConstructorArgReference(objectMapperId)
                        .getBeanDefinition());
            });

            log.debug("Client [{}]: Registering StringHttpMessageConverter", id);
            list.add(genericBeanDefinition(StringHttpMessageConverter.class)
                    .addPropertyValue("writeAcceptCharset", false)
                    .getBeanDefinition());

            return genericBeanDefinition(ClientHttpMessageConverters.class)
                    .addConstructorArgValue(list);
        });

        return genericBeanDefinition()
                .setFactoryMethodOnBean("getConverters", convertersId)
                .getBeanDefinition();
    }

    private String findObjectMapper(final String id) {
        return registry.find(id, ObjectMapper.class).orElse("jacksonObjectMapper");
    }

    private List<BeanReference> registerPlugins(final String id, final Client client) {
        final Stream<Optional<String>> plugins = Stream.of(
                registerChaosPlugin(id, client),
                registerMicrometerPlugin(id, client),
                registerRequestCompressionPlugin(id, client),
                registerLogbookPlugin(id, client),
                registerOpenTracingPlugin(id, client),
                registerCircuitBreakerFailsafePlugin(id, client),
                registerRetryPolicyFailsafePlugin(id, client),
                registerAuthorizationPlugin(id, client),
                registerBackupRequestFailsafePlugin(id, client),
                registerTimeoutFailsafePlugin(id, client),
                registerOriginalStackTracePlugin(id, client),
                registerCustomPlugin(id));

        return plugins
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Registry::ref)
                .collect(toCollection(Registry::list));
    }

    private Optional<String> registerChaosPlugin(final String id, final Client client) {
        final Chaos chaos = client.getChaos();
        final Latency latency = chaos.getLatency();
        final Exceptions exceptions = chaos.getExceptions();
        final ErrorResponses errorResponses = chaos.getErrorResponses();

        final List<String> injections = new ArrayList<>();

        if (latency.getEnabled()) {
            injections.add(registry.registerIfAbsent(id, LatencyInjection.class, () ->
                    genericBeanDefinition(LatencyInjection.class)
                            .addConstructorArgReference(
                                    registry.registerIfAbsent(name(id, LatencyInjection.class, Probability.class),
                                            () -> genericBeanDefinition(Probability.class)
                                                    .setFactoryMethod("fixed")
                                                    .addConstructorArgValue(latency.getProbability())))
                            .addConstructorArgValue(Clock.systemUTC())
                            .addConstructorArgValue(latency.getDelay().toDuration())));
        }

        if (exceptions.getEnabled()) {
            injections.add(registry.registerIfAbsent(id, ExceptionInjection.class, () ->
                    genericBeanDefinition(ExceptionInjection.class)
                            .addConstructorArgReference(
                                    registry.registerIfAbsent(name(id, ExceptionInjection.class, Probability.class),
                                            () -> genericBeanDefinition(Probability.class)
                                                    .setFactoryMethod("fixed")
                                                    .addConstructorArgValue(exceptions.getProbability())))
                            .addConstructorArgValue(
                                    Collections.<Supplier<Exception>>singletonList(SocketTimeoutException::new))));
        }

        if (errorResponses.getEnabled()) {
            injections.add(registry.registerIfAbsent(id, ErrorResponseInjection.class, () ->
                    genericBeanDefinition(ErrorResponseInjection.class)
                            .addConstructorArgReference(
                                    registry.registerIfAbsent(name(id, ErrorResponseInjection.class, Probability.class),
                                            () -> genericBeanDefinition(Probability.class)
                                                    .setFactoryMethod("fixed")
                                                    .addConstructorArgValue(errorResponses.getProbability())))
                            .addConstructorArgValue(errorResponses.getStatusCodes().stream()
                                    .map(HttpStatus::valueOf)
                                    .collect(toList()))));
        }

        if (injections.isEmpty()) {
            return Optional.empty();
        }

        final String pluginId = registry.registerIfAbsent(id, ChaosPlugin.class, () ->
                genericBeanDefinition(ChaosPlugin.class)
                        .addConstructorArgValue(injections.stream()
                                .map(Registry::ref)
                                .collect(toCollection(Registry::list))));

        return Optional.of(pluginId);
    }

    private Optional<String> registerMicrometerPlugin(final String id, final Client client) {
        if (client.getMetrics().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, MicrometerPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, MicrometerPlugin.class.getSimpleName());
                return genericBeanDefinition(MicrometerPluginFactory.class)
                        .setFactoryMethod("create")
                        .addConstructorArgValue(METER_REGISTRY_REF)
                        .addConstructorArgValue(Tags.of(clientId(id))
                                .and(tags(client.getMetrics().getTags())))
                        .addConstructorArgValue(registerRetryTagGenerator(client));
            });

            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private static Iterable<Tag> tags(final Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(e -> Tag.of(e.getKey(), e.getValue()))
                .collect(toList());
    }

    private List<BeanMetadataElement> registerRetryTagGenerator(final Client client) {
        if (client.getRetry().getEnabled()) {
            return list(genericBeanDefinition(RetryTagGenerator.class)
                    .getBeanDefinition());
        } else {
            return list();
        }
    }

    private Optional<String> registerLogbookPlugin(final String id, final Client client) {
        if (client.getLogging().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, LogbookPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, LogbookPlugin.class.getSimpleName());
                return genericBeanDefinition(LogbookPlugin.class)
                        .addConstructorArgValue(LOGBOOK_REF);
            });

            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerRequestCompressionPlugin(final String id, final Client client) {
        if (client.getRequestCompression().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, RequestCompressionPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, RequestCompressionPlugin.class);
                return genericBeanDefinition(RequestCompressionPlugin.class);
            });

            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerOpenTracingPlugin(final String id, final Client client) {
        if (client.getTracing().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, OpenTracingPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, OpenTracingPlugin.class.getSimpleName());
                return genericBeanDefinition(OpenTracingPluginFactory.class)
                        .setFactoryMethod("create")
                        .addConstructorArgValue(TRACER_REF)
                        .addConstructorArgValue(client)
                        .addConstructorArgValue(registry.findRef(id, SpanDecorator.class).orElse(null));
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerCircuitBreakerFailsafePlugin(final String id, final Client client) {
        if (client.getCircuitBreaker().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(name(id, CircuitBreaker.class, FailsafePlugin.class),
                    () -> {
                        log.debug("Client [{}]: Registering [CircuitBreakerFailsafePlugin]", id);
                        return genericBeanDefinition(FailsafePluginFactory.class)
                                .setFactoryMethod("create")
                                .addConstructorArgValue(genericBeanDefinition(RequestPolicy.class)
                                        .setFactoryMethod("of")
                                        .addConstructorArgValue(registerCircuitBreaker(id, client))
                                        .getBeanDefinition())
                                .addConstructorArgValue(createTaskDecorator(id, client));
                    });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerRetryPolicyFailsafePlugin(final String id, final Client client) {
        if (client.getRetry().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(name(id, RetryPolicy.class, FailsafePlugin.class), () -> {
                log.debug("Client [{}]: Registering [RetryPolicyFailsafePlugin]", id);
                return genericBeanDefinition(FailsafePluginFactory.class)
                        .setFactoryMethod("createRetryFailsafePlugin")
                        .addConstructorArgValue(client)
                        .addConstructorArgValue(createTaskDecorator(id, client));
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerAuthorizationPlugin(final String id, final Client client) {
        if (client.getAuth().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, AuthorizationPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, AuthorizationPlugin.class.getSimpleName());
                return genericBeanDefinition(AuthorizationPlugin.class)
                        .addConstructorArgReference(registerAuthorizationProvider(id, client.getAuth()));
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerBackupRequestFailsafePlugin(final String id, final Client client) {
        if (client.getBackupRequest().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(name(id, BackupRequest.class, FailsafePlugin.class),
                    () -> {
                        log.debug("Client [{}]: Registering [BackupRequestFailsafePlugin]", id);
                        return genericBeanDefinition(FailsafePluginFactory.class)
                                .setFactoryMethod("create")
                                .addConstructorArgValue(genericBeanDefinition(FailsafePluginFactory.class)
                                        .setFactoryMethod("createBackupRequest")
                                        .addConstructorArgValue(client)
                                        .getBeanDefinition())
                                .addConstructorArgValue(createTaskDecorator(id, client));
                    });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerTimeoutFailsafePlugin(final String id, final Client client) {
        if (client.getTimeouts().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(name(id, Timeout.class, FailsafePlugin.class), () -> {
                log.debug("Client [{}]: Registering [TimeoutFailsafePlugin]", id);
                final TimeSpan timeout = client.getTimeouts().getGlobal();
                return genericBeanDefinition(FailsafePluginFactory.class)
                        .setFactoryMethod("create")
                        .addConstructorArgValue(RequestPolicy.of(
                                Timeout.<ClientHttpResponse>of(timeout.toDuration())
                                        .withCancel(true)))
                        .addConstructorArgValue(createTaskDecorator(id, client));
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerOriginalStackTracePlugin(final String id, final Client client) {
        if (client.getStackTracePreservation().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, OriginalStackTracePlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, OriginalStackTracePlugin.class.getSimpleName());
                return genericBeanDefinition(OriginalStackTracePlugin.class);
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerCustomPlugin(final String id) {
        return registry.find(id, Plugin.class);
    }

    private Object createTaskDecorator(final String id, final Client client) {
        if (client.getTracing().getEnabled()) {
            return ref(registry.registerIfAbsent(id, TaskDecorator.class, () ->
                    genericBeanDefinition(TracedTaskDecorator.class)
                            .addConstructorArgValue(TRACER_REF)));
        }
        return TaskDecorator.identity();
    }

    private BeanMetadataElement registerCircuitBreaker(final String id, final Client client) {
        return ref(registry.registerIfAbsent(id, CircuitBreaker.class, () ->
                genericBeanDefinition(FailsafePluginFactory.class)
                        .setFactoryMethod("createCircuitBreaker")
                        .addConstructorArgValue(client)
                        .addConstructorArgReference(registerCircuitBreakerListener(id, client))));
    }

    private String registerCircuitBreakerListener(final String id, final Client client) {
        return registry.registerIfAbsent(id, CircuitBreakerListener.class, () -> {
            if (client.getMetrics().getEnabled()) {
                return genericBeanDefinition(MicrometerFailsafeFactory.class)
                        .setFactoryMethod("createCircuitBreakerListener")
                        .addConstructorArgValue(METER_REGISTRY_REF)
                        .addConstructorArgValue(ImmutableList.of(clientId(id), clientName(id, client)));
            } else {
                return genericBeanDefinition(MicrometerFailsafeFactory.class)
                        .setFactoryMethod("getDefaultCircuitBreakerListener");
            }
        });
    }

    private Tag clientId(final String id) {
        return Tag.of("client_id", id);
    }

    private Tag clientName(final String id, final Client client) {
        return Tag.of("clientName", getHost(client).orElse(id));
    }

    private Optional<String> getHost(final Client client) {
        return Optional.ofNullable(client.getBaseUrl())
                .map(URI::create).map(URI::getHost);
    }

    private String registerAuthorizationProvider(final String id, final Auth auth) {
        return registry.registerIfAbsent(id, AuthorizationProvider.class, () ->
                genericBeanDefinition(PlatformCredentialsAuthorizationProvider.class)
                        .addConstructorArgValue(auth.getCredentialsDirectory())
                        .addConstructorArgValue(id));
    }

    private String registerHttpClient(final String id, final Client client) {
        return registry.registerIfAbsent(id, HttpClient.class, () -> {
            log.debug("Client [{}]: Registering HttpClient", id);

            final String connectionManager = registry.registerIfAbsent(id, HttpClientConnectionManager.class, () ->
                    genericBeanDefinition(HttpClientFactory.class)
                            .setFactoryMethod("createHttpClientConnectionManager")
                            .addConstructorArgValue(client));

            if (client.getMetrics().getEnabled()) {
                registry.registerIfAbsent(id, HttpConnectionPoolMetrics.class, () ->
                        genericBeanDefinition(HttpConnectionPoolMetrics.class)
                                .addConstructorArgReference(connectionManager)
                                .addConstructorArgValue("http.client.connections")
                                .addConstructorArgValue(ImmutableList.of(clientId(id))));
            }

            return genericBeanDefinition(HttpClientFactory.class)
                    .setFactoryMethod("createHttpClient")
                    .addConstructorArgValue(client)
                    .addConstructorArgValue(configureFirstRequestInterceptors(id, client))
                    .addConstructorArgReference(connectionManager)
                    .addConstructorArgValue(registry.findRef(id, HttpClientCustomizer.class).orElse(null))
                    .addConstructorArgValue(findCacheStorageReference(id, client).orElse(null))
                    .setDestroyMethodName("close");
        });
    }

    private List<BeanMetadataElement> configureFirstRequestInterceptors(final String id, final Client client) {
        final List<BeanMetadataElement> interceptors = list();

        if (client.getTracing().getEnabled() && client.getTracing().getPropagateFlowId()) {
            log.debug("Client [{}]: Registering FlowHttpRequestInterceptor", id);
            interceptors.add(ref("flowHttpRequestInterceptor"));
        }

        return interceptors;
    }

    private Optional<BeanReference> findCacheStorageReference(final String id, final Client client) {
        if (client.getCaching().getEnabled()) {
            return registry.findRef(id, HttpCacheStorage.class);
        } else {
            return Optional.empty();
        }
    }

}
