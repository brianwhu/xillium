package lab.gear.util;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import org.xillium.base.*;
import org.xillium.data.DataObject;
import org.xillium.gear.model.coordinate;
import org.xillium.gear.util.*;
import org.testng.annotations.Test;


public class CacheSpaceTest {
    static final int PARAMETERS_WIDTH = 8;
    static final int PARAMETERS_COUNT = 4096;
    static final int SITUATIONS_COUNT = 4096;

    public static class TradingParameters implements DataObject {
        @coordinate(3) public String memberId;
        @coordinate(1) public String categoryId;
        @coordinate(2) public String goodsId;

        public BigDecimal param0;
        public BigDecimal param1;
        public BigDecimal param2;
        public BigDecimal param3;
        public BigDecimal param4;
        public BigDecimal param5;
        public BigDecimal param6;
        public BigDecimal param7;

        public TradingParameters(String c, String g, String m) {
            categoryId = c;
            goodsId = g;
            memberId = m;
        }

        public BigDecimal get(int index) {
            switch (index) {
            case 0: return param0;
            case 1: return param1;
            case 2: return param2;
            case 3: return param3;
            case 4: return param4;
            case 5: return param5;
            case 6: return param6;
            case 7: return param7;
            }
            throw new RuntimeException();
        }

        public void set(int index, BigDecimal value) {
            switch (index) {
            case 0: param0 = value; break;
            case 1: param1 = value; break;
            case 2: param2 = value; break;
            case 3: param3 = value; break;
            case 4: param4 = value; break;
            case 5: param5 = value; break;
            case 6: param6 = value; break;
            case 7: param7 = value; break;
            default: throw new RuntimeException();
            }
        }

        public String toString() {
            return "TradingParameters:<" + categoryId + ":" + goodsId + ':' + memberId + '>' +
                param0 + "," + param1 + "," + param2 + "," + param3 + "," + param4 + "," + param5 + "," + param6 + "," + param7;
        }
    }

    public static class TradingParametersRetriever implements Callable<List<TradingParameters>> {
        final String[] memberIds = { "John", "Steve", "Edward", "Isaac", "Albert", "Edwin", "Robert", "David", "Lee", "Mary", "Ana", "Cindy" };
        final String[] categoryIds = { "Fancy", "Cool", "Boring", "Special", "Nice", "Beautiful", "Essential", "Lovely", "Scary", "Troublesome" };
        final String[] goodsIds = { "A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z" };

        final List<TradingParameters> parameters = new ArrayList<TradingParameters>();
        final List<TradingParameters> situations = new ArrayList<TradingParameters>();

        public TradingParametersRetriever() {
            Random random = new Random();
            TradingParameters setting = null;

            for (int i = 0; i < PARAMETERS_COUNT; ++i) {
                switch (random.nextInt(4)) {
                case 0:
                case 1:
                    setting = new TradingParameters(
                        categoryIds[random.nextInt(categoryIds.length)],
                        goodsIds[random.nextInt(goodsIds.length)],
                        memberIds[random.nextInt(memberIds.length)]
                    );
                    break;
                case 2:
                    setting = new TradingParameters(
                        categoryIds[random.nextInt(categoryIds.length)],
                        goodsIds[random.nextInt(goodsIds.length)],
                        "-"
                    );
                    break;
                case 3:
                    setting = new TradingParameters(
                        categoryIds[random.nextInt(categoryIds.length)],
                        "-",
                        "-"
                    );
                    break;
                }
                for (int j = 0; j < PARAMETERS_WIDTH; ++j) {
                    setting.set(j, random.nextInt(5) < 4 ? null : new BigDecimal("0." + random.nextInt(100)));
                }
                parameters.add(setting);
            }
            setting = new TradingParameters("-", "-", "-");
            for (int j = 0; j < PARAMETERS_WIDTH; ++j) {
                setting.set(j, new BigDecimal("0." + random.nextInt(100)));
            }
            parameters.add(setting);
            //System.out.println(parameters.get(parameters.size() - 1));

            for (int i = 0; i < SITUATIONS_COUNT; ++i) {
                String categoryId = categoryIds[random.nextInt(categoryIds.length)];
                String goodsId = goodsIds[random.nextInt(goodsIds.length)];
                String memberId = memberIds[random.nextInt(memberIds.length)];
                situations.add(new TradingParameters(categoryId, goodsId, memberId));
            }

            Map<String, Map<String, Map<String, TradingParameters>>> map = new HashMap<String, Map<String, Map<String, TradingParameters>>>();
            for (TradingParameters item: parameters) {
                Map<String, Map<String, TradingParameters>> m2 = map.get(item.categoryId);
                if (m2 == null) map.put(item.categoryId, m2 = new HashMap<String, Map<String, TradingParameters>>());
                Map<String, TradingParameters> m3 = m2.get(item.goodsId);
                if (m3 == null) m2.put(item.goodsId, m3 = new HashMap<String, TradingParameters>());
                m3.put(item.memberId, item);
            }

            for (TradingParameters situation: situations) {
                for (int index = 0; index < PARAMETERS_WIDTH; ++index) {
                    situation.set(index, find(map, situation, index));
                }
            }
        }

        public List<TradingParameters> call() {
            return parameters;
        }

        private BigDecimal find(Map<String, Map<String, Map<String, TradingParameters>>> map, TradingParameters situation, int index) {
            Stack<TradingParameters> stack = new Stack<TradingParameters>();
            stack.push(map.get("-").get("-").get("-"));
            Map<String, Map<String, TradingParameters>> m2 = map.get(situation.categoryId);
            if (m2 == null) {
                m2 = map.get("-");
            }
            if (m2.get("-") != null && m2.get("-").get("-") != null) {
                stack.push(m2.get("-").get("-"));
            }
            Map<String, TradingParameters> m3 = m2.get(situation.goodsId);
            if (m3 == null) {
                m3 = m2.get("-");
            }
            if (m3 != null && m3.get("-") != null) {
                stack.push(m3.get("-"));
            }
            if (m3 != null) {
                TradingParameters target = m3.get(situation.memberId);
                if (target == null) {
                    target = m3.get("-");
                }
                if (target != null) {
                    stack.push(target);
                }
            }
            // backtrack
            while (stack.peek().get(index) == null) {
                stack.pop();
            }
            return stack.peek().get(index);
        }
    }

    @Test(groups={"cache", "cachespace"})
    public void test() throws Exception {
        TradingParametersRetriever retriever = new TradingParametersRetriever();

        CacheSpace<TradingParameters> c0 = new CacheSpace<TradingParameters>(null, TradingParameters.class);

        CacheSpace<TradingParameters> cs = new CacheSpace<TradingParameters>(retriever);
        cs.reload();
        System.out.println("Size = " + cs.count());

        long now = System.nanoTime();
        for (TradingParameters situation: retriever.situations) {
            TradingParameters p = cs.get(situation.categoryId, situation.goodsId, situation.memberId);
            for (int index = 0; index < PARAMETERS_WIDTH; ++index) {
                assert p.get(index).equals(situation.get(index));
            }
        }
        long cost = System.nanoTime() - now;
        System.out.println("Cost = " + cost);
    }
}
