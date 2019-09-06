package org.xillium.base;

import java.util.function.*;


/**
 * Facilities to allow use of lambda that throws checked exceptions.
 */
public interface Throwing {

    @FunctionalInterface
    public static interface Function<T, R, E extends Exception> extends java.util.function.Function<T, R> {
        @Override
        default R apply(T t) {
            try { return throwing(t); } catch (Exception x) { throw new RuntimeException(x); }
        }

        R throwing(T t) throws E;
    }

    @FunctionalInterface
    public static interface Consumer<T, E extends Exception> extends java.util.function.Consumer<T> {
        @Override
        default void accept(T t) {
            try { throwing(t); } catch (Exception x) { throw new RuntimeException(x); }
        }

        void throwing(T t) throws E;
    }

    /**
     * Casts a checked-exception throwing lambda into a Throwing.Function that only throws RuntimeException.
     */
    static <T, R, E extends Exception> java.util.function.Function<T, R> function(Function<T, R, E> f) {
        return f;
    }

    /**
     * Casts a checked-exception throwing lambda into a Throwing.Consumer that only throws RuntimeException.
     */
    static <T, E extends Exception> java.util.function.Consumer<T> consumer(Consumer<T, E> c) {
        return c;
    }

}

