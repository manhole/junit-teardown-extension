package com.tdder.junit.jupiter.extension;

import java.util.Deque;
import java.util.LinkedList;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TeardownExtension implements ParameterResolver, AfterEachCallback {

    private final ExtensionContext.Namespace namespace_ = ExtensionContext.Namespace.create(getClass());

    private final Object STORE_KEY = TeardownRegistry.class;

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == TeardownRegistry.class;
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final ExtensionContext.Store store = extensionContext.getStore(namespace_);
        return store.getOrComputeIfAbsent(STORE_KEY, (v) -> new TeardownRegistryImpl(),
                TeardownRegistryImpl.class);
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) throws Exception {
        final ExtensionContext.Store store = extensionContext.getStore(namespace_);
        final TeardownRegistryImpl teardown = store.get(STORE_KEY, TeardownRegistryImpl.class);
        if (teardown != null) {
            teardown.teardown();
        }
    }

    static class TeardownRegistryImpl implements TeardownRegistry {

        private final Deque<AutoCloseable> tasks_ = new LinkedList<>();

        @Override
        public <T extends AutoCloseable> T add(final T closeable) {
            tasks_.add(closeable);
            return closeable;
        }

        public void teardown() {
            while (!tasks_.isEmpty()) {
                // teardown in reverse order
                final AutoCloseable task = tasks_.removeLast();
                try {
                    task.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
