package org.xillium.core.util;

import java.util.*;


public class ModuleSorter {
    public static class Entry {
        public final String domain, name, simple, base, path;

        public Entry(String d, String n, String s, String b, String p) {
            domain = d;
            name = n;
            simple = s;
            base = b;
            path = p;
        }

        public boolean isSpecial() {
            return base != null && base.length() > 0;
        }

        public String toString() {
            return name + ':' + base + ':' + path;
        }
    }

    public void add(Entry entry) {
        _map.put(entry.name, entry);
    }

    public Sorted sort() {
        return new Sorted(_map.values());
    }

    public class Sorted {
        public Iterator<Entry> specials() {
            return _special.iterator();
        }

        public Iterator<Entry> regulars() {
            return _regular.iterator();
        }

        private final SortedSet<Entry> _special = new TreeSet<Entry>(new Comparator());
        private final List<Entry> _regular = new ArrayList<Entry>();

        Sorted(Collection<Entry> entries) {
            for (Entry e: entries) {
                if (e.isSpecial()) {
                    _special.add(e);
                } else {
                    _regular.add(e);
                }
            }
        }
    }

    private final Map<String, Entry> _map = new HashMap<String, Entry>();

    private class Comparator implements java.util.Comparator<Entry> {
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
        public int compare(Entry o1, Entry o2) {
            if (o1.name.equals(o2.name)) {
                return 0;
            } else {
                // is o1 based on o2?
                Set<String> seen = new HashSet<String>();
                seen.add(o1.name);
                Entry superier = _map.get(o1.base);
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
