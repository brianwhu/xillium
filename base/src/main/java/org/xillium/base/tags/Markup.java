package org.xillium.base.tags;

import java.util.*;


/**
 * A markup template facility.
 */
public class Markup {
    List<Tag> tags = new ArrayList<Tag>();

    public void add(Tag tag) {
        tags.add(tag);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Tag tag: tags) {
            tag.generate(this, sb);
        }
        return sb.toString();
    }
}
