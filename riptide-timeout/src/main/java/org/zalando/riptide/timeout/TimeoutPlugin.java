package org.zalando.riptide.timeout;

import com.google.gag.annotation.remark.*;
import lombok.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.util.concurrent.*;
import java.util.function.*;

import static java.util.Arrays.*;
import static org.apiguardian.api.API.Status.*;
import static org.zalando.riptide.CompletableFutures.*;

/**
 * @see "CompletableFuture#orTimeout(long, TimeUnit)"
 */
@API(status = STABLE)
@AllArgsConstructor
@ThisWouldBeOneLineIn(language = "Java 9", toWit = "return () -> execution.execute().orTimeout(timeout, unit)")
public final class TimeoutPlugin implements Plugin {

    private final ScheduledExecutorService scheduler;
    private final long timeout;
    private final TimeUnit unit;
    private final Executor executor;

    public TimeoutPlugin(final ScheduledExecutorService scheduler, final long timeout, final TimeUnit unit) {
        this(scheduler, timeout, unit, Runnable::run);
    }

    @Override
    public RequestExecution aroundAsync(final RequestExecution execution) {
        return arguments -> {
            final CompletableFuture<ClientHttpResponse> upstream = execution.execute(arguments);

            final CompletableFuture<ClientHttpResponse> downstream = new CompletableFuture<>();
            upstream.whenCompleteAsync(forwardTo(downstream), executor);

            final ScheduledFuture<?> scheduledTimeout = delay(timeout(downstream), cancel(upstream));
            upstream.whenCompleteAsync(cancel(scheduledTimeout), executor);

            return downstream;
        };
    }

    private <T> Runnable cancel(final CompletableFuture<T> future) {
        return () -> future.cancel(true);
    }

    private <T> Runnable timeout(final CompletableFuture<T> future) {
        return () -> future.completeExceptionally(new TimeoutException());
    }

    private ScheduledFuture<?> delay(final Runnable... tasks) {
        return scheduler.schedule(run(executor, tasks), timeout, unit);
    }

    private Runnable run(final Executor executor, final Runnable... tasks) {
        return () -> executor.execute(run(tasks));
    }

    private Runnable run(final Runnable... tasks) {
        return () -> stream(tasks).forEach(Runnable::run);
    }

    private <T> BiConsumer<T, Throwable> cancel(final Future<?> future) {
        return (result, throwable) -> future.cancel(true);
    }

}
