package org.zalando.riptide.micrometer;

import io.micrometer.core.instrument.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import javax.annotation.*;
import java.io.*;
import java.util.*;

import static com.google.common.base.MoreObjects.*;
import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public final class DefaultTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> tags(final RequestArguments arguments, @Nullable final ClientHttpResponse response,
            @Nullable final Throwable throwable) {
        return Arrays.asList(
                Tag.of("method", method(arguments)),
                Tag.of("uri", uri(arguments)),
                Tag.of("status", status(response)),
                Tag.of("clientName", client(arguments)),
                Tag.of("exception", exception(throwable))
        );
    }

    private String method(final RequestArguments arguments) {
        return arguments.getMethod().name();
    }

    private String uri(final RequestArguments arguments) {
        return firstNonNull(arguments.getUriTemplate(), arguments.getRequestUri().getPath());
    }

    private String status(@Nullable final ClientHttpResponse response) {
        if (response == null) {
            return "CLIENT_ERROR";
        }

        try {
            return String.valueOf(response.getRawStatusCode());
        } catch (final IOException e) {
            return "IO_ERROR";
        }
    }

    private String client(final RequestArguments arguments) {
        return firstNonNull(arguments.getRequestUri().getHost(), "none");
    }

    private String exception(@Nullable final Throwable throwable) {
        return throwable == null ? "None" : throwable.getClass().getSimpleName();
    }

}
