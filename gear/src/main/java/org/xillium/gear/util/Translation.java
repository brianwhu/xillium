package org.xillium.gear.util;

import java.io.*;
import java.util.*;
import org.xillium.base.beans.Strings;


/**
 * Service description.
 */
public class Translation {
    private final Map<String, Properties> _translations = new HashMap<String, Properties>();
    private final String _prefix;

    public Translation(String prefix) {
        _prefix = prefix;
    }

    protected synchronized Properties get(String language) throws IOException {
        Properties properties = _translations.get(language);
        if (properties == null) {
            try {
                Reader r = new InputStreamReader(getClass().getResourceAsStream(_prefix+'-'+language+".properties"), "UTF-8");
                try {
                    properties = new Properties();
                    properties.load(r);
                } finally {
                    r.close();
                }
                _translations.put(language, properties);
            } catch (NullPointerException x) {
                // no such language
            }
        }
        return properties;
    }

    public String translate(String language, String message) throws IOException {
		List<String> params = new ArrayList<String>();
        Properties translation = get(language);
        String translated = translation.getProperty(Strings.extractArguments(params, message));
        if (translated != null) {
            if (params.size() > 0) {
                Object[] args = params.toArray(new Object[params.size()]);
                for (int i = 0; i < args.length; ++i) {
                    String p = translation.getProperty((String)args[i]);
                    if (p != null) {
                        args[i] = p;
                    }
                }
                translated = String.format(null, translated, args);
            }
            return translated;
        } else {
            return message;
        }
    }
}
