package org.xillium.core.util;

import java.util.*;
import lombok.Getter;


public class ModuleSorter {

    public void add(ServiceModule entry) {
        _map.put(entry.name, entry);
    }

    public Sorted sort() {
        return new Sorted(_map.values());
    }

    public class Sorted {
        @Getter private final SortedSet<ServiceModule> specials = new TreeSet<ServiceModule>(new Comparator());
        @Getter private final List<ServiceModule> regulars = new ArrayList<ServiceModule>();

        Sorted(Collection<ServiceModule> entries) {
            for (ServiceModule e: entries) {
                if (e.isSpecial()) {
                    specials.add(e);
                } else {
                    regulars.add(e);
                }
            }
        }
    }

    private final Map<String, ServiceModule> _map = new HashMap<String, ServiceModule>();

    private class Comparator implements java.util.Comparator<ServiceModule> {
        /**
         * Order by "base" relationship.
         *
         *  If o1 and o2 are identical, they equal
         *  If o1 is eventually based on o2, then o1 is after o2
         *  If o2 is eventually based on o1, then o2 is before o1
         *  Otherwise the order is given by the dictionary order of their names
         *
         * @throws IllegalArgumentException if a dependency loop is detected
         */
        public int compare(ServiceModule o1, ServiceModule o2) {
            if (o1.name.equals(o2.name)) {
                return 0;
            } else {
                // is o1 based on o2?
                Set<String> seen = new HashSet<String>();
                seen.add(o1.name);
                ServiceModule superier = _map.get(o1.base);
                while (superier != null) {
                    if (superier.name.equals(o2.name)) return 1;
                    if (seen.contains(superier.name)) {
                        throw new IllegalArgumentException(o1.name + ": loop detected at " + superier.name);
                    } else {
                        seen.add(superier.name);
                    }
                    superier = _map.get(superier.base);
                }

                // is o2 based on o1?
                seen.clear();
                seen.add(o2.name);
                superier = _map.get(o2.base);
                while (superier != null) {
                    if (superier.name.equals(o1.name)) return -1;
                    if (seen.contains(superier.name)) {
                        throw new IllegalArgumentException(o2.name + ": loop detected at " + superier.name);
                    } else {
                        seen.add(superier.name);
                    }
                    superier = _map.get(superier.base);
                }

                return o1.name.compareTo(o2.name);
            }
        }
    }
}
