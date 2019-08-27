package org.zalando.riptide;

import com.google.common.reflect.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.fauxpas.*;

import static org.apiguardian.api.API.Status.*;
import static org.zalando.fauxpas.TryWith.*;

/**
 *
 * @see RoutingTree
 */
@API(status = STABLE)
@FunctionalInterface
public interface Route {

    /**
     * Executes this route against the specific response. It's within the responsibility of a {@link Route route}
     * implementation to ensure the stream is consumed (optional) and properly closed. A fully consumed response stream
     * allows for connection reuse and is therefore highly encouraged. Making sure that every response is properly
     * closed ensures that stale connections are not exhausting the connection pool.
     *
     * Most {@link Route routes} will do both operations internally, but the responsibility can be handed over to the
     * caller of the {@link Route route} by unmarshalling the stream into a {@link java.io.Closeable} or
     * {@link AutoCloseable} bean.
     *
     * @param response the client response
     * @param reader a utility to unmarshall the response into Java beans
     * @throws Exception if anything goes wrong during route execution, primarily used for {@link java.io.IOException}
     */
    void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception;

    default Route merge(final Route other) {
        return other;
    }

    static Route call(final ThrowingRunnable<? extends Exception> runnable) {
        return (response, reader) ->
                tryWith(response, (ClientHttpResponse ignored) -> runnable.tryRun());
    }

    static Route call(final ThrowingConsumer<ClientHttpResponse, ? extends Exception> consumer) {
        return (response, reader) ->
                tryWith(response, consumer);
    }

    static <I> Route call(final Class<I> type, final ThrowingConsumer<I, ? extends Exception> consumer) {
        return call(TypeToken.of(type), consumer);
    }

    static <I> Route call(final TypeToken<I> type, final ThrowingConsumer<I, ? extends Exception> consumer) {
        return (response, reader) -> {

            final I body = reader.read(type, response);
            consumer.tryAccept(body);
        };
    }

}
