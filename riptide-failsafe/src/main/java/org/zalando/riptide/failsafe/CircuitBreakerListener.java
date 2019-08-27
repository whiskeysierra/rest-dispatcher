package org.zalando.riptide.failsafe;

import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public interface CircuitBreakerListener {

    CircuitBreakerListener DEFAULT = new CircuitBreakerListener() {
        // nothing to implement, since default methods are sufficient
    };

    default void onOpen() {
        // nothing to do
    }

    default void onHalfOpen() {
        // nothing to do
    }

    default void onClose() {
        // nothing to do
    }

}
