package org.xillium.base.text;

import java.util.*;
import org.xillium.base.Functor;


/**
 * This class provides character searching and string splitting functions that are similar to those provides by {@code String}
 * but aware of balanced symbols like parentheses and quotation marks.
 * <p>
 * The stanard set of symbols that <i>balance</i> includes: "(" and ")", "[" and "]", "{" and "}", single quotes, double quotes, and "`"s.
 * <p>
 * Between a balanced pair of symbols, where the opening symbol is the same as the closing symbol (the quotation marks, for examples),
 * none of the other symbols are recognized.
 * <p>
 * Between a balanced pair of symbols, where the opening symbol is different from the closing symbol (parentheses, brackets, and braces,
 * etc.), other symbols are recognized and balanced.
 * <p>
 * Unexpected closing symbols are always ignored and has no impact on subsequent searching and balancing.
 * <p>
 * The standard set of symbols can be replaced completely, or supplemented with more symbols, on each invocation. In both cases,
 * the new symbols are introduced by a {@code Functor<Integer, Integer>} that returns the closing symbol (as an {@code int})
 * for every opening symbol it knows of, and {@code 0} otherwise.
 */
public abstract class Balanced {

    /**
     * Returns the index within a string of the first occurrence of the specified character, similar to {@code String.indexOf}.
     * However, any occurrence of the specified character enclosed between balanced symbols is ignored.
     *
     * @param symbols an optional functor to provide the complete set of balancing symbols
     * @param text a String
     * @param begin a begin offset
     * @param end an end offset
     * @param target the character to search for
     * @param extra an optional functor to provide balancing symbols in addition to the standard ones
     * @return the index of the character in the string, or -1 if the specified character is not found
     */
    public static int indexOf(
    Functor<Integer, Integer> symbols, String text, int begin, int end, char target, Functor<Integer, Integer> extra
    ) {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        while (begin < end) {
            int c = text.charAt(begin);
            if (deque.size() > 0 && c == balance(symbols, deque.peek(), extra)) {
                deque.pop();
            } else if (balance(symbols, c, extra) > 0) {
                if (deque.size() == 0 || balance(symbols, deque.peek(), extra) != deque.peek()) deque.push(c);
            } else if (deque.size() == 0 && c == target) {
                return begin;
            }
            ++begin;
        }
        return -1;
    }

    /**
     * Returns the index within a string of the first occurrence of the specified character, similar to String.indexOf().
     * However, any occurrence of the specified character enclosed between balanced parentheses/brackets/braces is ignored.
     *
     * @param text a String
     * @param begin a begin offset
     * @param end an end offset
     * @param target the character to search for
     * @param extra an optional functor to provide balancing symbols in addition to the standard ones
     * @return the index of the character in the string, or -1 if the specified character is not found
     */
    public static int indexOf(String text, int begin, int end, char target, Functor<Integer, Integer> extra) {
        return indexOf(text, begin, end, target, extra);
    }

    /**
     * Returns the index within a string of the first occurrence of the specified character, similar to String.indexOf().
     * However, any occurrence of the specified character enclosed between balanced parentheses/brackets/braces is ignored.
     *
     * @param text a String
     * @param begin a begin offset
     * @param end an end offset
     * @param target the character to search for
     * @return the index of the character in the string, or -1 if the specified character is not found
     */
    public static int indexOf(String text, int begin, int end, char target) {
        return indexOf(text, begin, end, target, null);
    }

    /**
     * Returns the index within this string of the first occurrence of the specified character, similar to String.indexOf().
     * However, any occurrence of the specified character enclosed between balanced parentheses/brackets/braces is ignored.
     *
     * @param text a String
     * @param target the character to search for
     * @return the index of the character in the string, or -1 if the specified character is not found
     */
    public static int indexOf(String text, char target) {
        return indexOf(text, 0, text.length(), target, null);
    }

    /**
     * Splits a string around the specified character, storing split parts into a provided list.
     * However, any occurrence of the specified character enclosed between balanced parentheses/brackets/braces is ignored.
     *
     * @param list a List to store split parts
     * @param symbols an optional functor to provide the complete set of balancing symbols
     * @param text a String
     * @param begin a begin offset
     * @param end an end offset
     * @param delimiter the character to split the string around
     * @param extra an optional functor to provide balancing symbols in addition to the standard ones
     * @return the first argument
     */
    public static List<String> split(
    List<String> list, Functor<Integer, Integer> symbols, String text, int begin, int end, char delimiter, Functor<Integer, Integer> extra
    ) {
        int next;
        while ((next = Balanced.indexOf(symbols, text, begin, end, delimiter, extra)) != -1) {
            list.add(text.substring(begin, next));
            begin = next + 1;
        }
        if (begin < end) list.add(text.substring(begin, end));
        while (list.size() > 0 && list.get(list.size()-1).length() == 0) list.remove(list.size()-1);
        return list;
    }

    /**
     * Splits a string around the specified character, returning the parts in an array.
     * However, any occurrence of the specified character enclosed between balanced parentheses/brackets/braces is ignored.
     *
     * @param symbols an optional functor to provide the complete set of balancing symbols
     * @param text a String
     * @param begin a begin offset
     * @param end an end offset
     * @param delimiter the character to split the string around
     * @param extra an optional functor to provide balancing symbols in addition to the standard ones
     * @return a String array containing the split parts
     */
    public static String[] split(
    Functor<Integer, Integer> symbols, String text, int begin, int end, char delimiter, Functor<Integer, Integer> extra
    ) {
        List<String> list = new ArrayList<>();
        return split(list, symbols, text, begin, end, delimiter, extra).toArray(new String[list.size()]);
    }

    /**
     * Splits a string around the specified character, returning the parts in an array.
     * However, any occurrence of the specified character enclosed between balanced parentheses/brackets/braces is ignored.
     *
     * @param text a String
     * @param delimiter the character to split the string around
     * @return a String array containing the split parts
     */
    public static String[] split(String text, char delimiter) {
        List<String> list = new ArrayList<>();
        return split(list, null, text, 0, text.length(), delimiter, null).toArray(new String[list.size()]);
    }

    private static int balance(Functor<Integer, Integer> symbols, int c, Functor<Integer, Integer> extra) {
        if (symbols != null) {
            return symbols.invoke(c);
        } else {
            switch (c) {
            case '(': return ')';
            case '[': return ']';
            case '{': return '}';
            case '\'': return '\'';
            case '"': return '"';
            case '`': return '`';
            default: return extra != null ? extra.invoke(c) : 0;
            }
        }
    }
}
