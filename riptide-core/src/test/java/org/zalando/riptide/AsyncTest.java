package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.concurrent.FailureCallback;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Route.pass;

public final class AsyncTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("http://localhost");

    private final Rest unit;
    private final MockRestServiceServer server;

    public AsyncTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
    }

    @Test
    public void shouldCall() throws Exception {
        server.expect(requestTo(url)).andRespond(withSuccess());

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ClientHttpResponse> verifier = mock(ThrowingConsumer.class);

        unit.get(url).dispatch(series(),
                on(SUCCESSFUL).call(verifier)).get();

        verify(verifier).accept(any());
    }

    @Test
    public void shouldExpand() throws Exception {
        server.expect(requestTo(URI.create("http://localhost/123"))).andRespond(withSuccess());

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ClientHttpResponse> verifier = mock(ThrowingConsumer.class);

        unit.get("http://localhost/{id}", 123)
            .dispatch(series(),
                on(SUCCESSFUL).call(verifier)).get();

        verify(verifier).accept(any());
    }

    @Test
    public void shouldCallWithoutParameters() throws Exception {
        server.expect(requestTo(url)).andRespond(withSuccess());

        final ThrowingRunnable verifier = mock(ThrowingRunnable.class);

        unit.get(url).dispatch(series(),
                on(SUCCESSFUL).call(verifier)).get();

        verify(verifier).run();
    }


    @Test
    public void shouldCallWithHeaders() throws Exception {
        server.expect(requestTo(url)).andRespond(withSuccess());

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ClientHttpResponse> verifier = mock(ThrowingConsumer.class);

        unit.get(url).headers(new HttpHeaders()).dispatch(series(),
                on(SUCCESSFUL).call(verifier)).get();

        verify(verifier).accept(any());
    }

    @Test
    public void shouldCallWithBody() throws Exception {
        server.expect(requestTo(url)).andRespond(withSuccess());

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ClientHttpResponse> verifier = mock(ThrowingConsumer.class);

        unit.get(url).body("test").dispatch(series(),
                on(SUCCESSFUL).call(verifier)).get();

        verify(verifier).accept(any());
    }

    @Test
    public void shouldCallWithHeadersAndBody() throws Exception {
        server.expect(requestTo(url)).andRespond(withSuccess());

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ClientHttpResponse> verifier = mock(ThrowingConsumer.class);

        unit.get(url).headers(new HttpHeaders()).body("test").dispatch(series(),
                on(SUCCESSFUL).call(verifier)).get();

        verify(verifier).accept(any());
    }

    @Test
    public void shouldCapture() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        unit.get(url)
                .dispatch(status(),
                        on(OK).call(response -> {
                            assertThat(response.getStatusCode(), is(OK));
                            assertThat(response.getHeaders().getContentType(), is(APPLICATION_JSON));
                        }))
                .get(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldIgnoreException() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url)).andRespond(withSuccess());

        unit.get(url).dispatch(series(),
                on(CLIENT_ERROR).call(pass()));
    }

    @Test
    public void shouldHandleExceptionWithGet() throws InterruptedException, ExecutionException, IOException {
        server.expect(requestTo(url)).andRespond(withSuccess());

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(NoRouteException.class));

        unit.get(url).dispatch(series(),
                on(CLIENT_ERROR).call(pass()))
                .get();
    }

    @Test
    public void shouldHandleNoRouteExceptionWithCallback() throws IOException {
        server.expect(requestTo(url)).andRespond(withSuccess());

        final FailureCallback callback = mock(FailureCallback.class);

        unit.get(url).dispatch(series(),
                on(CLIENT_ERROR).call(pass()))
                .addCallback(result -> {}, callback);

        verify(callback).onFailure(argThat(is(instanceOf(NoRouteException.class))));
    }

    @Test
    public void shouldIgnoreSuccessWhenHandlingExceptionWithCallback() throws IOException {
        server.expect(requestTo(url)).andRespond(withSuccess());

        unit.get(url).dispatch(series(),
                on(SUCCESSFUL).call(pass()))
                .addCallback(result -> {}, (mock(FailureCallback.class)));
    }

}
