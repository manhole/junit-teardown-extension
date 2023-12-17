package com.tdder.junit.jupiter.extension;

import static com.tdder.junit.jupiter.extension.JUnitRunner.runTest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

class TeardownExtensionTest {

    private static final List<String> messages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        messages.clear();
    }

    @Test
    void methodInjection() throws Exception {
        final TestExecutionSummary summary = runTest(MethodInjection.class);

        assertEquals(0, summary.getTestsFailedCount());
        assertEquals(1, summary.getTestsSucceededCount());
        assertThat(messages, is(contains("3", "2", "1")));
    }

    /*
     * Do not support constructor injection.
     */
    @Test
    void constructorInjection_forbidden() throws Exception {
        final TestExecutionSummary summary = runTest(ConstructorInjection.class);

        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());
        assertThat(messages, is(empty()));
    }

    @ExtendWith(TeardownExtension.class)
    static class MethodInjection {

        @Test
        void succeeded1(final TeardownRegistry teardownRegistry) throws Exception {
            assertThat(teardownRegistry, is(notNullValue()));

            teardownRegistry.add(() -> messages.add("1"));
            teardownRegistry.add(() -> messages.add("2"));
            teardownRegistry.add(() -> messages.add("3"));

            assertEquals(1, 1);
        }

    }

    @ExtendWith(TeardownExtension.class)
    static class ConstructorInjection {

        private final TeardownRegistry teardownRegistry_;

        ConstructorInjection(final TeardownRegistry teardownRegistry) {
            teardownRegistry_ = teardownRegistry;
        }

        @Test
        void test1() throws Exception {
            teardownRegistry_.add(() -> messages.add("never called"));
        }

    }

}
