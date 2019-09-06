package org.xillium.base;


/**
 * A Trifunctor embodies a three-argument function.
 */
@FunctionalInterface
public interface Trifunctor<R, T, U, V> {
    /**
     * Invokes the function on the three arguments.
     *
     * @param argument1 the first argument to pass to the trifunctor
     * @param argument2 the second argument to pass to the trifunctor
     * @param argument3 the third argument to pass to the trifunctor
     * @return a return value
     */
    public R invoke(T argument1, U argument2, V argument3);

    /**
     * A Trifunctor that always returns the first argument.
     */
    public static class First<R, U, V> implements Trifunctor<R, R, U, V> {
        public R invoke(R argument1, U argument2, V argument3) { return argument1; }
    }

    /**
     * A Trifunctor that always returns the second argument.
     */
    public static class Second<R, T, V> implements Trifunctor<R, T, R, V> {
        public R invoke(T argument1, R argument2, V argument3) { return argument2; }
    }

    /**
     * A Trifunctor that always returns the third argument.
     */
    public static class Third<R, T, U> implements Trifunctor<R, T, U, R> {
        public R invoke(T argument1, U argument2, R argument3) { return argument3; }
    }
}
