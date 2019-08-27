package org.zalando.riptide.opentracing;

import io.opentracing.*;
import lombok.*;
import org.zalando.riptide.*;

import java.util.*;
import java.util.function.*;

@AllArgsConstructor
final class CompositeLifecyclePolicy implements LifecyclePolicy {

    private final Collection<LifecyclePolicy> policies;

    @Override
    public Optional<Span> start(final Tracer tracer, final RequestArguments arguments) {
        for (final LifecyclePolicy policy : policies) {
            final Optional<Span> span = policy.start(tracer, arguments);

            if (span.isPresent()) {
                return Optional.of(new FinishingSpan(span.get(), policy::finish));
            }
        }

        return Optional.empty();
    }

    @Override
    public void finish(final Span span) {
        span.finish();
    }

    @AllArgsConstructor
    private static final class FinishingSpan extends ForwardingSpan {

        private final Span span;
        private final Consumer<Span> callback;

        @Override
        protected Span delegate() {
            return span;
        }

        @Override
        public void finish() {
            callback.accept(span);
        }

    }

}
