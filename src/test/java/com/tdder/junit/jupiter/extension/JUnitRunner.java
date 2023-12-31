package com.tdder.junit.jupiter.extension;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.platform.commons.util.ReflectionUtils;
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
        return runTest(discoveryRequest);
    }

    static TestExecutionSummary runTestMethod(final Class<?> testClass, final String methodName) {
        final Method testMethod = findMethod(testClass, methodName);
        final LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();
        requestBuilder.selectors(DiscoverySelectors.selectMethod(testClass, testMethod));
        final LauncherDiscoveryRequest discoveryRequest = requestBuilder.build();
        return runTest(discoveryRequest);
    }

    private static TestExecutionSummary runTest(final LauncherDiscoveryRequest discoveryRequest) {
        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(discoveryRequest, LoggingListener.forJavaUtilLogging(), listener);

        final TestExecutionSummary summary = listener.getSummary();
        summary.getFailures().forEach(f -> f.getException().printStackTrace());
        return summary;
    }

    private static Method findMethod(final Class<?> testClass, final String methodName) {
        final List<Method> methods = ReflectionUtils.findMethods(testClass, m -> m.getName().equals(methodName));
        if (methods.size() != 1) {
            throw new RuntimeException("Failed to specify one method:" + methods);
        }
        return methods.get(0);
    }

}
