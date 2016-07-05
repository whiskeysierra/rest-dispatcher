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

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

/**
 * TODO javadoc
 */
public final class NoRouteException extends RestClientException {

    private final ClientHttpResponse response;

    public NoRouteException(final ClientHttpResponse response) throws IOException {
        super(formatMessage(response));
        this.response = response;
    }

    private static String formatMessage(final ClientHttpResponse response) throws IOException {
        return String.format("Unable to dispatch response (%d %s, Content-Type: %s)",
                response.getRawStatusCode(), response.getStatusText(), response.getHeaders().getContentType());
    }

    public ClientHttpResponse getResponse() {
        return response;
    }

}
