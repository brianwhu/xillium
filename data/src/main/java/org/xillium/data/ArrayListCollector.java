package org.xillium.data;

import java.util.*;


/**
 * A Collector that is an ArrayList.
 */
@SuppressWarnings("serial")
public class ArrayListCollector<T extends DataObject> extends ArrayList<T> implements Collector<T> {
}
