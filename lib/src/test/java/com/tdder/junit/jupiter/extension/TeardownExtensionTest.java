package com.tdder.junit.jupiter.extension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

class TeardownExtensionTest {

    @Test
    void methodInjection() throws Exception {
        final TestExecutionSummary summary = runTest(MethodInjection.class);

        assertEquals(0, summary.getTestsFailedCount());
        assertEquals(1, summary.getTestsSucceededCount());
    }

    static TestExecutionSummary runTest(final Class<?> testClass) {
        final LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();
        requestBuilder.selectors(DiscoverySelectors.selectClass(testClass));
        final LauncherDiscoveryRequest discoveryRequest = requestBuilder.build();

        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(discoveryRequest, LoggingListener.forJavaUtilLogging(), listener);

        final TestExecutionSummary summary = listener.getSummary();
        summary.getFailures().forEach(f -> f.getException().printStackTrace());
        return summary;
    }

    @ExtendWith(TeardownExtension.class)
    static class MethodInjection {

        @Test
        void succeeded1(final TeardownRegistry teardown) throws Exception {
            assertThat(teardown, is(notNullValue()));

            assertEquals(1, 1);
        }

    }

}
