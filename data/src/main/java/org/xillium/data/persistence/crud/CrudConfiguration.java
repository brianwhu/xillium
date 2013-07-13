package org.xillium.data.persistence.crud;

import java.util.*;
import java.sql.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import org.xillium.base.beans.Strings;
import org.xillium.data.*;
import org.xillium.data.persistence.*;


/**
 * The global configuration for CRUD operations generated from database schema.
 */
public class CrudConfiguration {
    public static final Map<String, Map<String, String>> aliases = new HashMap<String, Map<String, String>>();
    public static final Map<String, String> icve = new HashMap<String, String>();

    /**
     * Defines alias mapping for a table.
     */
    public CrudConfiguration(String table, Map<String, String> alias) throws Exception {
        aliases.put(table, alias);
    }

    /**
     * Defines a mapping from the name of a constraint to an appropriate message when that constraint is violated.
     */
    public CrudConfiguration(Map<String, String> messages) throws Exception {
        icve.putAll(messages);
    }
}

