package com.tdder.junit.jupiter.extension;

import static com.tdder.junit.jupiter.extension.JUnitRunner.runTest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
    void staticFieldInjection() throws Exception {
        assertThat(StaticFieldInjection.teardown_, is(nullValue()));
        final TestExecutionSummary summary = runTest(StaticFieldInjection.class);

        // after test, static field should be cleared to null.
        assertThat(StaticFieldInjection.teardown_, is(nullValue()));

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

    @Test
    void staticFieldInjection_independenceOnMultipleTestMethods() throws Exception {
        final TestExecutionSummary summary = runTest(StaticFieldInjection_IndependenceOnMultipleTestMethods.class);

        assertEquals(0, summary.getTestsFailedCount());
        assertEquals(2, summary.getTestsSucceededCount());
        assertThat(messages, is(contains("2-2", "2-1", "setUp succeeded2", "1-2", "1-1", "setUp succeeded1")));
    }

    @ExtendWith(TeardownExtension.class)
    static class MethodInjection {

        @Test
        void succeeded1(final TeardownRegistry teardown) throws Exception {
            assertThat(teardown, is(notNullValue()));

            teardown.add(() -> messages.add("1"));
            teardown.add(() -> messages.add("2"));
            teardown.add(() -> messages.add("3"));

            assertEquals(1, 1);
        }

    }

    @ExtendWith(TeardownExtension.class)
    static class ConstructorInjection {

        private final TeardownRegistry teardown_;

        ConstructorInjection(final TeardownRegistry teardown) {
            teardown_ = teardown;
        }

        @Test
        void test1() throws Exception {
            teardown_.add(() -> messages.add("never called"));
        }

    }

    @ExtendWith(TeardownExtension.class)
    static class FieldInjection {

        // injected by extension
        private TeardownRegistry teardown_;

        @Test
        void succeeded1() throws Exception {
            teardown_.add(() -> messages.add("1"));
            teardown_.add(() -> messages.add("2"));
            teardown_.add(() -> messages.add("3"));

            assertEquals(1, 1);
        }

    }

    @ExtendWith(TeardownExtension.class)
    static class StaticFieldInjection {

        // injected by extension
        private static TeardownRegistry teardown_;

        @Test
        void succeeded1() throws Exception {
            teardown_.add(() -> messages.add("1"));
            teardown_.add(() -> messages.add("2"));
            teardown_.add(() -> messages.add("3"));

            assertEquals(1, 1);
        }

    }

    @ExtendWith(TeardownExtension.class)
    @TestMethodOrder(MethodOrderer.MethodName.class) // make the test method execution order deterministic.
    static class MethodInjection_IndependenceOnMultipleTestMethods {

        @BeforeEach
        void setUp(final TeardownRegistry teardown, final TestInfo testInfo) {
            assertThat(((TeardownRegistryImpl) teardown).size(), is(0));
            teardown.add(() -> messages.add("setUp " + testInfo.getTestMethod().get().getName()));
        }

        @Test
        void succeeded1(final TeardownRegistry teardown) throws Exception {
            assertThat(((TeardownRegistryImpl) teardown).size(), is(1));
            teardown.add(() -> messages.add("1-1"));
            teardown.add(() -> messages.add("1-2"));
        }

        @Test
        void succeeded2(final TeardownRegistry teardown) throws Exception {
            assertThat(((TeardownRegistryImpl) teardown).size(), is(1));
            teardown.add(() -> messages.add("2-1"));
            teardown.add(() -> messages.add("2-2"));
        }

    }

    @ExtendWith(TeardownExtension.class)
    @TestMethodOrder(MethodOrderer.MethodName.class) // make the test method execution order deterministic.
    static class FieldInjection_IndependenceOnMultipleTestMethods {

        private TeardownRegistry teardown_;

        @BeforeEach
        void setUp(final TestInfo testInfo) {
            assertThat(((TeardownRegistryImpl) teardown_).size(), is(0));
            teardown_.add(() -> messages.add("setUp " + testInfo.getTestMethod().get().getName()));
        }

        @Test
        void succeeded1() throws Exception {
            assertThat(((TeardownRegistryImpl) teardown_).size(), is(1));
            teardown_.add(() -> messages.add("1-1"));
            teardown_.add(() -> messages.add("1-2"));
        }

        @Test
        void succeeded2() throws Exception {
            assertThat(((TeardownRegistryImpl) teardown_).size(), is(1));
            teardown_.add(() -> messages.add("2-1"));
            teardown_.add(() -> messages.add("2-2"));
        }

    }

    @ExtendWith(TeardownExtension.class)
    @TestMethodOrder(MethodOrderer.MethodName.class) // make the test method execution order deterministic.
    static class StaticFieldInjection_IndependenceOnMultipleTestMethods {

        private static TeardownRegistry teardown_;

        @BeforeEach
        void setUp(final TestInfo testInfo) {
            teardown_.add(() -> messages.add("setUp " + testInfo.getTestMethod().get().getName()));
        }

        @Test
        void succeeded1() throws Exception {
            assertThat(((TeardownRegistryImpl) teardown_).size(), is(1));
            teardown_.add(() -> messages.add("1-1"));
            teardown_.add(() -> messages.add("1-2"));
        }

        @Test
        void succeeded2() throws Exception {
            assertThat(((TeardownRegistryImpl) teardown_).size(), is(4));
            teardown_.add(() -> messages.add("2-1"));
            teardown_.add(() -> messages.add("2-2"));
        }

    }

}