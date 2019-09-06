package org.xillium.base;


/**
 * A Bifunctor embodies a two-argument function.
 */
@FunctionalInterface
public interface Bifunctor<R, T, U> {
    /**
     * Invokes the function on the two arguments.
     *
     * @param argument1 the first argument to pass to the bifunctor
     * @param argument2 the second argument to pass to the bifunctor
     * @return a return value
     */
    public R invoke(T argument1, U argument2);

    /**
     * A Bifunctor that always returns the first argument.
     */
    public static class First<R, U> implements Bifunctor<R, R, U> {
        public R invoke(R argument1, U argument2) { return argument1; }
    }

    /**
     * A Bifunctor that always returns the second argument.
     */
    public static class Second<R, T> implements Bifunctor<R, T, R> {
        public R invoke(T argument1, R argument2) { return argument2; }
    }
}
