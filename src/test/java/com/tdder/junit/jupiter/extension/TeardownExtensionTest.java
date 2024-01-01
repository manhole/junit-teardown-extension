package com.tdder.junit.jupiter.extension;

import static com.tdder.junit.jupiter.extension.JUnitRunner.runTest;
import static com.tdder.junit.jupiter.extension.JUnitRunner.runTestMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.instanceOf;
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

    @Test
    void fieldInjection_exceptionAtTest() throws Exception {
        // Exercise
        final TestExecutionSummary summary = runTestMethod(FieldInjectionExceptionCase.class, "exceptionAtTest");

        // Verify
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());
        assertThat(messages, is(contains("2", "1")));

        final List<TestExecutionSummary.Failure> failures = summary.getFailures();
        assertThat(failures.size(), is(1));
        final TestExecutionSummary.Failure failure = failures.get(0);
        final Throwable e = failure.getException();
        assertThat(e, is(instanceOf(AssertionError.class)));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getSuppressed(), is(emptyArray()));
    }

    @Test
    void fieldInjection_exceptionAtTeardown() throws Exception {
        // Exercise
        final TestExecutionSummary summary = runTestMethod(FieldInjectionExceptionCase.class, "exceptionAtTeardown");

        // Verify
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());

        assertThat(messages, is(contains("3", "2", "1")));

        final List<TestExecutionSummary.Failure> failures = summary.getFailures();
        assertThat(failures.size(), is(1));
        final TestExecutionSummary.Failure failure = failures.get(0);
        final Throwable e = failure.getException();
        assertThat(e, is(instanceOf(RuntimeException.class)));
        assertThat(e.getMessage(), is("2-ex"));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getSuppressed(), is(emptyArray()));
    }

    @Test
    void fieldInjection_exceptionAtTestAndTeardown() throws Exception {
        // Exercise
        final TestExecutionSummary summary = runTestMethod(FieldInjectionExceptionCase.class,
                "exceptionAtTestAndTeardown");

        // Verify
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());

        assertThat(messages, is(contains("3", "2", "1")));

        final List<TestExecutionSummary.Failure> failures = summary.getFailures();
        assertThat(failures.size(), is(1));
        final TestExecutionSummary.Failure failure = failures.get(0);
        final Throwable e = failure.getException();
        assertThat(e, is(instanceOf(AssertionError.class)));
        assertThat(e.getMessage(), is("expected: <1> but was: <2>"));
        assertThat(e.getCause(), is(nullValue()));
        final Throwable[] suppressed = e.getSuppressed();
        assertThat(suppressed.length, is(1));
        final Throwable sup = suppressed[0];
        assertThat(sup.getMessage(), is("2-ex"));
    }

    @Test
    void fieldInjection_exceptionsAtTeardown() throws Exception {
        // Exercise
        final TestExecutionSummary summary = runTestMethod(FieldInjectionExceptionCase.class, "exceptionsAtTeardown");

        // Verify
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());

        assertThat(messages, is(contains("4", "3", "2", "1")));

        final List<TestExecutionSummary.Failure> failures = summary.getFailures();
        assertThat(failures.size(), is(1));
        final TestExecutionSummary.Failure failure = failures.get(0);
        final Throwable e = failure.getException();
        assertThat(e, is(instanceOf(RuntimeException.class)));
        assertThat(e.getMessage(), is("3-ex"));
        assertThat(e.getCause(), is(nullValue()));
        final Throwable[] suppressed = e.getSuppressed();
        assertThat(suppressed.length, is(1));
        final Throwable sup = suppressed[0];
        assertThat(sup.getMessage(), is("2-ex"));
    }

    @Test
    void methodInjection_exceptionAtTest() throws Exception {
        // Exercise
        final TestExecutionSummary summary = runTestMethod(MethodInjectionExceptionCase.class, "exceptionAtTest");

        // Verify
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());
        assertThat(messages, is(contains("2", "1")));

        final List<TestExecutionSummary.Failure> failures = summary.getFailures();
        assertThat(failures.size(), is(1));
        final TestExecutionSummary.Failure failure = failures.get(0);
        final Throwable e = failure.getException();
        assertThat(e, is(instanceOf(AssertionError.class)));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getSuppressed(), is(emptyArray()));
    }

    @Test
    void methodInjection_exceptionAtTeardown() throws Exception {
        // Exercise
        final TestExecutionSummary summary = runTestMethod(MethodInjectionExceptionCase.class, "exceptionAtTeardown");

        // Verify
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());

        assertThat(messages, is(contains("3", "2", "1")));

        final List<TestExecutionSummary.Failure> failures = summary.getFailures();
        assertThat(failures.size(), is(1));
        final TestExecutionSummary.Failure failure = failures.get(0);
        final Throwable e = failure.getException();
        assertThat(e, is(instanceOf(RuntimeException.class)));
        assertThat(e.getMessage(), is("2-ex"));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getSuppressed(), is(emptyArray()));
    }

    @Test
    void methodInjection_exceptionAtTestAndTeardown() throws Exception {
        // Exercise
        final TestExecutionSummary summary = runTestMethod(MethodInjectionExceptionCase.class,
                "exceptionAtTestAndTeardown");

        // Verify
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());

        assertThat(messages, is(contains("3", "2", "1")));

        final List<TestExecutionSummary.Failure> failures = summary.getFailures();
        assertThat(failures.size(), is(1));
        final TestExecutionSummary.Failure failure = failures.get(0);
        final Throwable e = failure.getException();
        assertThat(e, is(instanceOf(AssertionError.class)));
        assertThat(e.getMessage(), is("expected: <1> but was: <2>"));
        assertThat(e.getCause(), is(nullValue()));
        final Throwable[] suppressed = e.getSuppressed();
        assertThat(suppressed.length, is(1));
        final Throwable sup = suppressed[0];
        assertThat(sup.getMessage(), is("2-ex"));
    }

    @Test
    void methodInjection_exceptionsAtTeardown() throws Exception {
        // Exercise
        final TestExecutionSummary summary = runTestMethod(MethodInjectionExceptionCase.class, "exceptionsAtTeardown");

        // Verify
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());

        assertThat(messages, is(contains("4", "3", "2", "1")));

        final List<TestExecutionSummary.Failure> failures = summary.getFailures();
        assertThat(failures.size(), is(1));
        final TestExecutionSummary.Failure failure = failures.get(0);
        final Throwable e = failure.getException();
        assertThat(e, is(instanceOf(RuntimeException.class)));
        assertThat(e.getMessage(), is("3-ex"));
        assertThat(e.getCause(), is(nullValue()));
        final Throwable[] suppressed = e.getSuppressed();
        assertThat(suppressed.length, is(1));
        final Throwable sup = suppressed[0];
        assertThat(sup.getMessage(), is("2-ex"));
    }

    @Test
    void methodInjection_exceptionsAtTestAndTeardown() throws Exception {
        // Exercise
        final TestExecutionSummary summary = runTestMethod(MethodInjectionExceptionCase.class,
                "exceptionsAtTestAndTeardown");

        // Verify
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());

        assertThat(messages, is(contains("4", "3", "2", "1")));

        final List<TestExecutionSummary.Failure> failures = summary.getFailures();
        assertThat(failures.size(), is(1));
        final TestExecutionSummary.Failure failure = failures.get(0);
        final Throwable e = failure.getException();
        assertThat(e, is(instanceOf(AssertionError.class)));
        assertThat(e.getMessage(), is("expected: <1> but was: <2>"));
        assertThat(e.getCause(), is(nullValue()));

        final Throwable[] suppressed = e.getSuppressed();
        assertThat(suppressed.length, is(2));
        {
            final Throwable sup = suppressed[0];
            assertThat(sup.getMessage(), is("3-ex"));
        }
        {
            final Throwable sup = suppressed[1];
            assertThat(sup.getMessage(), is("2-ex"));
        }
    }

    @ExtendWith(TeardownExtension.class)
    static class MethodInjection {

        @Test
        void succeeded1(final TeardownRegistry teardown) throws Exception {
            assertThat(teardown, is(notNullValue()));

            teardown.add(() -> messages.add("1"));
            teardown.add(() -> messages.add("2"));
            teardown.add(() -> messages.add("3"));
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

    @ExtendWith(TeardownExtension.class)
    static class FieldInjectionExceptionCase {

        private TeardownRegistry teardown_;

        @Test
        void exceptionAtTest() throws Exception {
            teardown_.add(() -> messages.add("1"));
            teardown_.add(() -> messages.add("2"));

            assertEquals(1, 2);
        }

        @Test
        void exceptionAtTeardown() throws Exception {
            teardown_.add(() -> messages.add("1"));
            teardown_.add(() -> {
                messages.add("2");
                throw new RuntimeException("2-ex");
            });
            teardown_.add(() -> messages.add("3"));
        }

        @Test
        void exceptionAtTestAndTeardown() throws Exception {
            teardown_.add(() -> messages.add("1"));
            teardown_.add(() -> {
                messages.add("2");
                throw new RuntimeException("2-ex");
            });
            teardown_.add(() -> messages.add("3"));

            assertEquals(1, 2);
        }

        @Test
        void exceptionsAtTeardown() throws Exception {
            teardown_.add(() -> messages.add("1"));
            teardown_.add(() -> {
                messages.add("2");
                throw new RuntimeException("2-ex");
            });
            teardown_.add(() -> {
                messages.add("3");
                throw new RuntimeException("3-ex");
            });
            teardown_.add(() -> messages.add("4"));
        }

    }

    @ExtendWith(TeardownExtension.class)
    static class MethodInjectionExceptionCase {

        @Test
        void exceptionAtTest(final TeardownRegistry teardown) throws Exception {
            teardown.add(() -> messages.add("1"));
            teardown.add(() -> messages.add("2"));

            assertEquals(1, 2);
        }

        @Test
        void exceptionAtTeardown(final TeardownRegistry teardown) throws Exception {
            teardown.add(() -> messages.add("1"));
            teardown.add(() -> {
                messages.add("2");
                throw new RuntimeException("2-ex");
            });
            teardown.add(() -> messages.add("3"));
        }

        @Test
        void exceptionAtTestAndTeardown(final TeardownRegistry teardown) throws Exception {
            teardown.add(() -> messages.add("1"));
            teardown.add(() -> {
                messages.add("2");
                throw new RuntimeException("2-ex");
            });
            teardown.add(() -> messages.add("3"));

            assertEquals(1, 2);
        }

        @Test
        void exceptionsAtTeardown(final TeardownRegistry teardown) throws Exception {
            teardown.add(() -> messages.add("1"));
            teardown.add(() -> {
                messages.add("2");
                throw new RuntimeException("2-ex");
            });
            teardown.add(() -> {
                messages.add("3");
                throw new RuntimeException("3-ex");
            });
            teardown.add(() -> messages.add("4"));
        }

        @Test
        void exceptionsAtTestAndTeardown(final TeardownRegistry teardown) throws Exception {
            teardown.add(() -> messages.add("1"));
            teardown.add(() -> {
                messages.add("2");
                throw new RuntimeException("2-ex");
            });
            teardown.add(() -> {
                messages.add("3");
                throw new RuntimeException("3-ex");
            });
            teardown.add(() -> messages.add("4"));

            assertEquals(1, 2);
        }

    }

}
