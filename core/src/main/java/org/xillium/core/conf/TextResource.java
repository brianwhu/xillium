package org.xillium.core.conf;


/**
 * A text body with a name.
 */
public class TextResource {
    public final String name;
    public String text = ""; // an empty string can go into map, null can't

    public TextResource(String n) {
        name = n;
    }

    public void set(String t) {
        text = t;
    }
}
