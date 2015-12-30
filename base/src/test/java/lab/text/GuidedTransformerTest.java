package lab.text;

import java.util.*;
import java.util.regex.*;
import org.xillium.base.Trifunctor;
import org.xillium.base.beans.*;
import org.xillium.base.text.GuidedTransformer;

import org.testng.annotations.*;


/**
 * GuidedTransformer test cases
 */
public class GuidedTransformerTest {
    private static final Pattern PARAM_SYNTAX = Pattern.compile(":([-+]?\\p{Alpha}\\w*\\??):(\\p{Alpha}\\w*)");
    private static final Pattern QUOTE_SYNTAX = Pattern.compile("'([^']*)'");

    public static class P {
        public final String name;
        public final int type;
        public P(String n, int t) {
            name = n;
            type = t;
        }
    }

    @Test(groups={"functional", "text", "transformer"})
    public void testTranslation() {
        String input = "SELECT 'YYYY-MM-DD HH24:MI:SS' F, A.* FROM TABLE1 A WHERE LOCATION = :location:VARCHAR AND ID = :id:NUMERIC";
        String output = null;
        StringBuilder sb = new StringBuilder();
        List<P> params = new ArrayList<>();

        GuidedTransformer<List<P>> transformer = new GuidedTransformer<List<P>>(QUOTE_SYNTAX,
            GuidedTransformer.Action.COPY,
            new GuidedTransformer<List<P>>(PARAM_SYNTAX,
                new Trifunctor<StringBuilder, StringBuilder, List<P>, Matcher>() {
                    public StringBuilder invoke(StringBuilder sb, List<P> params, Matcher matcher) {
                        try {
                            params.add(new P(matcher.group(1), java.sql.Types.class.getField(matcher.group(2)).getInt(null)));
                        } catch (Exception x) {
                            throw new IllegalArgumentException("Parameter specification :" + matcher.group(1) + ':' + matcher.group(2), x);
                        }
                        return sb.append("?");
                    }
                },
                GuidedTransformer.Action.COPY
            )
        );

        // without GuidedTransformer
        long time = System.currentTimeMillis();
        for (int i = 0; i < 10000; ++i) {
            params.clear();
            sb.setLength(0);
            int top = 0;
            Matcher matcher = QUOTE_SYNTAX.matcher(input);
            while (matcher.find()) {
                scan(params, sb, input, top, matcher.start());
                sb.append(input, matcher.start(), matcher.end());
                top = matcher.end();
            }
            scan(params, sb, input, top, input.length());
            output = sb.toString();
        }
        time = System.currentTimeMillis() - time;
        System.out.println("time = " + time + " milliseconds");
        System.out.println(Beans.toString(params));
        System.out.println(output);

        // with GuidedTransformer
        time = System.currentTimeMillis();
        for (int i = 0; i < 10000; ++i) {
            params.clear();
            sb.setLength(0);
            output = transformer.invoke(sb, params, input).toString();
        }
        time = System.currentTimeMillis() - time;
        System.out.println("GuidedTransformer time = " + time + " milliseconds");
        System.out.println(Beans.toString(params));
        System.out.println(output);
        assert "SELECT 'YYYY-MM-DD HH24:MI:SS' F, A.* FROM TABLE1 A WHERE LOCATION = ? AND ID = ?".equals(output);
    }

    private static void scan(List<P> params, StringBuilder sb, String text, int start, int end) {
        if (start < end) {
            Matcher matcher = PARAM_SYNTAX.matcher(text.substring(start, end));
            while (matcher.find()) {
                try {
                    params.add(new P(matcher.group(1), java.sql.Types.class.getField(matcher.group(2)).getInt(null)));
                } catch (Exception x) {
                    throw new IllegalArgumentException("Parameter specification :" + matcher.group(1) + ':' + matcher.group(2), x);
                }
            }
            sb.append(matcher.replaceAll("?"));
        }
    }
}
