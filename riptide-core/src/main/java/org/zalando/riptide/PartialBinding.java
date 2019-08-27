package org.zalando.riptide;

import com.google.common.reflect.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.fauxpas.*;

import javax.annotation.*;

import static org.apiguardian.api.API.Status.*;

/**
 * A partial {@link Binding binding} that already has an attribute associated but is missing a {@link Route route}.
 * The functionality provided by this class is just syntactic sugar. Clients could use
 * {@link Binding#create(Object, Route)} directly.
 *
 * @see Binding#create(Object, Route)
 * @param <A> generic attribute.
 */
@API(status = STABLE)
public final class PartialBinding<A> {

    private final A attribute;

    PartialBinding(@Nullable final A attribute) {
        this.attribute = attribute;
    }

    public Binding<A> call(final ThrowingRunnable<? extends Exception> consumer) {
        return call(Route.call(consumer));
    }

    public Binding<A> call(final ThrowingConsumer<ClientHttpResponse, ? extends Exception> consumer) {
        return call(Route.call(consumer));
    }

    public <I> Binding<A> call(final Class<I> type, final ThrowingConsumer<I, ? extends Exception> consumer) {
        return call(Route.call(type, consumer));
    }

    public <I> Binding<A> call(final TypeToken<I> type, final ThrowingConsumer<I, ? extends Exception> consumer) {
        return call(Route.call(type, consumer));
    }

    @SafeVarargs
    public final <B> Binding<A> dispatch(final Navigator<B> navigator, final Binding<B>... bindings) {
        return call(RoutingTree.dispatch(navigator, bindings));
    }

    public Binding<A> call(final Route route) {
        return Binding.create(attribute, route);
    }

}
