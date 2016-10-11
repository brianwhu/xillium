package org.xillium.base.text;

import java.util.regex.*;
import org.xillium.base.Trifunctor;


/**
 * This class helps write complex text transformation logic as a composition of manageable processing functions guided by regex
 * patterns.
 * Each {@code GuidedTransformer} consists of a regex pattern and two functions, one to process accepted (matched) text segments
 * and the other rejected (unmatched).  Since a {@code GuidedTransformer} itself is also a function capable of processing rejected
 * text segments, a group of {@code GuidedTransformer}s can be composed into a text transformation pipeline with simple functions
 * at each processing stage.
 * <p>
 * In the following example, the code first copies all quoted text as recognized by pattern QUOTE_REGEX, then applies another regex
 * pattern PARAM_REGEX to pick out parameter specifications in the rest of the text and transform them into "?"s while consuming
 * the captured groups as well. Finally, everything left unrecognized is also copied to the output.
 * </p>
 * <pre>
 * {@code
 *      String transformed = new GuidedTransformer<MyFacility>(QUOTE_REGEX,
 *          GuidedTransformer.Action.COPY,
 *          new GuidedTransformer<MyFacility>(PARAM_REGEX,
 *              new Trifunctor<StringBuilder, StringBuilder, MyFacility, Matcher>() {
 *                  public StringBuilder invoke(StringBuilder sb, MyFacility facility, Matcher matcher) {
 *                      facility.process(matcher.group(1), matcher.group(2));
 *                      return sb.append("?");
 *                  }
 *              },
 *              GuidedTransformer.Action.COPY
 *          )
 *      ).invoke(new StringBuilder(), facility, "original text to process").toString();
 * }
 * </pre>
 * <p>A composition of {@code GuidedTransformer}s can be created and invoked in-place as shown above, or be pre-built once and
 * invoked many times later where fast transformation is required.</p>
 *
 * @param <F> the type of a caller provided facility object to be passed to both processing function
 */
public class GuidedTransformer<F> implements Trifunctor<StringBuilder, StringBuilder, F, CharSequence> {
    /**
     * Defalut actions
     */
    public static enum Action {
        SKIP,
        COPY
    }

    private final Pattern _pattern;
    private final Trifunctor<StringBuilder, StringBuilder, F, Matcher> _accepted;
    private final Trifunctor<StringBuilder, StringBuilder, F, CharSequence> _rejected;

    /**
     * Constructs a {@code GuidedTransformer} from a regex pattern, a {@code Trifunctor} that processes accepted text, and a
     * {@code Trifunctor} that processes rejected text.
     *
     * @param pattern a regex pattern
     * @param accepted a Trifunctor that is to be invoked on segments of text accepted by the pattern
     * @param rejected a Trifunctor that is to be invoked on segments of text rejected by the pattern
     */
    public GuidedTransformer(Pattern pattern, Trifunctor<StringBuilder, StringBuilder, F, Matcher> accepted,
                                              Trifunctor<StringBuilder, StringBuilder, F, CharSequence> rejected) {
        _pattern = pattern;
        _accepted = accepted;
        _rejected = rejected;
    }

    /**
     * Constructs a {@code GuidedTransformer} from a regex pattern and a {@code Trifunctor} that processes accepted text. 
     * Rejected text is handled based on the value of action.
     *
     * @param pattern a regex pattern
     * @param accepted a Trifunctor that is to be invoked on segments of text accepted by the pattern
     * @param action an instruction to either skip or copy segments of text rejected by the pattern
     */
    public GuidedTransformer(Pattern pattern, Trifunctor<StringBuilder, StringBuilder, F, Matcher> accepted, Action action) {
        _pattern = pattern;
        _accepted = accepted;
        _rejected = action == Action.SKIP ?
            new Trifunctor<StringBuilder, StringBuilder, F, CharSequence>() {
                public StringBuilder invoke(StringBuilder sb, F facility, CharSequence original) { return sb; }
            }
            :
            new Trifunctor<StringBuilder, StringBuilder, F, CharSequence>() {
                public StringBuilder invoke(StringBuilder sb, F facility, CharSequence original) { return sb.append(original); }
            };
    }

    /**
     * Constructs a {@code GuidedTransformer} from a regex pattern and a {@code Trifunctor} that processes rejected text.
     * Accepted text is handled based on the value of action.
     *
     * @param pattern a regex pattern
     * @param action an instruction to either skip or copy segments of text accepted by the pattern
     * @param rejected a Trifunctor that is to be invoked on segments of text rejected by the pattern
     */
    public GuidedTransformer(Pattern pattern, Action action, Trifunctor<StringBuilder, StringBuilder, F, CharSequence> rejected) {
        _pattern = pattern;
        _accepted = action == Action.SKIP ?
            new Trifunctor<StringBuilder, StringBuilder, F, Matcher>() {
                public StringBuilder invoke(StringBuilder sb, F facility, Matcher matcher) { return sb; }
            }
            :
            new Trifunctor<StringBuilder, StringBuilder, F, Matcher>() {
                public StringBuilder invoke(StringBuilder sb, F facility, Matcher matcher) { return sb.append(matcher.group(0)); }
            };
        _rejected = rejected;
    }

    /**
     * Invokes this {@code GuidedTransformer} to process the given text and produce output into a {@code StringBuilder}.
     *
     * @param sb a {@code StringBuilder} to collect transformed text
     * @param facility a caller provided facility object that is to be passed to the {@code Trifunctor}s
     * @param text the text to process
     * @return the {@code StringBuilder}
     */
    @Override
    public StringBuilder invoke(StringBuilder sb, F facility, CharSequence text) {
        int top = 0;
        Matcher matcher = _pattern.matcher(text);
        while (matcher.find()) {
            sb = _rejected.invoke(sb, facility, text.subSequence(top, matcher.start()));
            sb = _accepted.invoke(sb, facility, matcher);
            top = matcher.end();
        }
        return _rejected.invoke(sb, facility, text.subSequence(top, text.length()));
    }
}
