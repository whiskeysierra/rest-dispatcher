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

import lombok.SneakyThrows;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class Binding<A> implements Executor {

    private final Optional<A> attribute;
    private final Executor executor;

    private Binding(final Optional<A> attribute, final Executor executor) {
        this.attribute = attribute;
        this.executor = executor;
    }

    Optional<A> getAttribute() {
        return attribute;
    }

    @Override
    @SneakyThrows(Exception.class)
    public Capture execute(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters) throws IOException {
        return executor.execute(response, converters);
    }

    static <A> Binding<A> create(final A attribute, final Executor executor) {
        return create(Optional.of(attribute), executor);
    }

    static <A> Binding<A> create(final Optional<A> attribute, final Executor executor) {
        return new Binding<>(attribute, executor);
    }

}
