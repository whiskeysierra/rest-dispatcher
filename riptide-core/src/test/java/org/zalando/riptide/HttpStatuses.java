package org.zalando.riptide;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.springframework.http.HttpStatus;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.*;

final class HttpStatuses {

    @SuppressWarnings("deprecation")
    private static final ImmutableSet<HttpStatus> DEPRECATED = Sets.immutableEnumSet(
            CHECKPOINT, // duplicate with EARLY_HINTS
            MOVED_TEMPORARILY, // duplicate with FOUND
            REQUEST_ENTITY_TOO_LARGE, // duplicate with PAYLOAD_TOO_LARGE
            REQUEST_URI_TOO_LONG // duplicate with URI_TOO_LONG
    );

    static Stream<HttpStatus> supported() {
        final Predicate<HttpStatus> isDeprecated = DEPRECATED::contains;
        return Stream.of(HttpStatus.values()).filter(isDeprecated.negate());
    }

}
