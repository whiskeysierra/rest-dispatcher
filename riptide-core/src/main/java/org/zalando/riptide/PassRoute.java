package org.zalando.riptide;

import com.google.common.io.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;

import java.io.*;

import static org.apiguardian.api.API.Status.*;
import static org.zalando.fauxpas.TryWith.*;

@API(status = STABLE)
public final class PassRoute implements Route {

    private static final Route PASS = new PassRoute();

    private PassRoute() {

    }

    @Override
    public void execute(final ClientHttpResponse response, final MessageReader reader) throws IOException {
        tryWith(response, this::exhaust);
    }

    private void exhaust(final ClientHttpResponse response) throws IOException {
        ByteStreams.exhaust(response.getBody());
    }

    /**
     * Produces a {@link Route} that ignores the response by actively discarding the response body.
     *
     * <strong>Beware</strong> that this behavior will block forever on potentially endless response bodies
     * like streams.
     *
     * @return a {@link Route route} that doesn't execute any meaningful logic
     */
    public static Route pass() {
        return PASS;
    }

}
