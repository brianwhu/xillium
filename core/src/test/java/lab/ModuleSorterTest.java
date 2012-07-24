package lab;

import java.util.*;
import org.xillium.core.util.ModuleSorter;

import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class ModuleSorterTest {
    private static final String[] name = {
        "settings",
        "global",
        "portal",
        "local",
        "special",
        "account",
        "trading",
    };

    private static final String[] base = {
        "*",            //"settings",
        "settings",     //"global",
        "settings",     //"portal",
        "portal",       //"local",
        "global",       //"special"
        "",             //"account",
        "",             //"trading",
    };

    @Test(groups={"basic"})
    public void testModuleSorter() {
        ModuleSorter sorter = new ModuleSorter();

        Random random = new Random();
        Set<Integer> set = new HashSet<Integer>();
        for (int i = 0; i < name.length; ++i) {
            int next;
            do {
                next = random.nextInt(name.length);
            } while (set.contains(next));
            set.add(next);
            sorter.add(new ModuleSorter.Entry(name[next], base[next], "/path/to/whatever.jar"));
            System.out.println(name[next]);
        }

        ModuleSorter.Sorted sorted = sorter.sort();

        List<ModuleSorter.Entry> list = new ArrayList<ModuleSorter.Entry>();
        Iterator<ModuleSorter.Entry> it = sorted.specials();
        while (it.hasNext()) {
            list.add(it.next());
        }
        it = sorted.regulars();
        while (it.hasNext()) {
            list.add(it.next());
        }

        System.out.println("Sorted: " + list.size());

        for (int i = 0; i < name.length; ++i) {
            System.out.println(list.get(i));
            if (!name[i].equals(list.get(i).name)) {
                throw new RuntimeException("Not in the right order: " + name[i]);
            }
        }
    }
}
