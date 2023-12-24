package com.tdder.junit.jupiter.extension;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class JUnitRunner {

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

}
