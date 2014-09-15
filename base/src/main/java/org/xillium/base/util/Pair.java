package org.xillium.base.util;


/**
 * A pair of elements packed in a single object.
 * <p/>
 * This class can be used as a basis to build an isomorphic list of elements where the the list itself 
 * implements the same interfaces as its elements, a structure very useful in building multicasting functions.
 * <xmp>
 *      public interface Feature { ... }
 *
 *      public class CompoundFeature extends Pair<Feature, Feature> implements Feature {
 *          ...
 *      }
 *
 *      Feature feature = new CompoundFeature(f1, new CompoundFeature(f2, new CompoundFeature(f3, f4)));
 * </xmp>
 */
public class Pair<T, V> {
    public T first;
    public V second;

    public Pair(T f, V s) {
        first = f;
        second = s;
    }

    /**
     * Cleanses an isomorphic list of a particular element.
     *
     * Usage: feature = Pair.cleanse(feature, f2);
     *
     * @param pair - the first Pair object that is isomorphic to the elements, or an element
     * @param element - the element to remove
     */
    public static <T> T cleanse(T pair, T element) {
        if (pair == null || pair == element) {
            return null;
        } else if (pair instanceof Pair) {
            Pair<T, T> list = (Pair<T, T>)pair;
            if (list.first == element) {
                return list.second;
            } else if (list.second == element) {
                return list.first;
            } else if (list.second instanceof Pair) {
                list.second = cleanse(list.second, element);
            }
        }

        return pair;
    }

    /**
     * Tests whether an isomorphic list includes a particular element.
     *
     * @param pair - the first Pair object that is isomorphic to the elements, or an element
     * @param element - the element to look for
     */
    public static <T> boolean includes(T pair, T element) {
        if (pair == null) {
            return false;
        } else if (pair == element) {
            return true;
        } else if (pair instanceof Pair) {
            Pair<T, T> list = (Pair<T, T>)pair;
            if (list.first == element || list.second == element) {
                return true;
            } else if (list.second instanceof Pair) {
                return includes(list.second, element);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Counts the number of elements in an isomorphic list.
     *
     * @param pair - the first Pair object that is isomorphic to the elements, or an element
     */
    public static <T> int count(T pair) {
        if (pair == null) {
            return 0;
        } else if (pair instanceof Pair) {
            return 1 + count(((Pair<T, T>)pair).second);
        } else {
            return 1;
        }
    }

}
