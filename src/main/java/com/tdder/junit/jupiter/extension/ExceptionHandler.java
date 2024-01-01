package com.tdder.junit.jupiter.extension;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;

interface ExceptionHandler {

    static ExceptionHandler determine(final ExtensionContext extensionContext) {
        final Optional<Throwable> executionException = extensionContext.getExecutionException();
        if (executionException.isPresent()) {
            return new SuppressStrategy(executionException.get());
        }
        return new CollectStrategy();
    }

    void add(Exception e);

    void throwIfNeeded() throws Exception;

    class CollectStrategy implements ExceptionHandler {

        private Exception first;

        @Override
        public void add(final Exception e) {
            if (first == null) {
                first = e;
            } else {
                first.addSuppressed(e);
            }
        }

        @Override
        public void throwIfNeeded() throws Exception {
            if (first != null) {
                throw first;
            }
        }

    }

    class SuppressStrategy implements ExceptionHandler {

        private final Throwable first;

        SuppressStrategy(Throwable throwable) {
            first = throwable;
        }

        @Override
        public void add(final Exception e) {
            first.addSuppressed(e);
        }

        @Override
        public void throwIfNeeded() throws Exception {
        }

    }

}
