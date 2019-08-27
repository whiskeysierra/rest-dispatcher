package org.zalando.riptide;

import org.springframework.http.client.*;

import javax.annotation.*;
import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

final class DefaultRoutingTree<A> implements RoutingTree<A> {

    private final Navigator<A> navigator;
    private final Map<A, Route> routes;
    private final Route wildcard;

    DefaultRoutingTree(final Navigator<A> navigator, final List<Binding<A>> bindings) {
        this(navigator, map(bindings));
    }

    private DefaultRoutingTree(final Navigator<A> navigator, final Map<A, Route> routes) {
        this(navigator, unmodifiableMap(routes), routes.remove(null));
    }

    private DefaultRoutingTree(final Navigator<A> navigator, final Map<A, Route> routes, @Nullable final Route wildcard) {
        this.navigator = navigator;
        this.routes = routes;
        this.wildcard = wildcard;
    }

    private static <A> Map<A, Route> map(final List<Binding<A>> bindings) {
        return bindings.stream()
                .collect(toMap(Binding::getAttribute, Binding::getRoute, (u, v) -> {
                    throw new IllegalArgumentException(String.format("Duplicate key %s", u));
                }, LinkedHashMap::new));
    }

    @Override
    public Navigator<A> getNavigator() {
        return navigator;
    }

    @Override
    public Set<A> keySet() {
        return routes.keySet();
    }

    @Override
    public Optional<Route> get(final A attribute) {
        return Optional.ofNullable(routes.get(attribute));
    }

    @Override
    public Optional<Route> getWildcard() {
        return Optional.ofNullable(wildcard);
    }

    @Override
    public void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception {
        final Optional<Route> route = navigator.navigate(response, this);

        if (route.isPresent()) {
            try {
                route.get().execute(response, reader);
            } catch (final NoWildcardException e) {
                executeWildcard(response, reader);
            }
        } else {
            executeWildcard(response, reader);
        }
    }

    private void executeWildcard(final ClientHttpResponse response, final MessageReader reader) throws Exception {
        if (wildcard == null) {
            throw new NoWildcardException();
        }

        wildcard.execute(response, reader);
    }

}
