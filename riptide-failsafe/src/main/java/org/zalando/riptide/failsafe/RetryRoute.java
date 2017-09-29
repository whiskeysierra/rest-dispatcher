package org.zalando.riptide.failsafe;

import org.zalando.riptide.Route;

public final class RetryRoute {

    RetryRoute() {
        // package private so we can trick code coverage
    }

    public static Route retry() {
        return Route.call(response -> {
            throw new RetryException(response);
        });
    }
}
