package org.xillium.base.etc;

import java.lang.reflect.*;


/**
 * An object stringification utility.
 */
public class Auditable {
    /**
     * Stringifies the object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName()).append('{');
        Field[] fields = getClass().getDeclaredFields();
        for (Field field: fields) {
            field.setAccessible(true);
            sb.append(field.getName()).append(':');
            try {
                sb.append(field.get(this)).append(';');
            } catch (Exception x) {
                sb.append("(inaccessible);");
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
