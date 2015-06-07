package org.xillium.core.conf;


/**
 * A text body with a name.
 */
public class TextResource {
    public final String name;
    public String text;

    public TextResource(String n) {
        name = n;
    }

    public void set(String t) {
        text = t;
    }
}
