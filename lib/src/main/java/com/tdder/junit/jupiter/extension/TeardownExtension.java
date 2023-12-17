package com.tdder.junit.jupiter.extension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TeardownExtension implements ParameterResolver {

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

    static class TeardownRegistryImpl implements TeardownRegistry {

        @Override
        public <T extends AutoCloseable> T add(final T closeable) {
            return closeable;
        }

    }

}
