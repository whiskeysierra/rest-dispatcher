package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@FunctionalInterface
public interface Navigator<A> {

    default TypeToken<A> getType() {
        return new TypeToken<A>(getClass()) {};
    }

    Optional<Route> navigate(final ClientHttpResponse response, final RoutingTree<A> tree) throws IOException;

    default List<Binding<A>> merge(final List<Binding<A>> present, final List<Binding<A>> additional) {
        final Map<A, Binding<A>> bindings = new LinkedHashMap<>(present.size() + additional.size());

        present.forEach(binding -> bindings.put(binding.getAttribute(), binding));
        additional.forEach(binding -> bindings.put(binding.getAttribute(), binding));

        return new ArrayList<>(bindings.values());
    }

    default String toString(final A attribute) {
        return attribute.toString();
    }

}
