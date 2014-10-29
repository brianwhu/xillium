package org.xillium.base.util;


/**
 * A collection of commonly used mathematical utilities.
 */
public class Mathematical {
    /**
     * Rounds n down to nearest multiple of m
     */
    public static int floor(int n, int m) {
        return n >= 0 ? (n / m) * m : ((n - m + 1) / m) * m;
    }
 
    /**
     * Rounds n down to nearest multiple of m
     */
    public static long floor(long n, long m) {
        return n >= 0 ? (n / m) * m : ((n - m + 1) / m) * m;
    }
 
    /**
     * Rounds n up to nearest multiple of m
     */
    public static int ceiling(int n, int m) {
        return n >= 0 ? ((n + m - 1) / m) * m : (n / m) * m;
    }
 
    /**
     * Rounds n up to nearest multiple of m
     */
    public static long ceiling(long n, long m) {
        return n >= 0 ? ((n + m - 1) / m) * m : (n / m) * m;
    }
}
