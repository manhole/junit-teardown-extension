package com.tdder.junit.jupiter.extension;

/**
 * Teardown object registry.
 *
 * <p>TeardownRegistry instance is injected into a field or parameter by {@link TeardownExtension}</p>
 *
 * @see TeardownExtension
 * @author manhole
 */
public interface TeardownRegistry {

    /**
     * Register teardown object.
     *
     * <p>
     * Registered teardown objects are execute after test executed.
     * </p>
     * <ul>
     *     <li>If used on instance field or parameter, executed after {@link org.junit.jupiter.api.AfterEach}</li>
     *     <li>If used on static field or parameter, executed after {@link org.junit.jupiter.api.AfterAll}</li>
     * </ul>
     *
     * @param <T> {@code AutoCloseable}
     * @param closeable teardown object
     * @return closeable itself
     */
    <T extends AutoCloseable> T add(T closeable);

}
