package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.zalando.riptide.*;

import javax.annotation.*;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.zalando.riptide.autoconfigure.Name.*;

final class NameTest {

    @ParameterizedTest
    @CsvSource({
            "example,,exampleHttp",
            "my-example,,my-exampleHttp",
            ",,http",
            "example,Secure,exampleSecureHttp",
            "my-example,Secure,my-exampleSecureHttp",
            ",Secure,secureHttp"
    })
    void shouldToString(@Nullable final String id, @Nullable final String infix, final String expected) {
        assertEquals(expected, name(id, infix, Http.class).toString());
    }

    @ParameterizedTest
    @CsvSource({
            "example,,exampleHttp",
            "my-example,,myExampleHttp",
            ",,http",
            "example,Secure,exampleSecureHttp",
            "my-example,Secure,myExampleSecureHttp",
            ",Secure,secureHttp"
    })
    void shouldToNormalizedString(@Nullable final String id, @Nullable final String infix, final String expected) {
        assertEquals(expected, name(id, infix, Http.class).toNormalizedString());
    }

    @ParameterizedTest
    @CsvSource({
            "example,,exampleHttp,",
            "my-example,,my-exampleHttp,myExampleHttp",
            ",,http,",
            "example,Secure,exampleSecureHttp,",
            "my-example,Secure,my-exampleSecureHttp,myExampleSecureHttp",
            ",Secure,secureHttp,"
    })
    void shouldProduceAlternatives(@Nullable final String id, @Nullable final String infix,
            final String first, @Nullable final String second) {
        final Name unit = name(id, infix, Http.class);

        final Set<String> actual = unit.getAlternatives();

        assertEquals(Stream.of(first, second)
                .filter(Objects::nonNull).collect(toSet()), actual);
    }

}
