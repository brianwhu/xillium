package lab;

import java.util.*;
import org.xillium.core.util.ModuleSorter;
import org.xillium.core.util.ServiceModule;

import org.testng.annotations.*;


public class ModuleSorterTest {
    private static final String[] NAME = {
        "trex-settings",
        "trex-global",
        "unix-portal",
        "unix-local",
        "unix-special",
        "unix-account",
        "unix-trading",
    };

    private static final String[] BASE = {
        "*",            //"settings",
        "trex-settings",     //"global",
        "trex-settings",     //"portal",
        "unix-portal",       //"local",
        "trex-global",       //"special"
        "",             //"account",
        "",             //"trading",
    };

    private static final String[] LOOP = {
        "trex-global",       //"settings",
        "trex-settings",     //"global",
        "unix-settings",     //"portal",
        "unix-portal",       //"local",
        "trex-global",       //"special"
        "",             //"account",
        "",             //"trading",
    };

    private static ModuleSorter load(String[] name, String[] base) {
        ModuleSorter sorter = new ModuleSorter();

        Random random = new Random();
        Set<Integer> set = new HashSet<Integer>();
        for (int i = 0; i < name.length; ++i) {
            int next;
            do {
                next = random.nextInt(name.length);
            } while (set.contains(next));
            set.add(next);
            sorter.add(new ServiceModule("domain", name[next], "simple", base[next], "/path/to/whatever.jar"));
            System.out.println(name[next]);
        }

        return sorter;
    }

    @Test(groups={"basic"})
    public void testModuleSorter() {
/*
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
*/
        ModuleSorter sorter = load(NAME, BASE);
        ModuleSorter.Sorted sorted = sorter.sort();

        List<ServiceModule> list = new ArrayList<ServiceModule>();
        Iterator<ServiceModule> it = sorted.specials();
        while (it.hasNext()) {
            list.add(it.next());
        }
        it = sorted.regulars();
        while (it.hasNext()) {
            list.add(it.next());
        }

        System.out.println("Sorted: " + list.size());
        for (int i = 0; i < NAME.length; ++i) {
            System.out.println(list.get(i));
        }

        if (list.size() != NAME.length) {
            throw new RuntimeException("Wrong number of outputs: " + list.size());
        }

        for (int i = 0; i < NAME.length - 2; ++i) {
            if (!NAME[i].equals(list.get(i).name)) {
                throw new RuntimeException("Not in the right order: " + NAME[i]);
            }
        }
    }

    @Test(groups={"basic"}, expectedExceptions = IllegalArgumentException.class)
    public void testLoopDetection() {
        ModuleSorter sorter = load(NAME, LOOP);
        sorter.sort();
    }
}
