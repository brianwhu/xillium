package org.xillium.data;

import java.lang.reflect.*;
import java.util.*;
import org.xillium.data.validation.*;


/**
 * A data object.
 */
public interface DataObject {
    public static class Util {
        /**
         * Formulates a JSON representation of the structure of a DataObject.
         *
         * @param type - a DataObject class
         * @return a string that contains the JSON representation of the structure of the DataObject
         */
        public static String describe(Class<? extends DataObject> type) {
            return describe(new StringBuilder(), type);
        }

        public static String describe(StringBuilder sb, Class<? extends DataObject> type) {
            sb.append("[");
            print(sb, null, type);
            if (sb.length() > 1) sb.deleteCharAt(sb.length()-1);
            sb.append("]");
            return sb.toString();
        }

        private static void print(StringBuilder sb, String prefix, Class<? extends DataObject> type) {
            for (Field field: type.getDeclaredFields()) {
                if ((field.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC)) != 0) continue;

                String name = (prefix != null) ? prefix + '.' + field.getName() : field.getName();
                Class<?> ftype = field.getType();

                while (ftype.isArray()) {
                    ftype = ftype.getComponentType();
                    name = name + "[]";
                }
                if (DataObject.class.isAssignableFrom(ftype)) {
                    print(sb, name, (Class<? extends DataObject>)ftype);
                } else {
                    sb.append("{\"name\":\"").append(name).append("\",\"type\":\"").append(ftype.getName()).append('"');
                    subtype t = field.getAnnotation(subtype.class);
                    if (t != null) {
                        sb.append(",\"specialization\":\"").append(t.value()).append('"');
                    }

                    ranges s = field.getAnnotation(ranges.class);
                    if (s != null) {
                        sb.append(",\"ranges\":\"");
                        for (range r: s.value()) {
                            sb.append(r.min()).append(',').append(r.max()).append(';');
                        }
                        sb.append('"');
                    } else {
                        range r = field.getAnnotation(range.class);
                        if (r != null) {
                            sb.append(",\"range\":\"").append(r.min()).append(',').append(r.max()).append('"');
                        }
                    }

                    pattern p = field.getAnnotation(pattern.class);
                    if (p != null) {
                        sb.append(",\"pattern\":\"").append(p.value().replace("\\", "\\\\")).append('"');
                    }

                    size z = field.getAnnotation(size.class);
                    if (z != null && z.value() > 0) {
                        sb.append(",\"size\":").append(z.value());
                    }

                    values v = field.getAnnotation(values.class);
                    if (v != null) {
                        sb.append(",\"values\":\"(");
                        for (String value: v.value()) sb.append(value).append(',');
                        sb.deleteCharAt(sb.length()-1);
                        sb.append(")\"");
                    }

                    required r = field.getAnnotation(required.class);
                    if (r != null) {
                        sb.append(",\"required\":true");
                    }

                    sb.append("},");
                }
            }
        }
    }
}
