package org.xillium.data.validation;

import org.xillium.base.Trace;
import org.xillium.base.beans.Beans;
import java.lang.reflect.*;
import java.util.regex.*;


/**
 * A data validator associated with a member (field) of a DataObject.
 */
public class Validator {
    private static class Range<T extends Comparable<T>> {
        T min, max;
        boolean inclusive;
        Range(T min, T max, boolean inclusive) throws IllegalArgumentException {
            if ((min == null && max == null) || (min != null && max != null && min.compareTo(max) > 0)) {
                throw new IllegalArgumentException("Invalid range: min=" + min + ", max=" + max);
            }
            this.min = min;
            this.max = max;
            this.inclusive = inclusive;
        }
    }

    String _name;
    Method _valueOf;
    Range<?>[] _ranges;
    Pattern _pattern;
    int _size;
    Object[] _values;

    /**
     * Constructs a Validator for a member field inside a DataObject.
     *
     * @param name - the name of the field
     * @param type - the type of the field - if the field is an array, this is the component type
     * @param field - the field
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Validator(String name, Class<?> type, Field field) throws IllegalArgumentException {
        Trace.g.std.note(Validator.class, "Enter Validator.<init>(" + name + ", " + field + ')');
        _name = name;
        type = Beans.boxPrimitive(type);

        try {
            _valueOf = type == String.class ? null : getValueOf(type);
            if (_valueOf == null || (Modifier.isStatic(_valueOf.getModifiers()) && _valueOf.getReturnType() == type)) {
                try {
                    ranges s = field.getAnnotation(ranges.class);
                    if (s != null) {
                        _ranges = new Range[s.value().length];
                        for (int i = 0; i < _ranges.length; ++i) {
                            _ranges[i] = new Range(
                                (Comparable)convert(s.value()[i].min()),
                                (Comparable)convert(s.value()[i].max()),
                                s.value()[i].inclusive()
                            );
                        }
                    } else {
                        range r = field.getAnnotation(range.class);
                        if (r != null) {
                            _ranges = new Range[1];
                            _ranges[0] = new Range(
                                (Comparable)convert(r.min()),
                                (Comparable)convert(r.max()),
                                r.inclusive()
                            );
                        }
                    }

                    pattern p = field.getAnnotation(pattern.class);
                    _pattern = p != null ? Pattern.compile(p.value()) : null;

                    size z = field.getAnnotation(size.class);
                    _size = (z != null && z.value() > 0) ? z.value() : 0;

                    values v = field.getAnnotation(values.class);
                    _values = v != null ? convert(v.value()) : null;
                } catch (IllegalAccessException x) {
                    // should never happen
                    throw new RuntimeException(name, x);
                } catch (InvocationTargetException x) {
                    throw new IllegalArgumentException("Incompatible values in validation rules", x.getCause());
                } catch (ClassCastException x) {
                    throw new IllegalArgumentException("Type is not Comparable yet has range specifications");
                }
            } else {
                throw new IllegalArgumentException("'valueOf()' is non-static or returns wrong type");
            }
        } catch (NoSuchMethodException x) {
            throw new IllegalArgumentException("Type has no static method 'valueOf()'");
        }
    }

    public String getName() {
        return _name;
    }

    /**
     * Parses a string to produce a validated value of this given data type.
     *
     * @throws DataValidationException
     */
    public Object parse(String text) throws DataValidationException {
        try {
            preValidate(text);
            Object object = _valueOf != null ? valueOf(text) : text;
            postValidate(object);
            return object;
        } catch (DataValidationException x) {
            throw x;
        } catch (InvocationTargetException x) {
            // various format errors from valueOf() - ignore the details
            throw new DataValidationException("FailureInDataValidation{"+_name+'}');
        } catch (Throwable t) {
            throw new DataValidationException(_name, t);
        }
    }

    /**
     * Performs all data validation that is based on the string representation of the value before it is converted.
     *
     * @param text - the string representation of the data value
     * @throws DataValidationException if any of the data constraints are violated
     */
    public void preValidate(String text) throws DataValidationException {
        // size
        Trace.g.std.note(Validator.class, "preValidate: size = " + _size);
        if (_size > 0 && text.length() > _size) {
            throw new DataValidationException(_name + " - 'size' check failure for value '" + text + '\'');
        }

        // pattern
        Trace.g.std.note(Validator.class, "preValidate: pattern = " + _pattern);
        if (_pattern != null && !_pattern.matcher(text).matches()) {
            throw new DataValidationException(_name + " - 'pattern' check failure for value '" + text + '\'');
        }
    }

    /**
     * Performs all data validation that is appicable to the data value itself
     *
     * @param object - the data value
     * @throws DataValidationException if any of the data constraints are violated
     */
    @SuppressWarnings("unchecked")
    public void postValidate(Object object) throws DataValidationException {
        if (_values != null || _ranges != null) {
            if (_values != null) for (Object value: _values) {
                if (value.equals(object)) return;
            }

            if (_ranges != null) for (@SuppressWarnings("rawtypes") Range r: _ranges) {
                @SuppressWarnings("rawtypes")
                Comparable o = (Comparable)object;
                if (r.inclusive) {
                    if ((r.min == null || r.min.compareTo(o) <= 0) && (r.max == null || o.compareTo(r.max) <= 0)) {
                        return;
                    }
                } else {
                    if ((r.min == null || r.min.compareTo(o) < 0) && (r.max == null || o.compareTo(r.max) < 0)) {
                        return;
                    }
                }
            }

            throw new DataValidationException(_name + " - values/ranges check failure for value '" + object + '\'');
        }
    }

    /*!
     * Converts a string into a value of the associated type.
     * <p>
     * Note: If the string is the empty string "", it is converted to
     * <ul>
     * <li>Empty string "", if the associated type is String
     * <li>null, otherwise
     * </li>
     */
    private final Object convert(String text) throws IllegalAccessException, InvocationTargetException {
        return _valueOf != null ? (text.length() > 0 ? valueOf(text) : null) : text;
    }

    /*!
     * Converts an array of strings into an array of values of the associated type.
     * <p>
     * Note: If the string is the empty string "", it is converted to
     * <ul>
     * <li>Empty string "", if the associated type is String
     * <li>null, otherwise
     * </li>
     */
    private final Object[] convert(String[] text) throws IllegalAccessException, InvocationTargetException {
        if (_valueOf != null) {
            Object[] values = new Object[text.length];
            for (int i = 0; i < values.length; ++i) {
                values[i] = text[i].length() > 0 ? valueOf(text[i]) : null;
            }
            return values;
        } else {
            return text;
        }
    }

    private final Method getValueOf(Class<?> type) throws NoSuchMethodException {
        try {
            return type.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException x) {
            return type.getMethod("valueOf", type, String.class);
        }
    }

    private final Object valueOf(String text) throws IllegalAccessException, InvocationTargetException {
    return _valueOf.getParameterTypes().length == 1 ? _valueOf.invoke(null, text) : _valueOf.invoke(null, _valueOf.getReturnType(), text);
    }
}
