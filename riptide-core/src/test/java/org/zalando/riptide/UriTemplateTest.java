package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;
import java.util.Arrays;

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Routes.pass;

@RunWith(Parameterized.class)
public class UriTemplateTest {

    private final Rest unit;
    private final MockRestServiceServer server;

    private final HttpMethod method;
    private final Executor executor;

    public UriTemplateTest(final HttpMethod method, final Executor executor) {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();

        this.method = method;
        this.executor = executor;
    }

    interface Executor {
        Requester execute(Rest client, String uriTemplate, Object... uriVariables);
    }

    @Parameterized.Parameters(name= "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {HttpMethod.GET, (Executor) Rest::get},
                {HttpMethod.HEAD, (Executor) Rest::head},
                {HttpMethod.POST, (Executor) Rest::post},
                {HttpMethod.PUT, (Executor) Rest::put},
                {HttpMethod.PATCH, (Executor) Rest::patch},
                {HttpMethod.DELETE, (Executor) Rest::delete},
                {HttpMethod.OPTIONS, (Executor) Rest::options},
                {HttpMethod.TRACE, (Executor) Rest::trace},
        });
    }

    @Before
    public void setUp() {
        server.expect(requestTo("https://api.example.com/pages/123"))
                .andExpect(method(method))
                .andRespond(withSuccess());
    }

    @After
    public void tearDown() {
        server.verify();
    }

    @Test
    public void shouldExpand() throws IOException {
        executor.execute(unit, "/pages/{page}", 123)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

}
