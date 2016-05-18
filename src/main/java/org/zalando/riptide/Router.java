package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClientException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

final class Router {

    final <A, S> Capture route(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters,
            final Selector<A> selector, final Collection<Binding<A>> bindings) {

        @Nullable
        final Binding<A> wildcard = findWildcard(bindings);

        try {
            final Collection<Binding<A>> nonWildcardBindings = bindings.stream()
                    .filter(not(isWildcard())).collect(toList());
            final Binding<A> match = selector.select(response, nonWildcardBindings);

            if (match == null) {
                return routeToWildcardIfPossible(wildcard, response, converters);
            } else {
                try {
                    return match.execute(response, converters);
                } catch (final NoRouteException e) {
                    return bubbleUpToWildcardIfPossible(wildcard, response, converters, e);
                }
            }
        } catch (final IOException e) {
            throw new RestClientException("Unable to execute binding", e);
        }
    }

    @Nullable
    private <A> Binding<A> findWildcard(final Collection<Binding<A>> bindings) {
        final List<Binding<A>> list = bindings.stream()
                .filter(isWildcard())
                .collect(toList());

        switch (list.size()) {
            case 0:
                return null;
            case 1:
                return list.get(0);
            default:
                throw new IllegalArgumentException("Multiple wildcard entries");
        }
     }

    private <A> Predicate<Binding<A>> isWildcard() {
        return binding -> binding.getAttribute() == null;
    }

    private <T> Predicate<T> not(final Predicate<T> predicate) {
        return predicate.negate();
    }

    private <A> Capture bubbleUpToWildcardIfPossible(final @Nullable Binding<A> wildcard,
            final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters,
            final NoRouteException e) throws IOException {

        try {
            return routeToWildcardIfPossible(wildcard, response, converters);
        } catch (final NoRouteException ignored) {
            // propagating didn't work, preserve original exception
            throw e;
        }
    }

    private <A> Capture routeToWildcardIfPossible(final @Nullable Binding<A> wildcard,
            final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters) throws IOException {

        if (wildcard == null) {
            throw new NoRouteException(response);
        } else {
            return wildcard.execute(response, converters);
        }
    }

}
