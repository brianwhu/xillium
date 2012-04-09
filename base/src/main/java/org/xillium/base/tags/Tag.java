package org.xillium.base.tags;


/**
 * A basic trace facility.
 */
public interface Tag {
    /**
     * Generates contents into the provided StringBuilder.
     */
    public void generate(Markup markup, StringBuilder sb);
}
