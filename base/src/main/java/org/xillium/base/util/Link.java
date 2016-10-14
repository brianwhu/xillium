package org.xillium.base.util;

import org.xillium.base.Open;


/**
 * A link to form a linked list as an explicit recursive structure, which is useful in association with Macro to produce text from data objects.
 */
public class Link<T> implements Open {
    private Link<T> last;

    /**
     * a data object wrapped in this link
     */
    public final T data;

    /**
     * a pointer to the next link
     */
    public Link<T> next;

    /**
     * Constructs a single link wrapping an object.
     *
     * @param object - a data object
     */
    public Link(T object) {
        data = object;
    }

    /**
     * Adds a new object at the end of the list identified by the root link.
     *
     * @param root - the root link of the list, which can be null
     * @param object - the data object to add
     * @return the root link
     */
    public static <T> Link<T> make(Link<T> root, T object) {
        Link<T> link = new Link<>(object);

        if (root == null) {
            root = link;
        } else if (root.last == null) {
            root.next = link;
        } else {
            root.last.next = link;
        }
        root.last = link;
        return root;
    }
}
