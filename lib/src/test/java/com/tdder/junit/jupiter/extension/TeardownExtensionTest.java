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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
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

    @Test
    void fieldInjection() throws Exception {
        final TestExecutionSummary summary = runTest(FieldInjection.class);

        assertEquals(0, summary.getTestsFailedCount());
        assertEquals(1, summary.getTestsSucceededCount());
        assertThat(messages, is(contains("3", "2", "1")));
    }

    @Test
    void methodInjection_independenceOnMultipleTestMethods() throws Exception {
        final TestExecutionSummary summary = runTest(MethodInjection_IndependenceOnMultipleTestMethods.class);

        assertEquals(0, summary.getTestsFailedCount());
        assertEquals(2, summary.getTestsSucceededCount());
        assertThat(messages, is(contains("1-2", "1-1", "setUp succeeded1", "2-2", "2-1", "setUp succeeded2")));
    }

    @Test
    void fieldInjection_independenceOnMultipleTestMethods() throws Exception {
        final TestExecutionSummary summary = runTest(FieldInjection_IndependenceOnMultipleTestMethods.class);

        assertEquals(0, summary.getTestsFailedCount());
        assertEquals(2, summary.getTestsSucceededCount());
        assertThat(messages, is(contains("1-2", "1-1", "setUp succeeded1", "2-2", "2-1", "setUp succeeded2")));
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

    @ExtendWith(TeardownExtension.class)
    static class FieldInjection {

        // injected by extension
        private TeardownRegistry teardownRegistry_;

        @Test
        void succeeded1() throws Exception {
            teardownRegistry_.add(() -> messages.add("1"));
            teardownRegistry_.add(() -> messages.add("2"));
            teardownRegistry_.add(() -> messages.add("3"));

            assertEquals(1, 1);
        }

    }

    @ExtendWith(TeardownExtension.class)
    @TestMethodOrder(MethodOrderer.MethodName.class) // make the test method execution order deterministic.
    static class MethodInjection_IndependenceOnMultipleTestMethods {

        @BeforeEach
        void setUp(final TeardownRegistry teardownRegistry, final TestInfo testInfo) {
            assertThat(((TeardownRegistryImpl) teardownRegistry).size(), is(0));
            teardownRegistry.add(() -> messages.add("setUp " + testInfo.getTestMethod().get().getName()));
        }

        @Test
        void succeeded1(final TeardownRegistry teardownRegistry) throws Exception {
            assertThat(((TeardownRegistryImpl) teardownRegistry).size(), is(1));
            teardownRegistry.add(() -> messages.add("1-1"));
            teardownRegistry.add(() -> messages.add("1-2"));
        }

        @Test
        void succeeded2(final TeardownRegistry teardownRegistry) throws Exception {
            assertThat(((TeardownRegistryImpl) teardownRegistry).size(), is(1));
            teardownRegistry.add(() -> messages.add("2-1"));
            teardownRegistry.add(() -> messages.add("2-2"));
        }

    }

    @ExtendWith(TeardownExtension.class)
    @TestMethodOrder(MethodOrderer.MethodName.class) // make the test method execution order deterministic.
    static class FieldInjection_IndependenceOnMultipleTestMethods {

        private TeardownRegistry teardownRegistry_;

        @BeforeEach
        void setUp(final TestInfo testInfo) {
            assertThat(((TeardownRegistryImpl) teardownRegistry_).size(), is(0));
            teardownRegistry_.add(() -> messages.add("setUp " + testInfo.getTestMethod().get().getName()));
        }

        @Test
        void succeeded1() throws Exception {
            assertThat(((TeardownRegistryImpl) teardownRegistry_).size(), is(1));
            teardownRegistry_.add(() -> messages.add("1-1"));
            teardownRegistry_.add(() -> messages.add("1-2"));
        }

        @Test
        void succeeded2() throws Exception {
            assertThat(((TeardownRegistryImpl) teardownRegistry_).size(), is(1));
            teardownRegistry_.add(() -> messages.add("2-1"));
            teardownRegistry_.add(() -> messages.add("2-2"));
        }

    }

}
