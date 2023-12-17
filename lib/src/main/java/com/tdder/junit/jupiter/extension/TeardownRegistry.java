package com.tdder.junit.jupiter.extension;

public interface TeardownRegistry {

    <T extends AutoCloseable> T add(T closeable);

}
