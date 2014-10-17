package org.xillium.core.management;

import java.beans.ConstructorProperties;
import javax.management.*;


/**
 * A JMX bean that maintains an internal cache.
 */
@MXBean
public interface WithCache extends Manageable {

    /**
     * Cache state, a JMX open type
     */
    public static class CacheState {

        @ConstructorProperties({"size", "max", "read", "hit", "swap"})
        public CacheState(int s, int m, long r, long h, long p) {
            size = s;
            max = m;
            read = r;
            hit = h;
            swap = p;
        }

        /**
         * Returns the current cache size.
         */
        public int getSize() { return size; }

        /**
         * Returns the maximum cache size ever reached (the high water mark).
         */
        public int getMax() { return max; }

        /**
         * Returns the number of cache reads.
         */
        public long getRead() { return read; }

        /**
         * Returns the number of cache hits.
         */
        public long getHit() { return hit; }

        /**
         * Returns the number of cache replacements
         */
        public long getSwap() { return swap; }

        private final int size;
        private final int max;
        private final long read;
        private final long hit;
        private final long swap;

    }

	/**
	 * Refreshes the internal cache.
	 */
    public void refresh();

    /**
     * Reports current cache state, or null if there's no detailed state to report.
     */
    public CacheState getCacheState();

}
