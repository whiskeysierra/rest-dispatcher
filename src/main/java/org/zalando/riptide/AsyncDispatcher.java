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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.List;

import static org.zalando.riptide.AsyncRest.handle;
import static org.zalando.riptide.Binding.route;

public final class AsyncDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncRest.class);

    private final List<HttpMessageConverter<?>> converters;
    private final ListenableFuture<ClientHttpResponse> future;
    private final Router router;

    AsyncDispatcher(final List<HttpMessageConverter<?>> converters, final ListenableFuture<ClientHttpResponse> future, 
            final Router router) {
        this.converters = converters;
        this.future = future;
        this.router = router;
    }

    @SafeVarargs
    public final <A> void dispatch(final Selector<A> selector, final Binding<A>... bindings) {
        dispatch(selector, route(bindings), handle(throwable ->
                LOG.error("Failed to dispatch asynchronously", throwable)));
    }

    public final <A> void dispatch(final Selector<A> selector, final List<Binding<A>> bindings,
            final FailureCallback callback) {
        final SuccessCallback<ClientHttpResponse> success = response ->
                router.route(response, converters, selector, bindings);

        final FailureCallback failure = exception -> {
            try {
                throw exception;
            } catch (Throwable throwable) {
                callback.onFailure(throwable);
            }
        };

        future.addCallback(success, failure);
    }

}
