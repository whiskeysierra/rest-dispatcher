package org.zalando.riptide;

import com.google.common.collect.ForwardingObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;

abstract class ForwardingClientHttpResponse extends ForwardingObject implements ClientHttpResponse {

    @Override
    protected abstract ClientHttpResponse delegate();

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return getStatusCode().value();
    }

    @Override
    public HttpStatusCode getStatusCode() throws IOException {
        return delegate().getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return delegate().getStatusText();
    }

    @Override
    public InputStream getBody() throws IOException {
        return delegate().getBody();
    }

    @Override
    public HttpHeaders getHeaders() {
        return delegate().getHeaders();
    }

}
