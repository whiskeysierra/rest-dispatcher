package org.zalando.riptide.autoconfigure;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RetryPolicyFactoryBeanTest {

    private final RetryPolicyFactoryBean unit = new RetryPolicyFactoryBean();

    @Test
    public void shouldRetryNeverIfNotConfigured() {
        assertThat(unit.getObject().getMaxRetries(), is(0));
    }

    @Test
    public void shouldRetryAsConfigured() {
        final RiptideProperties.Retry config = new RiptideProperties.Retry();
        config.setMaxRetries(42);
        unit.setConfiguration(config);

        assertThat(unit.getObject().getMaxRetries(), is(42));
    }

    @Test
    public void shouldRetryForeverIfNotSpecified() {
        final RiptideProperties.Retry config = new RiptideProperties.Retry();
        config.setMaxDuration(TimeSpan.of(1, MINUTES));
        unit.setConfiguration(config);

        assertThat(unit.getObject().getMaxRetries(), is(-1));
    }
}
