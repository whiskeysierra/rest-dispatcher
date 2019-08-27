package org.zalando.riptide;

import org.apiguardian.api.*;
import org.springframework.http.*;

import javax.annotation.*;
import java.io.*;
import java.net.*;
import java.util.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public interface RequestArguments {

    interface Entity {

        void writeTo(HttpOutputMessage message) throws IOException;

        default boolean isEmpty() {
            return false;
        }

    }

    URI getBaseUrl();

    UrlResolution getUrlResolution();

    HttpMethod getMethod();

    String getUriTemplate();

    List<Object> getUriVariables();

    URI getUri();

    <T> Optional<T> getAttribute(Attribute<T> attribute);

    Map<String, List<String>> getQueryParams();

    URI getRequestUri();

    Map<String, List<String>> getHeaders();

    Object getBody();

    Entity getEntity();

    Route getRoute();

    RequestArguments withBaseUrl(@Nullable URI baseUrl);

    RequestArguments withUrlResolution(@Nullable UrlResolution resolution);

    RequestArguments withMethod(@Nullable HttpMethod method);

    RequestArguments withUriTemplate(@Nullable String uriTemplate);

    RequestArguments replaceUriVariables(List<Object> uriVariables);

    RequestArguments withUri(@Nullable URI uri);

    <T> RequestArguments withAttribute(Attribute<T> attribute, T value);

    RequestArguments withQueryParam(String name, String value);

    RequestArguments withQueryParams(Map<String, ? extends Collection<String>> queryParams);

    RequestArguments withoutQueryParam(String name);

    RequestArguments replaceQueryParams(Map<String, ? extends Collection<String>> queryParams);

    RequestArguments withHeader(String name, String value);

    RequestArguments withHeaders(Map<String, ? extends Collection<String>> headers);

    RequestArguments withoutHeader(String name);

    RequestArguments replaceHeaders(Map<String, ? extends Collection<String>> headers);

    RequestArguments withBody(@Nullable Object body);

    RequestArguments withEntity(@Nullable Entity entity);

    RequestArguments withRoute(Route route);

    static RequestArguments create() {
        return new DefaultRequestArguments();
    }

}
