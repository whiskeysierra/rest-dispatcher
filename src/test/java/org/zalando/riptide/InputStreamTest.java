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

import org.junit.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Selectors.contentType;

public final class InputStreamTest {

    static class InputStreamHttpMessageConverter implements HttpMessageConverter<InputStream> {

        @Override
        public boolean canRead(final Class<?> clazz, final MediaType mediaType) {
            return InputStream.class.isAssignableFrom(clazz);
        }

        @Override
        public boolean canWrite(final Class<?> clazz, final MediaType mediaType) {
            return false;
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return singletonList(MediaType.APPLICATION_OCTET_STREAM);
        }

        @Override
        public InputStream read(final Class<? extends InputStream> clazz, final HttpInputMessage inputMessage) throws IOException,
                HttpMessageNotReadableException {
            return inputMessage.getBody();
        }

        @Override
        public void write(final InputStream t, final MediaType contentType, final HttpOutputMessage outputMessage) throws IOException,
                HttpMessageNotWritableException {
            throw new IllegalStateException();


        }

    }

    private final URI url = URI.create("https://api.example.com/blobs/123");

    private final Rest unit;
    private final MockRestServiceServer server;

    public InputStreamTest() {
        final RestTemplate template = new RestTemplate();
        final InputStreamHttpMessageConverter converter = new InputStreamHttpMessageConverter();
        template.setMessageConverters(Collections.singletonList(converter));
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldAllowCloseOnce() throws IOException {
        final InputStream content = new CloseOnceInputStream(new byte[]{'b', 'l', 'o', 'b'});
        content.close();
        try {
            content.close();
            fail("Should prevent multiple close calls");
        } catch (final IOException e) {
            assertEquals("Stream is already closed", e.getMessage());
        }
    }

    @Test
    public void shouldNotAllowReadAfterClose() throws IOException {
        final InputStream content = new CloseOnceInputStream(new byte[]{'b', 'l', 'o', 'b'});
        content.close();
        try {
            final int ch = content.read();
            fail("Should prevent read calls after close");
        } catch (final IOException e) {
            assertEquals("Stream is already closed", e.getMessage());
        }
    }

    @Test
    public void shouldExtractOriginalBody() throws Exception {
        final InputStream content = new CloseOnceInputStream(new byte[]{'b', 'l', 'o', 'b'});

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(content))
                        .contentType(APPLICATION_OCTET_STREAM));

        final InputStream inputStream = unit.execute(GET, url)
                .dispatch(contentType(),
                        on(APPLICATION_OCTET_STREAM).capture(InputStream.class))
                .as(InputStream.class)
                .orElseThrow(AssertionError::new);

        assertEquals(content, inputStream);

        final int ch1 = inputStream.read();
        assertEquals('b', ch1);
    }

}
