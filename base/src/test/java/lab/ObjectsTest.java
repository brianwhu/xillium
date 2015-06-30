package lab;

import java.lang.reflect.Array;
import java.util.*;
import java.math.BigDecimal;
import org.xillium.base.Functor;
import org.xillium.base.util.*;
import org.xillium.base.type.typeinfo;
import org.testng.annotations.*;


/**
 * Objects tests
 */
public class ObjectsTest {
    public static class S {
        public String t;
        public int[] i = new int[8];
        public List<int[]> j = Arrays.asList(new int[8], new int[5], new int[2]);
        @SuppressWarnings("unchecked") @typeinfo(Integer.class) public List<Integer>[] k = (List<Integer>[])Array.newInstance(List.class, 3);
        public S() {
            k[0] = new ArrayList<Integer>(); k[0].add(0);
            k[1] = new ArrayList<Integer>(); k[1].add(0);
            k[2] = new ArrayList<Integer>(); k[2].add(0);
        }
    }

    public static class T {
        public S s = new S();
        public S[] a = new S[] { new S(), new S() };
        public S[][] b = new S[][] {
            { new S(), new S() },
            { new S() }
        };
    }

    /**
     * Objects.getProperty and Objects.setProperty
     */
    @Test(groups={"objects", "objects-property"})
    public void testObjectsProperty() throws Exception {
        String Good = "GOOD";
        String OneK = "1024";
        String TwoK = "2048";
        String Nice = "4096";

        T data = new T();
        org.xillium.base.util.Objects.setProperty(data, "s.t", Good);
        assert org.xillium.base.util.Objects.getProperty(data, "s.t").equals(Good);

        org.xillium.base.util.Objects.setProperty(data, "s.i[5]", OneK);
        assert org.xillium.base.util.Objects.getProperty(data, "s.i[5]").equals(Integer.parseInt(OneK));

        org.xillium.base.util.Objects.setProperty(data, "s.i[2]", TwoK);
        assert org.xillium.base.util.Objects.getProperty(data, "s.i[2]").equals(Integer.parseInt(TwoK));

        org.xillium.base.util.Objects.setProperty(data, "a[1].i[2]", TwoK);
        assert org.xillium.base.util.Objects.getProperty(data, "s.i[2]").equals(Integer.parseInt(TwoK));

        org.xillium.base.util.Objects.setProperty(data, "b[0][1].i[2]", TwoK);
        assert org.xillium.base.util.Objects.getProperty(data, "b[0][1].i[2]").equals(Integer.parseInt(TwoK));

        org.xillium.base.util.Objects.setProperty(data, "b[0][1].j[1][3]", TwoK);
        org.xillium.base.util.Objects.setProperty(data, "b[0][1].j[1][4]", Nice);
        assert org.xillium.base.util.Objects.getProperty(data, "b[0][1].j[1][3]").equals(Integer.parseInt(TwoK));
        assert org.xillium.base.util.Objects.getProperty(data, "b[0][1].j[1][4]").equals(Integer.parseInt(Nice));

        org.xillium.base.util.Objects.setProperty(data, "b[0][1].k[2][0]", TwoK);
        assert org.xillium.base.util.Objects.getProperty(data, "b[0][1].k[2][0]").equals(Integer.parseInt(TwoK));

        try {
            org.xillium.base.util.Objects.getProperty(data, "b[0][1].notThere[2][0]");
            assert false;
        } catch (NoSuchFieldException x) {
        }
    }

    /**
     * Objects.concat
     */
    @Test(groups={"objects", "objects-array"})
    public void testObjectsArrayConcatReference() throws Exception {
        Random random = new Random();
        String[] a = new String[1 + random.nextInt(64)];
        for (int i = 0; i < a.length; ++i) a[i] = String.valueOf(random.nextInt());
        String[] b = new String[1 + random.nextInt(64)];
        for (int i = 0; i < b.length; ++i) b[i] = String.valueOf(random.nextInt());
        String[] c = new String[1 + random.nextInt(64)];
        for (int i = 0; i < c.length; ++i) c[i] = String.valueOf(random.nextInt());

        String[] o = org.xillium.base.util.Objects.concat(a, b, c);

        assert o.length == a.length + b.length + c.length;
        assert Arrays.equals(a, Arrays.copyOfRange(o, 0,                   a.length));
        assert Arrays.equals(b, Arrays.copyOfRange(o, a.length,            a.length + b.length));
        assert Arrays.equals(c, Arrays.copyOfRange(o, a.length + b.length, a.length + b.length + c.length));
    }

    /**
     * Objects.concat
     */
    @Test(groups={"objects", "objects-array"})
    public void testObjectsArrayConcatInteger() throws Exception {
        Random random = new Random();
        int[] a = new int[1 + random.nextInt(64)];
        for (int i = 0; i < a.length; ++i) a[i] = random.nextInt();
        int[] b = new int[1 + random.nextInt(64)];
        for (int i = 0; i < b.length; ++i) b[i] = random.nextInt();
        int[] c = new int[1 + random.nextInt(64)];
        for (int i = 0; i < c.length; ++i) c[i] = random.nextInt();

        int[] o = org.xillium.base.util.Objects.concat(a, b, c);

        assert o.length == a.length + b.length + c.length;
        assert Arrays.equals(a, Arrays.copyOfRange(o, 0,                   a.length));
        assert Arrays.equals(b, Arrays.copyOfRange(o, a.length,            a.length + b.length));
        assert Arrays.equals(c, Arrays.copyOfRange(o, a.length + b.length, a.length + b.length + c.length));
    }

    /**
     * Objects.store and Objects.apply
     */
    @Test(groups={"objects", "objects-array"})
    public void testObjectsArrayStoreApply() throws Exception {
        Random random = new Random();
        BigDecimal[] decimals = new BigDecimal[32];
        for (int i = 0; i < decimals.length; ++i) {
            decimals[i] = new BigDecimal(random.nextDouble());
        }
        BigDecimal extra = new BigDecimal(random.nextDouble());

        Object[] objects = org.xillium.base.util.Objects.store(decimals, extra);
        for (int i = 0; i < decimals.length; ++i) {
            assert objects[i] == decimals[i];
        }
        assert objects[decimals.length] == extra;

        Double[] doubles = org.xillium.base.util.Objects.apply(new Double[decimals.length], decimals, new Functor<Double, BigDecimal>() {
            public Double invoke(BigDecimal decimal) {
                return decimal.doubleValue();
            }
        });
        for (int i = 0; i < decimals.length; ++i) {
            assert doubles[i] == decimals[i].doubleValue();
        }
    }
}
