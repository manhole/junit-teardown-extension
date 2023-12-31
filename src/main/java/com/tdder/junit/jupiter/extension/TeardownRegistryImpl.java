package com.tdder.junit.jupiter.extension;

import java.util.Deque;
import java.util.LinkedList;

import org.junit.jupiter.api.extension.ExtensionContext;

class TeardownRegistryImpl implements TeardownRegistry, ExtensionContext.Store.CloseableResource {

    private final Deque<AutoCloseable> tasks_ = new LinkedList<>();

    @Override
    public <T extends AutoCloseable> T add(final T closeable) {
        tasks_.add(closeable);
        return closeable;
    }

    int size() {
        return tasks_.size();
    }

    @Override
    public void close() throws Exception {
        Exception ex = null;
        while (!tasks_.isEmpty()) {
            // teardown in reverse order
            final AutoCloseable task = tasks_.removeLast();
            try {
                task.close();
            } catch (final Exception e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex.addSuppressed(e);
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

}
