package org.zalando.riptide.problem;

import org.springframework.http.MediaType;
import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.problem.Exceptional;
import org.zalando.problem.Problem;
import org.zalando.riptide.Navigators;
import org.zalando.riptide.Route;

import static org.springframework.http.MediaType.parseMediaType;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Route.named;
import static org.zalando.riptide.RoutingTree.dispatch;

public final class ProblemRoute {

    private static final MediaType PROBLEM = parseMediaType("application/problem+json");

    /**
     * @see <a href="http://zalando.github.io/restful-api-guidelines/common-data-objects/CommonDataObjects.html#must-use-problem-json">Zalando RESTful API Guidelines</a>
     */
    private static final MediaType X_DOT_PROBLEM = parseMediaType("application/x.problem+json");

    /**
     * Alternative spelling for {@link #X_DOT_PROBLEM}.
     */
    private static final MediaType X_DASH_PROBLEM = parseMediaType("application/x-problem+json");

    private static final Route PROPAGATE = problemHandling(named("Propagate", Exceptional::propagate));

    ProblemRoute() {
        // package private so we can trick code coverage
    }

    /**
     * Produces a {@link Route route} that dispatches on the {@link Navigators#contentType() content type} and
     * recognises {@code application/problem+json} as well as {@code application/x-problem+json} and
     * {@code application/x.problem+json} as {@link Problem problems} and {@link Exceptional#propagate() propagates}
     * them.
     *
     * @see #problemHandling(ThrowingConsumer)
     * @see Exceptional#propagate()
     * @return static route for handling problems by propagating them as exceptions
     */
    public static Route problemHandling() {
        return PROPAGATE;
    }

    /**
     * Produces a {@link Route route} that dispatches on the {@link Navigators#contentType() content type} and
     * recognises {@code application/problem+json} as well as {@code application/x-problem+json} and
     * {@code application/x.problem+json} as {@link Problem problems} and handles them given the supplied consumer.
     *
     * @param consumer the exception handler
     * @return a route for handling problems dynamically
     */
    public static Route problemHandling(final ThrowingConsumer<Exceptional, ? extends Exception> consumer) {
        final Route route = Route.call(Exceptional.class, consumer);
        
        return dispatch(contentType(),
                on(PROBLEM).call(route),
                on(X_DOT_PROBLEM).call(route),
                on(X_DASH_PROBLEM).call(route));
    }

}
