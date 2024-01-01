package com.tdder.junit.jupiter.extension;

import java.util.Deque;
import java.util.LinkedList;

class TeardownRegistryImpl implements TeardownRegistry {

    private final Deque<AutoCloseable> tasks_ = new LinkedList<>();

    @Override
    public <T extends AutoCloseable> T add(final T closeable) {
        tasks_.add(closeable);
        return closeable;
    }

    int size() {
        return tasks_.size();
    }

    public void teardown(final ExceptionHandler exceptionHandler) throws Exception {
        while (!tasks_.isEmpty()) {
            // teardown in reverse order
            final AutoCloseable task = tasks_.removeLast();
            try {
                task.close();
            } catch (final Exception e) {
                exceptionHandler.add(e);
            }
        }
    }

}
