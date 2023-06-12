package org.zalando.riptide;

import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.zalando.riptide.Http.ConfigurationStage;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.currentThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.MockWebServerUtil.*;
import static org.zalando.riptide.Navigators.series;

final class ThreadAffinityTest {

    @Test
    void syncBlocking() {
        final ConfigurationStage stage = Http.builder()
                .executor(Runnable::run)
                .requestFactory(new SimpleClientHttpRequestFactory());

        test(stage, "main", "main", "main");
    }

    @Test
    void asyncBlocking() {
        final ConfigurationStage stage = Http.builder()
                .executor(Executors.newSingleThreadExecutor(threadFactory("process")))
                .requestFactory(new SimpleClientHttpRequestFactory());

        test(stage, "process", "process", "process");
    }

    @SneakyThrows
    void test(final ConfigurationStage stage, final String request, final String dispatch, final String callback) {
        final MockWebServer server = new MockWebServer();

        try {
            server.enqueue(emptyMockResponse());

            final Http http = stage
                    .baseUrl(getBaseUrl(server))
                    .build();

            final AtomicReference<Thread> requestThread = new AtomicReference<>();
            final AtomicReference<Thread> dispatchThread = new AtomicReference<>();
            final AtomicReference<Thread> callbackThread = new AtomicReference<>();

            http.get("/")
                    .body(message ->
                            requestThread.set(currentThread()))
                    .dispatch(series(),
                            on(SUCCESSFUL).call((response, reader) ->
                                    dispatchThread.set(currentThread())))
                    .whenComplete((response, exception) ->
                            callbackThread.set(currentThread()))
            .join();

            assertEquals(request, requestThread.get().getName(), "request thread");
            assertEquals(dispatch, callbackThread.get().getName(), "callback thread");
            assertEquals(callback, dispatchThread.get().getName(), "dispatch thread");
            verify(server, 1, "/");
        } finally {
            server.shutdown();
        }
    }

    private ThreadFactory threadFactory(final String name) {
        return runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setName(name);
            thread.setDaemon(true);
            return thread;
        };
    }

}
