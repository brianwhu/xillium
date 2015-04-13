package org.xillium.base.type;

import java.util.*;
import org.xillium.base.beans.Strings;


/**
 * A String-friendly wrapper of java.util.EnumSet, this bit mask implementation allows its
 * value to be represented as a String of concatenated enum names.
 * <p/>
 * Beans.setValue() recognizes Flags fields and will perform String to Flags assignment.
 * However due to type erasure Beans can't resolve the enum type associated with the Flags
 * field unless the field is annotated with @typeinfo, as following.
 * <xmp>
 *      @typeinfo(MyEnumType.class) Flags<MyEnumType> flags;
 * </xmp>
 */
public class Flags<E extends Enum<E>> {
    public static final String MULTI_VALUE_SEPARATOR = "[,:\\s]{1,}";

    private final EnumSet<E> _mask;

    /**
     * Constructs a Flags with all individual flags cleared.
     */
    public Flags(Class<E> type) {
        _mask = EnumSet.noneOf(type);
    }

    /**
     * Sets a flag corresponding to the given enum value.
     */
    public Flags<E> set(E e) {
        _mask.add(e);
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
     * Tests whether a flag corresponding to the given enum value is set or not.
     */
    public boolean isSet(E e) {
        return _mask.contains(e);
    }

    /**
     * Tests whether none of the flags is set or not.
     */
    public boolean isNone() {
        return _mask.size() == 0;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Flags) && _mask.equals(((Flags)o)._mask);
    }

    @Override
    public int hashCode() {
        return _mask.hashCode();
    }

    /**
     * Returns a string representation of this object, in a format compatible with the static valueOf() method.
     */
    public String toString() {
        return Strings.join(_mask, ':');
    }

    /**
     * Returns a string representation of this object, in a format compatible with the static valueOf() method.
     */
    public String getText() {
        return Strings.join(_mask, ':');
    }

    /**
     * Returns a Flags with a value represented by a string of concatenated enum literal names
     * separated by any combination of a comma, a colon, or a white space.
     */
    public static <E extends Enum<E>> Flags<E> valueOf(Class<E> type, String values) {
        Flags<E> flags = new Flags<E>(type);
        for (String text : values.trim().split(MULTI_VALUE_SEPARATOR)) {
            flags.set(Enum.valueOf(type, text));
        }
        return flags;
    }
}
