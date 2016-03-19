package lab.text;

import java.util.*;
import java.util.regex.*;
import org.xillium.base.Functor;
import org.xillium.base.Trifunctor;
import org.xillium.base.beans.*;
import org.xillium.base.text.GuidedTransformer;
import org.xillium.base.text.Macro;

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

    // nested paratheses parsing
    //private static final Pattern COMMA_FREE = Pattern.compile("[^,]+");
    private static final Pattern PARENTHESES = Pattern.compile("\\([^()]*\\)");

    @Test(groups={"functional", "text", "transformer"})
    public void testNestedParentheses() {
        String text = "NVL(return_error_type, result_type) first argument,second argument,(f a, (g 1, 3))";

        final Map<String, String> store = new HashMap<>();
        GuidedTransformer<Map<String, String>> encoder = new GuidedTransformer<Map<String, String>>(PARENTHESES,
            new Trifunctor<StringBuilder, StringBuilder, Map<String, String>, Matcher>() {
                public StringBuilder invoke(StringBuilder sb, Map<String, String> store, Matcher matcher) {
                    sb.append("{#").append(store.size()).append('}');
                    store.put("#" + store.size(), matcher.group(0));
                    return sb;
                }
            },
            GuidedTransformer.Action.COPY
        );

        StringBuilder sb = new StringBuilder();
        int size = 0;
        do {
            size = store.size();
            text = encoder.invoke(sb.delete(0, sb.length()), store, text).toString();
        } while (size != store.size());

System.out.println(Beans.toString(store));
System.out.println(text);

        String[] args = text.split(",");
        for (int i = 0; i < args.length; ++i) {
            while (true) {
                String arg = Macro.expand(args[i], new Functor<Object, String>() {
                    public Object invoke(String name) {
                        return store.get(name);
                    }
                });
                if (arg.equals(args[i])) {
                    args[i] = arg;
                    break;
                } else {
                    args[i] = arg;
                }
            }
        }

        System.out.println(Beans.toString(args));
        assert args.length == 3;
        assert args[0].equals("NVL(return_error_type, result_type) first argument");
        assert args[1].equals("second argument");
        assert args[2].equals("(f a, (g 1, 3))");
    }
}
