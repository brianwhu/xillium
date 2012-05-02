package org.xillium.base.beans;

import java.lang.reflect.Array;


public class JSONBuilder {
    /**
     * Constructs a new JSONBuilder with a default buffer size.
     */
	public JSONBuilder() {
		_sb = new StringBuilder();
	}

    /**
     * Constructs a new JSONBuilder with the given buffer size.
     */
	public JSONBuilder(int size) {
		_sb = new StringBuilder(size);
	}

    /**
     * Appends a liternal character to the JSON stream.
     */
	public JSONBuilder append(char c) {
		_sb.append(c);
		return this;
	}

    /**
     * Appends a liternal string to the JSON stream.
     */
	public JSONBuilder append(String s) {
		_sb.append(s);
		return this;
	}

    /**
     * Replaces the last character in the JSON stream.
     */
	public JSONBuilder replaceLast(char c) {
        _sb.setCharAt(_sb.length()-1, c);
		return this;
	}

    /**
     * Quotes properly a string and appends it to the JSON stream.
     */
	public JSONBuilder quote(String value) {
		_sb.append('"');
		for (int i = 0; i < value.length(); ++i) {
			char c = value.charAt(i);
			switch (c) {
			case '"':
				_sb.append("\\\"");
				break;
			case '\\':
				_sb.append("\\\\");
				break;
			default:
				if (c < 0x20) {
					_sb.append(CTRLCHARS[c]);
				} else {
					_sb.append(c);
				}
				break;
			}
		}
		_sb.append('"');
		return this;
	}

	public JSONBuilder serialize(Object value) {
		if (value == null) {
			_sb.append("null");
        } else {
            Class<?> t = value.getClass();
            if (t.isArray()) {
                _sb.append('[');
                for (int i = 0; i < Array.getLength(value); ++i) {
                    serialize(Array.get(value, i));
                    _sb.append(',');
                }
                _sb.setCharAt(_sb.length()-1, ']');
            } else if (Iterable.class.isAssignableFrom(t)) {
                _sb.append('[');
                for (Object object: (Iterable<?>)value) {
                    serialize(object);
                    _sb.append(',');
                }
                _sb.setCharAt(_sb.length()-1, ']');
            } else if (Number.class.isAssignableFrom(t) || Boolean.class.isAssignableFrom(t)) {
                _sb.append(value.toString());
            } else if (String.class == t) {
                quote((String)value);
            } else {
                quote(value.toString());
            }
        }
        return this;
	}

	public JSONBuilder serialize(String name, Object value) {
		return quote(name).append(':').serialize(value);
	}

	public int length() {
		return _sb.length();
	}

	public String toString() {
		return _sb.toString();
	}

	private static String[] CTRLCHARS = {
		/* 00 */"\\u0000",
		/* 01 */"\\u0001",
		/* 02 */"\\u0002",
		/* 03 */"\\u0003",
		/* 04 */"\\u0004",
		/* 05 */"\\u0005",
		/* 06 */"\\u0006",
		/* 07 */"\\u0007",
		/* 08 */"\\b",
		/* 09 */"\\t",
		/* 0A */"\\n",
		/* 0B */"\\u000b",
		/* 0C */"\\f",
		/* 0D */"\\r",
		/* 0E */"\\u000e",
		/* 0F */"\\u000f",
		/* 10 */"\\u0010",
		/* 11 */"\\u0011",
		/* 12 */"\\u0012",
		/* 13 */"\\u0013",
		/* 14 */"\\u0014",
		/* 15 */"\\u0015",
		/* 16 */"\\u0016",
		/* 17 */"\\u0017",
		/* 18 */"\\u0018",
		/* 19 */"\\u0019",
		/* 1A */"\\u001a",
		/* 1B */"\\u001b",
		/* 1C */"\\u001c",
		/* 1D */"\\u001d",
		/* 1E */"\\u001e",
		/* 1F */"\\u001f",
	};

	private final StringBuilder _sb;
}
