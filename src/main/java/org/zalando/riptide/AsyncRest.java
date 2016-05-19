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

import com.google.gag.annotation.remark.OhNoYouDidnt;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import java.net.URI;
import java.util.List;

public final class AsyncRest extends RestBase<AsyncRestTemplate, AsyncDispatcher> {

    private AsyncRest(final AsyncRestTemplate template) {
        super(template, template::getUriTemplateHandler);
    }

    @Override
    protected <T> AsyncDispatcher execute(final HttpMethod method, final URI url, final HttpEntity<T> entity) {
        final List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        final Callback<T> callback = new Callback<>(converters, entity);

        final ListenableFuture<ClientHttpResponse> future = template.execute(url, method,
                new AsyncRequestCallbackAdapter<>(callback), BufferingClientHttpResponse::buffer);

        return new AsyncDispatcher(converters, future, router);
    }

    public static AsyncRest create(final AsyncRestTemplate template) {
        return new AsyncRest(template);
    }

    // syntactic sugar
    @OhNoYouDidnt
    public static FailureCallback handle(final FailureCallback callback) {
        return callback;
    }
}
