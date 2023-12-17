package com.tdder.junit.jupiter.extension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.platform.commons.util.ReflectionUtils;

public class TeardownExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    private final ExtensionContext.Namespace namespace_ = ExtensionContext.Namespace.create(getClass());

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
        final ExtensionContext.Store store = extensionContext.getStore(namespace_);
        return store.getOrComputeIfAbsent(STORE_KEY, (v) -> new TeardownRegistryImpl(),
                TeardownRegistryImpl.class);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        final TestInstances requiredTestInstances = extensionContext.getRequiredTestInstances();
        final List<Object> allInstances = requiredTestInstances.getAllInstances();
        for (final Object instance : allInstances) {
            final Predicate<Field> fieldPredicate = ((Predicate<Field>) ReflectionUtils::isNotStatic)
                    .and(field -> field.getType().isAssignableFrom(TeardownRegistry.class))
                    .and(ReflectionUtils::isNotFinal);
            final List<Field> fields = ReflectionUtils.findFields(instance.getClass(), fieldPredicate,
                    ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);
            for (final Field field : fields) {
                final ExtensionContext.Namespace namespace = namespace_.append(field);
                final ExtensionContext.Store store = extensionContext.getStore(namespace);
                // ExtensionContext.Store.CloseableResource mechanism calls TeardownRegistry
                final TeardownRegistry teardownRegistry = store.getOrComputeIfAbsent(STORE_KEY,
                        (v) -> new TeardownRegistryImpl(),
                        TeardownRegistryImpl.class);

                final boolean accessible = field.isAccessible();
                field.setAccessible(true);
                field.set(instance, teardownRegistry);
                field.setAccessible(accessible);
            }
        }

    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) throws Exception {
        final ExtensionContext.Store store = extensionContext.getStore(namespace_);
        final TeardownRegistryImpl teardown = store.get(STORE_KEY, TeardownRegistryImpl.class);
        if (teardown != null) {
            teardown.close();
        }
    }

}
