package lab;

import org.xillium.base.util.Pair;
import java.io.Serializable;
import org.testng.annotations.*;


public class PairTest {

    public static class Chain extends Pair<Serializable, Serializable> implements Serializable {
        public Chain(Serializable a, Serializable b) {
            super(a, b);
        }
    }

    @Test(groups={"pair"})
    public void cleansingTest() {
        String A0 = "A0";
        String A1 = "A1";
        String A2 = "A2";
        String A3 = "A3";
        String A4 = "A4";
        String A5 = "A5";
        String A6 = "A6";

        Chain chain = new Chain(A6, new Chain(A5, new Chain(A4, new Chain(A3, new Chain(A2, new Chain(A1, A0))))));

        assert Chain.count(chain) == 7;
        assert Chain.includes(chain, A6);
        assert Chain.includes(chain, A5);
        assert Chain.includes(chain, A4);
        assert Chain.includes(chain, A3);
        assert Chain.includes(chain, A2);
        assert Chain.includes(chain, A1);
        assert Chain.includes(chain, A0);
        chain = (Chain)Chain.cleanse(chain, A2);
        assert Chain.count(chain) == 6;
        assert Chain.includes(chain, A6);
        assert Chain.includes(chain, A5);
        assert Chain.includes(chain, A4);
        assert Chain.includes(chain, A3);
        assert !Chain.includes(chain, A2);
        assert Chain.includes(chain, A1);
        assert Chain.includes(chain, A0);
        chain = (Chain)Chain.cleanse(chain, A2);
        chain = (Chain)Chain.cleanse(chain, A4);
        chain = (Chain)Chain.cleanse(chain, A6);
        assert Chain.count(chain) == 4;
        assert !Chain.includes(chain, A6);
        assert Chain.includes(chain, A5);
        assert !Chain.includes(chain, A4);
        assert Chain.includes(chain, A3);
        assert !Chain.includes(chain, A2);
        assert Chain.includes(chain, A1);
        assert Chain.includes(chain, A0);
    }
}
