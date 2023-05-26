package org.zalando.riptide.logbook;

import okhttp3.mockwebserver.MockWebServer;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.Sink;
import org.zalando.logbook.Strategy;
import org.zalando.riptide.Http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.POST;
import static org.zalando.riptide.logbook.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.logbook.MockWebServerUtil.textMockResponse;

final class RiptideLogbookCompatibilityTest implements CompatibilityTest {

    @Override
    public Interaction test(final Strategy strategy) throws IOException {
        final MockWebServer server = new MockWebServer();

        try {
            final ExecutorService executor = newSingleThreadExecutor();

            try {
                final Sink sink = spy(new Sink() {
                    @Override
                    public void write(final Precorrelation precorrelation, final HttpRequest request) throws IOException {
                        request.getBody();
                    }

                    @Override
                    public void write(final Correlation correlation, final HttpRequest request,
                            final HttpResponse response) throws IOException {
                        response.getBody();
                    }

                    @Override
                    public void writeBoth(final Correlation correlation, final HttpRequest request,
                            final HttpResponse response) throws IOException {
                        request.getBody();
                        response.getBody();
                    }
                });

                final Logbook logbook = Logbook.builder()
                        .sink(sink)
                        .strategy(strategy)
                        .build();

                final Http http = Http.builder()
                        .executor(executor)
                        .requestFactory(new SimpleClientHttpRequestFactory())
                        .baseUrl(getBaseUrl(server))
                        .plugin(new LogbookPlugin(logbook))
                        .build();

                server.enqueue(textMockResponse("World!"));

                final ClientHttpResponse response = http.post("/greet")
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Hello?")
                        .call((resp, reader) -> {
                            // nothing to do
                        })
                        .join();

                assertThat(response.getStatusCode(), is(HttpStatus.OK));
                assertThat(response.getRawStatusCode(), is(200));
                assertThat(response.getStatusText(), is("OK"));
                assertThat(response.getHeaders(), hasKey("Content-Type"));
                assertThat(new String(toByteArray(response.getBody()), UTF_8), is("World!"));
                MockWebServerUtil.verify(server, 1, "/greet", POST.toString());

                return getInteraction(sink);
            } finally {
                executor.shutdown();
            }
        } finally {
            server.shutdown();
        }
    }

    private Interaction getInteraction(final Sink sink) throws IOException {
        return new Interaction(getRequest(sink), getResponse(sink));
    }

    private HttpRequest getRequest(final Sink sink) throws IOException {
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(sink).write(any(), captor.capture());
        return captor.getValue();
    }

    private HttpResponse getResponse(final Sink sink) throws IOException {
        final ArgumentCaptor<HttpResponse> captor = ArgumentCaptor.forClass(HttpResponse.class);
        verify(sink).write(any(), any(), captor.capture());
        return captor.getValue();
    }

}
