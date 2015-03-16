package lab;

import java.util.Random;
import java.util.Arrays;
import java.math.BigDecimal;
import org.xillium.base.Functor;
import org.xillium.base.util.*;
import org.testng.annotations.*;


/**
 * Objects tests
 */
public class ObjectsTest {
    static class S {
        public String t;
        public int[] i = new int[8];
    }

    static class T {
        S s = new S();
    }

    /**
     * Objects.getProperty and Objects.setProperty
     */
    @Test(groups={"objects", "objects-property"})
    public void testObjectsProperty() throws Exception {
        String Good = "GOOD";
        String OneK = "1024";
        String TwoK = "2048";

        T data = new T();
        Objects.setProperty(data, "s.t", Good);
        assert Objects.getProperty(data, "s.t").equals(Good);

        Objects.setProperty(data, "s.i[5]", OneK);
        assert Objects.getProperty(data, "s.i[5]").equals(Integer.parseInt(OneK));

        Objects.setProperty(data, "s.i[2]", TwoK);
        assert Objects.getProperty(data, "s.i[2]").equals(Integer.parseInt(TwoK));
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

        String[] o = Objects.concat(a, b, c);

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

        int[] o = Objects.concat(a, b, c);

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

        Object[] objects = Objects.store(decimals, extra);
        for (int i = 0; i < decimals.length; ++i) {
            assert objects[i] == decimals[i];
        }
        assert objects[decimals.length] == extra;

        Double[] doubles = Objects.apply(new Double[decimals.length], decimals, new Functor<Double, BigDecimal>() {
            public Double invoke(BigDecimal decimal) {
                return decimal.doubleValue();
            }
        });
        for (int i = 0; i < decimals.length; ++i) {
            assert doubles[i] == decimals[i].doubleValue();
        }
    }
}
