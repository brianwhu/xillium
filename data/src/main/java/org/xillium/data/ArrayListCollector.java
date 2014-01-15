package org.xillium.data;

import java.util.*;


/**
 * A Collector that is an ArrayList.
 */
public class ArrayListCollector<T extends DataObject> extends ArrayList<T> implements Collector<T> {
    private static final long serialVersionUID = -2065000095890333812L;
}
