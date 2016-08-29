package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.Binding.create;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Route.pass;

@RunWith(MockitoJUnitRunner.class)
public class RoutingTreeTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private Route other;
    
    @Mock
    private Route expected;
    
    private final MessageReader reader = mock(MessageReader.class);
    
    @Test
    public void shouldUsedAttributeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                Binding.create(OK, expected),
                Binding.create(null, other))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    public void shouldUsedWildcardRoute() throws Exception {
        RoutingTree.dispatch(status(),
                Binding.create(OK, other),
                Binding.create(null, expected))
                .execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    public void shouldUsedAddedAttributeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                Binding.create(null, other))
                .merge(Binding.create(OK, expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    public void shouldUsedAddedWildcardRoute() throws Exception {
        RoutingTree.dispatch(status(),
                Binding.create(OK, other))
                .merge(Binding.create(null, expected))
                .execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    public void shouldUseLastWildcardRoute() throws Exception {
        RoutingTree.dispatch(status(),
                Binding.create((HttpStatus) null, other))
                .merge(Binding.create((HttpStatus) null, expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    public void shouldUseLastAttributeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                Binding.create(OK, other))
                .merge(Binding.create(OK, expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    public void shouldUseLastAddedAttributeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                Binding.create(OK, other),
                Binding.create(null, other))
                .merge(Binding.create(OK, other))
                .merge(Binding.create(OK, expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    public void shouldUseLastAddedWildcardeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                Binding.create(OK, other),
                Binding.create(null, other))
                .merge(asList(Binding.create(null, other),
                        Binding.create(null, expected)))
                .execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    public void shouldCreateNewRoutingTreeIfChanged() {
        final RoutingTree<HttpStatus> tree = RoutingTree.dispatch(status(), on(OK).call(pass()));
        final RoutingTree<HttpStatus> result = tree.merge(anyStatus().call(pass()));
        Assert.assertNotEquals(tree, result);
    }

    @Test
    public void shouldCreateNewRoutingTreeIfNotChanged() {
        final RoutingTree<HttpStatus> tree = RoutingTree.dispatch(status(), on(OK).call(pass()));
        final RoutingTree<HttpStatus> result = tree.merge(on(OK).call(pass()));
        Assert.assertNotEquals(tree, result);
    }

    @Test
    public void shouldCatchIOExceptionFromResponse() throws Exception {
        exception.expect(IOException.class);

        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenThrow(new IOException());

        RoutingTree.dispatch(status(), singletonList(anyStatus().call(pass())))
                .execute(response, reader);
    }

    @Test
    public void shouldCatchIOExceptionFromBinding() throws Exception {
        exception.expect(IOException.class);

        final HttpStatus anyStatus = null;
        final Binding<HttpStatus> binding = create(anyStatus, (response, converters) -> {
            throw new IOException();
        });

        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(OK);

        RoutingTree.dispatch(status(), singletonList(binding))
                .execute(response, reader);
    }

    @Test
    public void shouldFailForDuplicateBindings() {
        exception.expect(IllegalArgumentException.class);

        RoutingTree.dispatch(status(),
                on(OK).call(pass()),
                on(OK).call(pass()));
    }

    private MockClientHttpResponse response(final HttpStatus status) {
        return new MockClientHttpResponse((byte[]) null, status);
    }

}
