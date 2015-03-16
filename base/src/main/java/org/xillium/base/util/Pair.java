package org.xillium.base.util;

import org.xillium.base.Functor;


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

    /**
     * Constructs a Pair of 2 elements.
     */
    public Pair(T f, V s) {
        first = f;
        second = s;
    }

    /**
     * Cleanses an isomorphic list of a particular element. Only one thread should be updating the list at a time.
     *
     * Usage: feature = Pair.cleanse(feature, f2);
     *
     * @param list - a list represented by the first Pair object isomorphic to the elements, or an element when the list is trivial
     * @param element - the element to remove
     * @return the original isomorphic list with the given element cleansed
     */
    @SuppressWarnings("unchecked")
    public static <T> T cleanse(T list, T element) {
        if (list == null || list == element) {
            return null;
        } else if (list instanceof Pair) {
            Pair<T, T> pair = (Pair<T, T>)list;
            if (pair.first == element) {
                return pair.second;
            } else if (pair.second == element) {
                return pair.first;
            } else if (pair.second instanceof Pair) {
                pair.second = cleanse(pair.second, element);
            }
        }

        return list;
    }

    /**
     * Tests whether an isomorphic list includes a particular element.
     *
     * @param list - a list represented by the first Pair object isomorphic to the elements, or an element when the list is trivial
     * @param element - the element to look for
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean includes(T list, T element) {
        if (list == null) {
            return false;
        } else if (list == element) {
            return true;
        } else if (list instanceof Pair) {
            Pair<T, T> pair = (Pair<T, T>)list;
            if (pair.first == element || pair.second == element) {
                return true;
            } else if (pair.second instanceof Pair) {
                return includes(pair.second, element);
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
     * @param list - a list represented by the first Pair object isomorphic to the elements, or an element when the list is trivial
     * @return the number of elements discovered
     */
    @SuppressWarnings("unchecked")
    public static <T> int count(T list) {
        if (list == null) {
            return 0;
        } else if (list instanceof Pair) {
            return 1 + count(((Pair<T, T>)list).second);
        } else {
            return 1;
        }
    }

    /**
     * Traverses an isomorphic list using a Functor. The return values from the functor are discarded.
     *
     * @param list - a list represented by the first Pair object isomorphic to the elements, or an element when the list is trivial
     * @param func - a functor that will be invoked to inspect the elements, one at a time
     * @return the number of elements traversed
     */
    @SuppressWarnings("unchecked")
    public static <R, T> int traverse(T list, Functor<R, T> func) {
        if (list == null) {
            return 0;
        } else if (list instanceof Pair) {
            Pair<T, T> pair = (Pair<T, T>)list;
            func.invoke(pair.first);
            return 1 + traverse(pair.second, func);
        } else {
            func.invoke(list);
            return 1;
        }
    }

}
