package org.zalando.riptide.failsafe;

import com.google.common.annotations.*;
import com.google.common.collect.*;
import lombok.*;
import net.jodah.failsafe.*;
import net.jodah.failsafe.event.*;
import net.jodah.failsafe.function.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.idempotency.*;

import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import static lombok.AccessLevel.*;
import static org.apiguardian.api.API.Status.*;

@API(status = MAINTAINED)
@AllArgsConstructor(access = PRIVATE)
public final class FailsafePlugin implements Plugin {

    public static final Attribute<Integer> ATTEMPTS = Attribute.generate();

    private final ImmutableList<? extends Policy<ClientHttpResponse>> policies;
    private final ScheduledExecutorService scheduler;
    private final Predicate<RequestArguments> predicate;
    private final RetryListener listener;

    public FailsafePlugin(final ImmutableList<? extends Policy<ClientHttpResponse>> policies,
            final ScheduledExecutorService scheduler) {
        this(policies, scheduler, new IdempotencyPredicate(), RetryListener.DEFAULT);
    }

    public FailsafePlugin withPredicate(final Predicate<RequestArguments> predicate) {
        return new FailsafePlugin(policies, scheduler, predicate, listener);
    }

    public FailsafePlugin withListener(final RetryListener listener) {
        return new FailsafePlugin(policies, scheduler, predicate, listener);
    }

    @Override
    public RequestExecution aroundDispatch(final RequestExecution execution) {
        return arguments -> {
            final Policy<ClientHttpResponse>[] policies = select(arguments);

            if (policies.length == 0) {
                return execution.execute(arguments);
            }

            return Failsafe.with(select(arguments))
                    .with(scheduler)
                    .getStageAsync(context -> execution
                            .execute(withAttempts(arguments, context.getAttemptCount())));
        };
    }

    private Policy<ClientHttpResponse>[] select(final RequestArguments arguments) {
        final Stream<Policy<ClientHttpResponse>> stream = policies.stream()
                .filter(skipRetriesIfNeeded(arguments))
                .map(withRetryListener(arguments));

        @SuppressWarnings("unchecked") final Policy<ClientHttpResponse>[] policies = stream.toArray(Policy[]::new);

        return policies;
    }

    // TODO depends on the exception, e.g. pre-request exceptions are fine!
    private Predicate<Policy<ClientHttpResponse>> skipRetriesIfNeeded(final RequestArguments arguments) {
        return predicate.test(arguments) ?
                policy -> true :
                policy -> !(policy instanceof RetryPolicy);
    }

    private UnaryOperator<Policy<ClientHttpResponse>> withRetryListener(final RequestArguments arguments) {
        return policy -> {
            if (policy instanceof RetryPolicy) {
                final RetryPolicy<ClientHttpResponse> retryPolicy = (RetryPolicy<ClientHttpResponse>) policy;
                return retryPolicy.copy()
                        .onFailedAttempt(new RetryListenerAdapter(listener, arguments));
            } else {
                return policy;
            }
        };
    }

    private RequestArguments withAttempts(final RequestArguments arguments, final int attempts) {
        if (attempts == 0) {
            return arguments;
        }

        return arguments.withAttribute(ATTEMPTS, attempts);
    }

    @VisibleForTesting
    @AllArgsConstructor
    static final class RetryListenerAdapter implements CheckedConsumer<ExecutionAttemptedEvent<ClientHttpResponse>> {
        private final RetryListener listener;
        private final RequestArguments arguments;

        @Override
        public void accept(final ExecutionAttemptedEvent<ClientHttpResponse> event) {
            listener.onRetry(arguments, event);
        }
    }

}
