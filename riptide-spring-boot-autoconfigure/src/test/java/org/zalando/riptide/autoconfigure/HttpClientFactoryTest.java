package org.zalando.riptide.autoconfigure;

import com.google.common.collect.*;
import org.junit.jupiter.api.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.CertificatePinning.*;

import java.io.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

final class HttpClientFactoryTest {

    @Test
    void shouldFailOnKeystoreNotFound() {
        final Keystore nonExistingKeystore = new Keystore();
        nonExistingKeystore.setPath("i-do-not-exist.keystore");

        final RiptideProperties.Client client = new RiptideProperties.Client();
        client.setCertificatePinning(new CertificatePinning(true, nonExistingKeystore));

        final FileNotFoundException exception = assertThrows(FileNotFoundException.class, () ->
                HttpClientFactory.createHttpClientConnectionManager(withDefaults(client)));

        assertThat(exception.getMessage(), containsString("i-do-not-exist.keystore"));
    }

    @Test
    void shouldFailOnInvalidKeystore() {
        final Keystore invalidKeystore = new Keystore();
        invalidKeystore.setPath("application-default.yml");

        final RiptideProperties.Client client = new RiptideProperties.Client();
        client.setCertificatePinning(new CertificatePinning(true, invalidKeystore));

        assertThrows(IOException.class, () ->
        HttpClientFactory.createHttpClientConnectionManager(withDefaults(client)));
    }

    private RiptideProperties.Client withDefaults(final RiptideProperties.Client client) {
        final RiptideProperties properties = Defaulting.withDefaults(
                new RiptideProperties(new Defaults(), ImmutableMap.of("example", client)));

        return properties.getClients().get("example");
    }

}
