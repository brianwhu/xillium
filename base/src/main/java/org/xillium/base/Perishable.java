package org.xillium.base;

import java.util.LinkedHashMap;


/**
 * The Perishable interface should be implemented by any class whose instances may expire and becomes unusable.
 */
public interface Perishable {

    /**
     * Returns whether the Perishable has expired.
     *
     * @return whether the Perishable has expired
     */
    public boolean hasExpired();


    /**
     * A specialized Singleton to hold a Perishable, which when expired is considered as non-existent.
     */
    public static class Singleton<T extends Perishable> extends org.xillium.base.Singleton<T> {
        @Override
        protected boolean isMissing(T result) {
            return result == null || result.hasExpired();
        }
    }


    /**
     * A specialized self-cleaning {@code Map} to store Perishables, based on {@link java.util.LinkedHashMap LinkedHashMap}.
     */
    public static class Map<K, V extends Perishable> extends LinkedHashMap<K, V> {

        /**
         * Constructs a {@code Map} with default initial capacity and load factor.
         */
        public Map() {
        }

        /**
         * Constructs a {@code Map} with default load factor.
         *
         * @param initial the initial capacity
         */
        public Map(int initial) {
            super(initial);
        }

        /**
         * Constructs a {@code Map}.
         *
         * @param initial the initial capacity
         * @param load the load factor
         */
        public Map(int initial, float load) {
            super(initial, load);
        }

        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<K,V> eldest) {
            return eldest.getValue().hasExpired();
        }
    }

}
