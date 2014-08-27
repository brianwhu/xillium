package org.xillium.base.type;

import java.util.*;
import org.xillium.base.beans.Strings;


/**
 * A String-friendly wrapper of java.util.EnumSet, this bit mask implementation allows its
 * state to be represented as a String of concatenated names.
 * <p/>
 * Beans.setValue() recognizes Flags fields and will perform String to Flags assignment.
 * However due to type erasure Beans can't resolve the enum type associated with the Flags
 * field so the field must be declared and initialized, as following.
 * <xmp>
 *      Flags<MyEnumType> flags = new Flags<MyEnumType>(MyEnumType.class);
 * </xmp>
 */
public class Flags<E extends Enum<E>> {
    private final Class<E> _enum;
    private final EnumSet<E> _mask;

    /**
     * Constructs a Flags with all individual flags cleared.
     */
    public Flags(Class<E> type) {
        _enum = type;
        _mask = EnumSet.noneOf(type);
    }

    /**
     * Sets a flag corresponding to the given enum literal.
     */
    public Flags<E> set(E e) {
        _mask.add(e);
        return this;
    }

    /**
     * Sets a set of flags as denoted by a String of concatenated enum literal names
     * separated by any combination of a comma, a colon, or a white space.
     */
    public Flags<E> set(String values) {
        for (String text : values.trim().split("[,:\\s]{1,}")) {
            _mask.add(Enum.valueOf(_enum, text));
        }
        return this;
    }

    /**
     * Clears all flags.
     */
    public Flags<E> clear() {
        _mask.clear();
        return this;
    }

    /**
     * Tests whether a flag corresponding to the given enum literal is set or not.
     */
    public boolean isSet(E e) {
        return _mask.contains(e);
    }

    /**
     * Tests whether none of the flags is set.
     */
    public boolean isNone() {
        return _mask.size() == 0;
    }

    /**
     * Returns a string representation of all flags, in a format compatible with the set() method.
     */
    public String toString() {
        return Strings.join(_mask, ':');
    }
}
