package com.tdder.junit.jupiter.extension;

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
import org.junit.platform.commons.util.ReflectionUtils;

public class TeardownExtension
        implements ParameterResolver, BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
            TeardownExtension.class);

    private final Object STORE_KEY = TeardownRegistry.class;

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final boolean method = parameterContext.getDeclaringExecutable() instanceof Method;
        return parameterContext.getParameter().getType() == TeardownRegistry.class && method;
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        return store.getOrComputeIfAbsent(STORE_KEY, (v) -> new TeardownRegistryImpl(), TeardownRegistryImpl.class);
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

        final List<Field> fields = instanceFields(testInstance.getClass());
        for (final Field field : fields) {
            final ExtensionContext.Store store = getStore(extensionContext, field);
            final TeardownRegistry teardownRegistry = store.getOrComputeIfAbsent(STORE_KEY,
                    (v) -> new TeardownRegistryImpl(), TeardownRegistryImpl.class);

            field.setAccessible(true);
            field.set(testInstance, teardownRegistry);
        }
    }

    private void injectStaticFields(final ExtensionContext extensionContext)
            throws IllegalAccessException {

        final Class<?> testClass = extensionContext.getRequiredTestClass();
        final List<Field> fields = staticFields(testClass);
        for (final Field field : fields) {
            final ExtensionContext.Store store = getStore(extensionContext, field);
            final TeardownRegistry teardownRegistry = store.getOrComputeIfAbsent(STORE_KEY,
                    (v) -> new TeardownRegistryImpl(), TeardownRegistryImpl.class);

            field.setAccessible(true);
            field.set(null, teardownRegistry);
        }
    }

    private void teardownParameters(final ExtensionContext extensionContext) throws Exception {
        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        final TeardownRegistryImpl teardown = store.get(STORE_KEY, TeardownRegistryImpl.class);
        if (teardown != null) {
            teardown.close();
        }
    }

    private void teardownInstanceFields(final ExtensionContext extensionContext, final Object testInstance)
            throws Exception {
        final List<Field> fields = instanceFields(testInstance.getClass());
        for (final Field field : fields) {
            final ExtensionContext.Store store = getStore(extensionContext, field);
            final TeardownRegistryImpl teardown = store.get(STORE_KEY, TeardownRegistryImpl.class);
            teardown.close();
        }
    }

    private void teardownStaticFields(final ExtensionContext extensionContext) throws Exception {

        final Class<?> testClass = extensionContext.getRequiredTestClass();
        final List<Field> fields = staticFields(testClass);
        for (final Field field : fields) {
            final ExtensionContext.Store store = getStore(extensionContext, field);
            final TeardownRegistryImpl teardown = store.get(STORE_KEY, TeardownRegistryImpl.class);
            teardown.close();

            // Clear static field to null. Because it will remain in memory.
            field.setAccessible(true);
            field.set(null, null);
        }
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) throws Exception {
        teardownParameters(extensionContext);

        final TestInstances requiredTestInstances = extensionContext.getRequiredTestInstances();
        final List<Object> allInstances = requiredTestInstances.getAllInstances();
        for (final Object instance : allInstances) {
            teardownInstanceFields(extensionContext, instance);
        }
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) throws Exception {
        teardownStaticFields(extensionContext);
    }

    private static List<Field> instanceFields(final Class<?> testClass) {
        final Predicate<Field> predicate = ((Predicate<Field>) ReflectionUtils::isNotStatic)
                .and(field -> field.getType().isAssignableFrom(TeardownRegistry.class))
                .and(ReflectionUtils::isNotFinal);
        return ReflectionUtils.findFields(testClass, predicate, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);
    }

    private static List<Field> staticFields(final Class<?> testClass) {
        final Predicate<Field> predicate = ((Predicate<Field>) ReflectionUtils::isStatic)
                .and(field -> field.getType().isAssignableFrom(TeardownRegistry.class))
                .and(ReflectionUtils::isNotFinal);
        return ReflectionUtils.findFields(testClass, predicate, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);
    }

    private ExtensionContext.Store getStore(final ExtensionContext extensionContext, final Field field) {
        final ExtensionContext.Namespace namespace = NAMESPACE.append(field);
        return extensionContext.getStore(namespace);
    }

}
