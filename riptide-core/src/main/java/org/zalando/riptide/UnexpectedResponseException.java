package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;

/**
 * Thrown when no matching {@link Route route} was found during {@link Dispatcher#dispatch(RoutingTree) dispatch}.
 *
 * @see NoRoute#noRoute()
 */
@API(status = STABLE)
@SuppressWarnings("serial")
public final class UnexpectedResponseException extends HttpResponseException {

    @API(status = INTERNAL)
    public UnexpectedResponseException(final ClientHttpResponse response) throws IOException {
        super("Unable to dispatch response", response);
    }

}
