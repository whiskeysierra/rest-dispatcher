package org.zalando.riptide;

import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public final class NoWildcardException extends RuntimeException {

    /**
     * We don't care for the stack trace. It's exclusively used to jump around the stack.
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
