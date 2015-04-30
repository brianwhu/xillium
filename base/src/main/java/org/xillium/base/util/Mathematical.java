package org.xillium.base.util;


/**
 * A collection of commonly used mathematical utilities.
 */
public class Mathematical {
    /**
     * Rounds n down to the nearest multiple of m
     *
     * @param n an integer
     * @param m an integer
     * @return the value after rounding {@code n} down to the nearest multiple of {@code m}
     */
    public static int floor(int n, int m) {
        return n >= 0 ? (n / m) * m : ((n - m + 1) / m) * m;
    }
 
    /**
     * Rounds n down to the nearest multiple of m
     *
     * @param n a long
     * @param m a long
     * @return the value after rounding {@code n} down to the nearest multiple of {@code m}
     */
    public static long floor(long n, long m) {
        return n >= 0 ? (n / m) * m : ((n - m + 1) / m) * m;
    }
 
    /**
     * Rounds n up to the nearest multiple of m
     *
     * @param n an integer
     * @param m an integer
     * @return the value after rounding {@code n} up to the nearest multiple of {@code m}
     */
    public static int ceiling(int n, int m) {
        return n >= 0 ? ((n + m - 1) / m) * m : (n / m) * m;
    }
 
    /**
     * Rounds n up to the nearest multiple of m
     *
     * @param n a long
     * @param m a long
     * @return the value after rounding {@code n} up to the nearest multiple of {@code m}
     */
    public static long ceiling(long n, long m) {
        return n >= 0 ? ((n + m - 1) / m) * m : (n / m) * m;
    }
}
