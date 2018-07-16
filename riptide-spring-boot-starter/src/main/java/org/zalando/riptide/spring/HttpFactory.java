package org.zalando.riptide.spring;

import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.UrlResolution;

import java.util.List;

final class HttpFactory {

    private HttpFactory() {

    }

    @SuppressWarnings("unused")
    public static Http create(
            final String baseUrl,
            final UrlResolution urlResolution,
            final AsyncClientHttpRequestFactory requestFactory,
            final List<HttpMessageConverter<?>> converters,
            final List<Plugin> plugins) {

        return Http.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .urlResolution(urlResolution)
                .converters(converters)
                .plugins(plugins)
                .build();
    }

}
