package org.zalando.riptide.autoconfigure;

import com.google.gag.annotation.remark.*;
import lombok.extern.slf4j.*;
import org.apache.http.*;
import org.apache.http.client.cache.*;
import org.apache.http.client.config.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.client.cache.*;
import org.apache.http.impl.conn.*;
import org.apache.http.ssl.SSLContexts;
import org.zalando.riptide.autoconfigure.RiptideProperties.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.CertificatePinning.*;

import javax.annotation.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static java.lang.String.*;
import static java.util.concurrent.TimeUnit.*;

@SuppressWarnings("unused")
@Slf4j
final class HttpClientFactory {

    private HttpClientFactory() {

    }

    public static HttpClientConnectionManager createHttpClientConnectionManager(final Client client)
            throws GeneralSecurityException, IOException {

        final Connections connections = client.getConnections();

        final PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", new SSLConnectionSocketFactory(createSSLContext(client)))
                        .build(),
                null, // connection factory
                null, // scheme port resolver
                null, // dns resolver
                connections.getTimeToLive().getAmount(),
                connections.getTimeToLive().getUnit());

        manager.setMaxTotal(connections.getMaxTotal());
        manager.setDefaultMaxPerRoute(connections.getMaxPerRoute());

        return manager;
    }

    public static CloseableHttpClient createHttpClient(final Client client,
            final List<HttpRequestInterceptor> firstRequestInterceptors,
            final HttpClientConnectionManager connectionManager,
            @Nullable final HttpClientCustomizer customizer,
            @Nullable final Object cacheStorage) {

        final Caching caching = client.getCaching();
        final HttpClientBuilder builder = caching.getEnabled() ?
                configureCaching(caching, cacheStorage) :
                HttpClientBuilder.create();

        final RequestConfig.Builder config = RequestConfig.custom();

        firstRequestInterceptors.forEach(builder::addInterceptorFirst);

        final Connections connections = client.getConnections();
        config.setConnectTimeout((int) connections.getConnectTimeout().to(MILLISECONDS));
        config.setSocketTimeout((int) connections.getSocketTimeout().to(MILLISECONDS));

        builder.setConnectionManager(connectionManager);
        builder.setDefaultRequestConfig(config.build());
        builder.disableAutomaticRetries();

        Optional.ofNullable(customizer).ifPresent(customize(builder));

        return builder.build();
    }

    private static HttpClientBuilder configureCaching(final Caching caching,
            @Nullable final Object cacheStorage) {
        final Heuristic heuristic = caching.getHeuristic();

        final CacheConfig.Builder config = CacheConfig.custom()
                .setSharedCache(caching.getShared())
                .setMaxObjectSize(caching.getMaxObjectSize())
                .setMaxCacheEntries(caching.getMaxCacheEntries());

        if (heuristic.getEnabled()) {
            config.setHeuristicCachingEnabled(true);
            config.setHeuristicCoefficient(heuristic.getCoefficient());
            config.setHeuristicDefaultLifetime(heuristic.getDefaultLifeTime().to(TimeUnit.SECONDS));
        }

        @Hack("return cast tricks classloader in case of missing httpclient-cache")
        CachingHttpClientBuilder builder = CachingHttpClients.custom()
                                                             .setCacheConfig(config.build())
                                                             .setHttpCacheStorage((HttpCacheStorage) cacheStorage)
                                                             .setCacheDir(Optional.ofNullable(caching.getDirectory())
                                                                                  .map(Path::toFile)
                                                                                  .orElse(null));
        return HttpClientBuilder.class.cast(builder);
    }

    private static SSLContext createSSLContext(final Client client) throws GeneralSecurityException, IOException {
        final CertificatePinning pinning = client.getCertificatePinning();

        if (pinning.getEnabled()) {
            final Keystore keystore = pinning.getKeystore();
            final String path = keystore.getPath();
            final String password = keystore.getPassword();

            final URL resource = HttpClientFactory.class.getClassLoader().getResource(path);

            if (resource == null) {
                throw new FileNotFoundException(format("Keystore [%s] not found.", path));
            }

            try {
                return SSLContexts.custom()
                        .loadTrustMaterial(resource, password == null ? null : password.toCharArray())
                        .build();
            } catch (final Exception e) {
                log.error("Error loading keystore [{}]:", path,
                        e); // log full exception, bean initialization code swallows it
                throw e;
            }
        }

        return SSLContexts.createDefault();
    }

    private static Consumer<HttpClientCustomizer> customize(final HttpClientBuilder builder) {
        return customizer -> customizer.customize(builder);
    }

}
