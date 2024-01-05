package com.tdder.junit.jupiter.extension;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * JUnit Jupiter extension that tears down test fixtures.
 *
 * <p>This extension is used with {@link TeardownRegistry}.
 * When test adds teardown object to {@link TeardownRegistry}, added teardown object will be executed after test.</p>
 *
 * <p>
 * Example:
 * </p>
 * <pre>
 * &#064;ExtendWith(TeardownExtension.class)
 * class MyTest {
 *
 *     // This field is injected by TeardownExtension.
 *     private TeardownRegistry teardownRegistry;
 *
 *     &#064;Test
 *     void someTest() {
 *         // === Setup ===
 *
 *         // Create test fixture
 *         final FooFixture fooFixture = createFooFixture(...);
 *
 *         // Register tear down code block
 *         teardownRegistry.add(() -&gt; fooFixture.clear());
 *
 *         final BarFixture barFixture = createBarFixture(...);
 *         teardownRegistry.add(() -&gt; barFixture.close());
 *
 *         // ...
 *
 *         // === Exercise ===
 *         // ...
 *
 *         // === Verify ===
 *         // ...
 *
 *         // After tests, either succeeded or failure, all added to TeardownRegistry code blocks are executed.
 *         // The execution order is in reverse order of addition to the TeardownRegistry.
 *     }
 *
 * }
 * </pre>
 *
 * @see TeardownRegistry
 * @author manhole
 */
public class TeardownExtension
        implements ParameterResolver, BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
            TeardownExtension.class);

    private final Object INSTANCE_STORE_KEY = TeardownExtension.class.getName() + "_INSTANCE";

    private final Object STATIC_STORE_KEY = TeardownExtension.class.getName() + "_STATIC";

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final boolean method = parameterContext.getDeclaringExecutable() instanceof Method;
        return parameterContext.getParameter().getType() == TeardownRegistry.class && method;
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {

        final Executable executable = parameterContext.getDeclaringExecutable();
        if (ModifierSupport.isStatic(executable)) {
            // @BeforeAll
            return resolveParameter(extensionContext, STATIC_STORE_KEY);
        } else {
            // @Before, test method
            return resolveParameter(extensionContext, INSTANCE_STORE_KEY);
        }
    }

    public Object resolveParameter(final ExtensionContext extensionContext, final Object storeKey)
            throws ParameterResolutionException {

        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        return store.getOrComputeIfAbsent(storeKey, (v) -> new TeardownRegistryImpl(), TeardownRegistryImpl.class);
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        injectStaticFields(extensionContext);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        final TestInstances requiredTestInstances = extensionContext.getRequiredTestInstances();
        final List<Object> allInstances = requiredTestInstances.getAllInstances();
        for (final Object instance : allInstances) {
            injectInstanceFields(extensionContext, instance);
        }
    }

    private void injectInstanceFields(final ExtensionContext extensionContext, final Object testInstance)
            throws IllegalAccessException {

        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        final List<Field> fields = instanceFields(testInstance.getClass());
        for (final Field field : fields) {
            final TeardownRegistry teardownRegistry = store.getOrComputeIfAbsent(INSTANCE_STORE_KEY,
                    (v) -> new TeardownRegistryImpl(), TeardownRegistryImpl.class);

            field.setAccessible(true);
            field.set(testInstance, teardownRegistry);
        }
    }

    private void injectStaticFields(final ExtensionContext extensionContext) throws IllegalAccessException {
        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        final Class<?> testClass = extensionContext.getRequiredTestClass();
        final List<Field> fields = staticFields(testClass);
        for (final Field field : fields) {
            final TeardownRegistry teardownRegistry = store.getOrComputeIfAbsent(STATIC_STORE_KEY,
                    (v) -> new TeardownRegistryImpl(), TeardownRegistryImpl.class);

            field.setAccessible(true);
            field.set(null, teardownRegistry);
        }
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) throws Exception {
        teardownContext(extensionContext, INSTANCE_STORE_KEY);
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) throws Exception {
        teardownContext(extensionContext, STATIC_STORE_KEY);
        teardownStaticFields(extensionContext);
    }

    private void teardownContext(final ExtensionContext extensionContext, final Object storeKey) throws Exception {
        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        final TeardownRegistryImpl teardown = store.get(storeKey, TeardownRegistryImpl.class);
        if (teardown != null) {
            final ExceptionHandler exceptionHandler = ExceptionHandler.determine(extensionContext);
            teardown.teardown(exceptionHandler);
            exceptionHandler.throwIfNeeded();
        }
    }

    private void teardownStaticFields(final ExtensionContext extensionContext) throws Exception {
        final Class<?> testClass = extensionContext.getRequiredTestClass();
        final List<Field> fields = staticFields(testClass);
        for (final Field field : fields) {
            // Clear static field to null. Because it will remain in memory.
            field.setAccessible(true);
            field.set(null, null);
        }
    }

    private static List<Field> instanceFields(final Class<?> testClass) {
        final Predicate<Field> predicate = ((Predicate<Field>) ModifierSupport::isNotStatic)
                .and(field -> field.getType().isAssignableFrom(TeardownRegistry.class))
                .and(ModifierSupport::isNotFinal);
        return ReflectionSupport.findFields(testClass, predicate, HierarchyTraversalMode.TOP_DOWN);
    }

    private static List<Field> staticFields(final Class<?> testClass) {
        final Predicate<Field> predicate = ((Predicate<Field>) ModifierSupport::isStatic)
                .and(field -> field.getType().isAssignableFrom(TeardownRegistry.class))
                .and(ModifierSupport::isNotFinal);
        return ReflectionSupport.findFields(testClass, predicate, HierarchyTraversalMode.TOP_DOWN);
    }

}
