package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apiguardian.api.API;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.zalando.fauxpas.ThrowingUnaryOperator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

@API(status = STABLE)
public final class Requester extends Dispatcher {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final RequestArguments arguments;
    private final Plugin plugin;

    private final ImmutableMultimap<String, String> query;
    private final HttpHeaders headers;

    Requester(final AsyncClientHttpRequestFactory requestFactory, final MessageWorker worker,
            final RequestArguments arguments, final Plugin plugin, final ImmutableMultimap<String,String> query,
            final HttpHeaders headers) {
        this.requestFactory = requestFactory;
        this.worker = worker;
        this.arguments = arguments;
        this.plugin = plugin;
        this.query = query;
        this.headers = headers;
    }

    private Requester(final Requester requester, final ImmutableMultimap<String,String> query, final HttpHeaders headers) {
        this(requester.requestFactory, requester.worker, requester.arguments, requester.plugin, query, headers);
    }

    public Requester queryParam(final String name, final String value) {
       return new Requester(this, ImmutableMultimap.<String, String>builder().putAll(query).put(name, value).build(), headers);
    }

    public Requester queryParams(final Multimap<String, String> params) {
        return new Requester(this, ImmutableMultimap.<String, String>builder().putAll(query).putAll(params).build(), headers);
    }

    public Requester accept(final MediaType acceptableMediaType, final MediaType... acceptableMediaTypes) {
        final HttpHeaders headers = copyHeaders();
        headers.setAccept(Lists.asList(acceptableMediaType, acceptableMediaTypes));
        return new Requester(this, query, headers);
    }

    public Requester contentType(final MediaType contentType) {
        final HttpHeaders headers = copyHeaders();
        headers.setContentType(contentType);
        return new Requester(this, query, headers);
    }

    public Requester ifModifiedSince(final OffsetDateTime since) {
        final HttpHeaders headers = copyHeaders();
        headers.setIfModifiedSince(since.toInstant().toEpochMilli());
        return new Requester(this, query, headers);
    }

    public Requester ifUnmodifiedSince(final OffsetDateTime since) {
        final HttpHeaders headers = copyHeaders();
        headers.setIfUnmodifiedSince(since.toInstant().toEpochMilli());
        return new Requester(this, query, headers);
    }

    public Requester ifNoneMatch(final String... entityTags) {
        return ifNoneMatch(Arrays.asList(entityTags));
    }

    public Requester ifMatch(final String... entityTags) {
        return ifMatch(Arrays.asList(entityTags));
    }

    private Requester ifMatch(final List<String> entityTags) {
        final HttpHeaders headers = copyHeaders();
        headers.setIfMatch(entityTags);
        return new Requester(this, query, headers);
    }

    private Requester ifNoneMatch(final List<String> entityTags) {
        final HttpHeaders headers = copyHeaders();
        headers.setIfNoneMatch(entityTags);
        return new Requester(this, query, headers);
    }

    public Requester header(final String name, final String value) {
        final HttpHeaders headers = copyHeaders();
        headers.add(name, value);
        return new Requester(this, query, headers);
    }

    public Requester headers(final HttpHeaders headers) {
        final HttpHeaders copy = copyHeaders();
        copy.putAll(headers);
        return new Requester(this, query, copy);
    }

    private HttpHeaders copyHeaders() {
        final HttpHeaders copy = new HttpHeaders();
        copy.putAll(headers);
        return copy;
    }

    public <T> Dispatcher body(@Nullable final T body) {
        return execute(body);
    }

    @Override
    public CompletableFuture<ClientHttpResponse> call(final Route route) {
        return execute(null).call(route);
    }

    private <T> Dispatcher execute(final @Nullable T body) {
        final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        headers.forEach(builder::putAll);

        return new ResponseDispatcher(new HttpEntity<>(body, headers), arguments
                .withQueryParams(ImmutableMultimap.copyOf(query))
                .withRequestUri()
                .withHeaders(builder.build())
                .withBody(body)
        );
    }

    private final class ResponseDispatcher extends Dispatcher {

        private final HttpEntity<?> entity;
        private final RequestArguments arguments;

        ResponseDispatcher(final HttpEntity<?> entity, final RequestArguments arguments) {
            this.entity = entity;
            this.arguments = arguments;
        }

        @Override
        public CompletableFuture<ClientHttpResponse> call(final Route route) {
            try {
                final RequestExecution before = plugin.beforeSend(this::send);
                final RequestExecution after = plugin.beforeDispatch(dispatch(before, route));

                return after.execute(arguments);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private CompletableFuture<ClientHttpResponse> send(final RequestArguments arguments) throws IOException {
            final AsyncClientHttpRequest request = createRequest(arguments);
            // TODO do this in the IO thread!
            worker.write(request, entity);
            final ListenableFuture<ClientHttpResponse> original = request.executeAsync();

            final CompletableFuture<ClientHttpResponse> future = preserveCancelability(original);
            original.addCallback(future::complete, future::completeExceptionally);
            return future;
        }

        private AsyncClientHttpRequest createRequest(final RequestArguments arguments) throws IOException {
            final URI requestUri = arguments.getRequestUri();
            final HttpMethod method = arguments.getMethod();
            return requestFactory.createAsyncRequest(requestUri, method);
        }

        private RequestExecution dispatch(final RequestExecution execution, final Route route) {
            return arguments -> execution.execute(arguments).thenApply(dispatch(route));
        }

        private ThrowingUnaryOperator<ClientHttpResponse, Exception> dispatch(final Route route) {
            return response -> {
                try {
                    route.execute(response, worker);
                } catch (final NoWildcardException e) {
                    throw new UnexpectedResponseException(response);
                }

                return response;
            };
        }

    }

}
