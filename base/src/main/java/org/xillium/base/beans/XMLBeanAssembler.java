package org.xillium.base.beans;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import java.lang.reflect.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xillium.base.etc.S;
import org.xillium.base.util.XML;


/**
 * An XML to Java beans binding utility.
 */
public class XMLBeanAssembler extends DefaultHandler {
    private static final Logger _logger = Logger.getLogger(XMLBeanAssembler.class.getName());
    SAXParser _parser;
    ObjectFactory _factory;
    String _jpkg;
    boolean _lenient;

    /**
     * Constructs an XMLBeanAssembler that uses the given ObjectFactory to create objects during assembly.
     *
     * @param factory an ObjectFactory
     * @throws ParserConfigurationException if the internal SAX parser can't be properly configured
     * @throws SAXException if the internal SAX parser fails
     */
    public XMLBeanAssembler(ObjectFactory factory) throws ParserConfigurationException, SAXException {
        _factory = factory;
        _parser = XML.newSAXParser();
    }

    /**
     * Tells XMLBeanAssembler to use a default Java package when a package can't be derived from namespace specification.
     *
     * @param pkg a Java package name
     * @return the XMLBeanAssembler itself
     */
    public XMLBeanAssembler setPackage(String pkg) {
        _jpkg = pkg;
        return this;
    }

    /**
     * Assembles a bean from an XML file.
     *
     * @param <T> the expected object type
     * @param file the path to the XML file
     * @return an object assembled from this XML file
     * @throws SAXException if the internal SAX parser fails
     * @throws IOException if any IO errors occur
     */
    @SuppressWarnings("unchecked")
    public <T> T build(String file) throws SAXException, IOException {
        _parser.parse(new File(file), this);
        return (T)getBean();
    }

    /**
     * Assembles a bean from an XML file.
     *
     * @param <T> the expected object type
     * @param file the path to the XML file
     * @param type the expected object type
     * @return an object assembled from this XML file
     * @throws SAXException if the internal SAX parser fails
     * @throws IOException if any IO errors occur
     */
    public <T> T build(String file, Class<T> type) throws SAXException, IOException {
        _parser.parse(new File(file), this);
        return type.cast(getBean());
    }

    /**
     * Assembles a bean from an XML stream.
     *
     * @param <T> the expected object type
     * @param stream the XML input stream
     * @return an object assembled from this XML stream
     * @throws SAXException if the internal SAX parser fails
     * @throws IOException if any IO errors occur
     */
    @SuppressWarnings("unchecked")
    public <T> T build(InputStream stream) throws SAXException, IOException {
        _parser.parse(stream, this);
        return (T)getBean();
    }

    /**
     * Assembles a bean from an XML stream.
     *
     * @param <T> the expected object type
     * @param stream the XML input stream
     * @param type the expected object type
     * @return an object assembled from this XML stream
     * @throws SAXException if the internal SAX parser fails
     * @throws IOException if any IO errors occur
     */
    public <T> T build(InputStream stream, Class<T> type) throws SAXException, IOException {
        _parser.parse(stream, this);
        return type.cast(getBean());
    }

    /**
     * Reports whether this XMLBeanAssembler is lenient on object creation failures.
     *
     * @return whether this XMLBeanAssembler is lenient on object creation failures
     */
    public boolean isLenient() {
        return _lenient;
    }

    /**
     * Sets whether this XMLBeanAssembler should be lenient on object creation failures.
     *
     * @param lenient whether this XMLBeanAssembler should be lenient on object creation failures
     */
    public void setLenient(boolean lenient) {
        _lenient = lenient;
    }


    private String fixPotentialArrayName(String name) {
        if (name.endsWith("[]")) {
            return "[L" + fixPotentialArrayName(name.substring(0, name.length()-2)) + ';';
        } else {
            return name;
        }
    }

    private void guessClassReference(TypedValues list, String name) {
        try {
            Class<?> type = Class.forName(fixPotentialArrayName(name));
            list.add(new TypedValue(type.getClass(), type));
        } catch (ClassNotFoundException y) {
            _logger.fine(name + " looked like a class reference but is not");
        }
    }

    private TypedValues guessUntypedValue(String name, String value) {
        TypedValues list = new TypedValues(name);

        if (value.startsWith("java:")) { // possible java static reference
            int dot;
            if (value.equals("java:null")) {
                list.add(new TypedValue(Object.class, null));
            } else if (value.charAt(5) == '$') { // reference to a local object (assemble @id)
                ElementInfo element = _local.get(value.substring(6));
                if (element != null) {
                    list.add(new TypedValue(element.type, element.data));
                }
            } else if ((dot = value.lastIndexOf('.')) > 5) {
                try {
                    // public static refernce? (don't override access control)
                    _logger.fine("public static refernce? " + value);
                    Object object = Class.forName(value.substring(5, dot)).getField(value.substring(dot+1)).get(null);
                    Class<?> type = object.getClass();
                    list.add(new TypedValue(type, object));
                    // automatic upscaling to larger types
                    if (type == Byte.class) {
                        object = new Short(((Byte)object).byteValue());
                        list.add(new TypedValue(Short.class, object));
                        type = Short.class;
                    }
                    if (type == Short.class) {
                        object = new Integer(((Short)object).shortValue());
                        list.add(new TypedValue(Integer.class, object));
                        type = Integer.class;
                    }
                    if (type == Integer.class) {
                        object = new Long(((Integer)object).intValue());
                        list.add(new TypedValue(Long.class, object));
                        type = Long.class;
                    }
                    if (type == Long.class) {
                        object = new Float(((Long)object).longValue());
                        list.add(new TypedValue(Float.class, object));
                        type = Float.class;
                    }
                    if (type == Float.class) {
                        object = new Double(((Float)object).floatValue());
                        list.add(new TypedValue(Double.class, object));
                    }
                } catch (Exception x) {
                    _logger.fine(value + " looked like a static reference but is not: " + x.getMessage());
                    // class reference?
                    guessClassReference(list, value.substring(5));
                }
            } else { // no '.' at all
                // class reference?
                guessClassReference(list, value.substring(5));
            }
        } else if (value.equals("true")) {
            list.add(new TypedValue(Boolean.class, Boolean.TRUE));
        } else if (value.equals("false")) {
            list.add(new TypedValue(Boolean.class, Boolean.FALSE));
        } else {
            // numbers? multiple possibilities
            try {
                list.add(new TypedValue(Integer.class, Integer.valueOf(value)));
            } catch (Exception x) {}
            try {
                list.add(new TypedValue(Long.class, Long.valueOf(value)));
            } catch (Exception x) {}
            try {
                list.add(new TypedValue(Float.class, Float.valueOf(value)));
            } catch (Exception x) {}
            try {
                list.add(new TypedValue(Double.class, Double.valueOf(value)));
            } catch (Exception x) {}
        }

        // always try String as the last resort
        list.add(new TypedValue(String.class, value));

        return list;
    }

    private boolean isPrimitiveType(String name) {
        return name.equals("boolean") || name.equals("boolean...") ||
               name.equals("byte")    || name.equals("byte...") ||
               name.equals("double")  || name.equals("double...") ||
               name.equals("float")   || name.equals("float...") ||
               name.equals("integer") || name.equals("integer...") ||
               name.equals("long")    || name.equals("long...") ||
               name.equals("short")   || name.equals("short...") ||
               name.equals("string")  || name.equals("string...") ||
               name.equals("class")   || name.equals("class...");
    }

    private String args(Object... property) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < property.length; ++i) sb.append(property[i].getClass().getName()).append(':').append(property[i]).append(' ');
        return sb.append(')').toString();
    }

    private void addSetProperty(Object bean, Class<?>[] type, String name, Object... property)
    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (name == null) {
            name = type[0].getSimpleName();
        }

        int dot;
        if ((dot = name.indexOf('.')) > -1) {
            _logger.fine("looking at a composite property " + name);
            do {
                bean = Beans.invoke(bean, "get" + name.substring(0, dot));
                name = Strings.capitalize(name.substring(dot + 1)); // lower case letters may be found after '.'
            } while ((dot = name.indexOf('.')) > -1);
        }

        // attempt property injection using setXXX(), addXXX(), set(), or add(), in that order
        try {
            _logger.fine(S.fine(_logger) ? "trying set" + name + "() on " + bean.getClass() + ": " + property.length + args(property) : null);
            Beans.invoke(bean, "set" + name, property);
            _logger.fine("... successful");
        } catch (NoSuchMethodException x) {
            try {
                _logger.fine(S.fine(_logger) ? "trying add" + name + "() on " + bean.getClass() + ": " + property.length + args(property) : null);
                Beans.invoke(bean, "add" + name, property);
                _logger.fine("... successful");
            } catch (NoSuchMethodException x1) {
                try {
                    _logger.fine(S.fine(_logger) ? "trying set() on " + bean.getClass() + ": " + property.length + args(property) : null);
                    Beans.invoke(bean, "set", property);
                    _logger.fine("... successful");
                } catch (NoSuchMethodException x2) {
                    _logger.fine(S.fine(_logger) ? "trying add() on " + bean.getClass() + ": " + property.length + args(property) : null);
                    Beans.invoke(bean, "add", property);
                    _logger.fine("... successful");
                }
            }
        }
    }

    private void injectProperty(Object bean, Class<?> type, Object property, String alias, TypedValueGroup arguments)
    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        NoSuchMethodException nsme = null;

        if (arguments != null && arguments.size() > 0) {
            Object[] props = new Object[arguments.size() + 1];
            props[0] = property;
            Class<?>[] types = new Class<?>[arguments.size() + 1];
            types[0] = type;

            arguments.reset();
            while (arguments.load(props, 1)) {
                try {
                    addSetProperty(bean, types, alias, props);
                    return;
                } catch (NoSuchMethodException x) {
                    // not a problem
                    nsme = x;
                }
            }
        } else {
            try {
                addSetProperty(bean, new Class<?>[] { type }, alias, property);
                return;
            } catch (NoSuchMethodException x) {
                // not a problem
                nsme = x;
            }
        }

        // now try the super classes
        Class<?>[] interfaces = type.getInterfaces();
        for (Class<?> face : interfaces) {
            try {
                injectProperty(bean, face, property, alias, arguments);
                return;
            } catch (NoSuchMethodException x) {
                continue;
            }
        }
        Class<?> supertype = type.getSuperclass();
        if (supertype != null) {
            injectProperty(bean, supertype, property, alias, arguments);
        } else {
            throw nsme;
        }
    }

    static class TypedValue {
        final Class<?> type; // required for boxed/unboxed primitives
        final Object data;

        TypedValue(Class<?> t, Object v) {
            type = t;
            data = v;
        }

        public String toString() {
            return "type="+type.getSimpleName()+",data="+data.getClass().getSimpleName()+':'+data;
        }
    }

    @SuppressWarnings("serial")
    static class TypedValues extends ArrayList<TypedValue> {
        final String name;

        public TypedValues(String n) {
            name = n;
        }
    }

    static class TypedValueGroup {
        private List<TypedValues> _values = new ArrayList<TypedValues>();
        private int[] _index;

        void add(TypedValues values) {
            _values.add(values);
        }

/*
        TypedValues shift() {
            return _values.remove(0);
        }

        TypedValues pop() {
            return _values.remove(_values.size()-1);
        }

*/
        TypedValues get(int index) {
            return _values.get(index);
        }

        TypedValueGroup complete() {
            _index = new int[_values.size()];
            return this;
        }

        TypedValueGroup reset() {
            for (int i = 0; i < _index.length; ++i) {
                _index[i] = 0;
            }
            return this;
        }

        int size() {
            return _values.size();
        }

        boolean load(Object[] props, int offset) {
            if (_values.size() > 0) {
                int p = _values.size() - 1;
                while (_index[p] > _values.get(p).size() - 1 && p > 0) {
                    _index[p] = 0;
                    --p;
                    ++_index[p];
                }
                if (_index[p] > _values.get(p).size() - 1) {
                    return false;
                }

//System.err.print("\t TypedValueGroup.load{");
                for (int i = 0; i < _values.size(); ++i) {
                    props[i+offset] = _values.get(i).get(_index[i]).data;
//System.err.print("[" + _index[i] + "](" + props[i+offset].getClass().getSimpleName() + ')');
                }
//System.err.println("}");
                ++_index[_values.size() - 1];
                return true;
            } else {
                return false;
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append('{');
            for (TypedValues typed: _values) {
                sb.append(typed);
            }
            return sb.append('}').toString();
        }
    }

    static class ElementInfo {
        Class<?> type;
        String name;
        Object data;
        Map<String, String> pkgs = new HashMap<String, String>(); // xmlns/packages defined on this element
        String jpkg; // the java package assigned to this element
        TypedValueGroup args = new TypedValueGroup();
        Map<String, String> inst = new HashMap<String, String>(); // assembly instructions

        public String toString() {
            int size = args.size();
            StringBuilder sb = new StringBuilder(name).append('@').append(hashCode()).append(':').append(size).append('\n');
            for (int i = 0; i < size; ++i) {
                sb.append(args.get(i).get(0)).append('\n');
            }
            return sb.toString();
        }
    }

    private final List<ElementInfo> _stack = new ArrayList<ElementInfo>();
    private final Map<String, ElementInfo> _local = new HashMap<String, ElementInfo>();
    private final StringBuffer _chars = new StringBuffer();

    private Object _top;

    /**
     * Returns the last assembled object.
     *
     * @return the last assembled object
     */
    public Object getBean() {
        return _top;
    }

    /**
     * Receive notification of the beginning of the document.
     */
    @Override
    public void startDocument() {
        //_logger.fine("startDocument");
    }

    /**
     * Receive notification of the end of the document.
     */
    @Override
    public void endDocument() {
        //_logger.fine("endDocument");
    }

    /**
     * Receive notification of the start of an element.
     */
    @Override
    public void startElement(String uri, String l, String q, Attributes a) {
        /*
         * 1. Load a class that matches the element name.
         * 2. If no class found, assume the element maps to a String.
         * 3. Otherwise, construct a new object of the class with element attributes.
         */
        _logger.fine(S.fine(_logger) ?
                    "Consider element " + l +
                  "\n             uri " + uri +
                  "\n               q " + q : null);
        ElementInfo info = new ElementInfo();

        // Record java packages defined on this element as xmlns
        for (int i = 0; i < a.getLength(); ++i) {
            _logger.fine(S.fine(_logger) ?
                    "            attr " + a.getQName(i) + "=" + a.getValue(i) +
                  "\n                 " + a.getQName(i) + ":" + a.getURI(i) : null);
            if (a.getQName(i).startsWith("xmlns:") && a.getValue(i).startsWith("java://")) {
                info.pkgs.put(a.getQName(i).substring(6), a.getValue(i).substring(7));
            }
        }

        // Resolve the package name of this element, which could be empty (default package)
        int colon = q.indexOf(':');
        if (colon > 0) {
            String xmlns = q.substring(0, colon);
            // is it defined right here?
            info.jpkg = info.pkgs.get(xmlns);
            // find a matching namespace from ancesters
            if (info.jpkg == null && !_stack.isEmpty()) {
                for (int i = _stack.size()-1; i >= 0; --i) {
                    info.jpkg = _stack.get(i).pkgs.get(xmlns);
                    if (info.jpkg != null) {
                        break;
                    }
                }
            }
        } else if (isPrimitiveType(q)) {
            info.jpkg = "java.lang";
        } else if (!_stack.isEmpty()) {
            info.jpkg = _stack.get(_stack.size()-1).jpkg;
        } else {
            info.jpkg = _jpkg;
        }

        _logger.fine("to create element with package = " + info.jpkg);
        try {
            info.name = (info.jpkg != null) ? info.jpkg + '.' + Strings.toCamelCase(l) : Strings.toCamelCase(l);
            try {
	            if (info.name.endsWith("...")) {
	            	// Array construction
	            	info.type = Class.forName(info.name.substring(0, info.name.length()-3));
	            	info.data = new ArrayList<Object>();
	            } else {
	            	// Non-array construction
	                int size = a.getLength();
	                TypedValueGroup arguments = new TypedValueGroup();
	                for (int i = 0; i < size; ++i) {
	                    if (!a.getQName(i).startsWith("xmlns:") && !a.getQName(i).equals("xmlns")) {
	                        arguments.add(guessUntypedValue(a.getQName(i), a.getValue(i)));
	                    }
	                }
	                arguments.complete();
                    _logger.fine(S.fine(_logger) ? "arguments=" + arguments : null);
	
	                if (arguments.size() > 0) {
	                    if (arguments.size() == 1 && "java.lang".equals(info.jpkg)) {
	                        info.inst.put("@as", Strings.toCamelCase(arguments.get(0).name, '-', false)); // respect original spelling
	                        info.data = arguments.get(0).get(0).data;
	                        info.type = arguments.get(0).get(0).type;
	                    } else {
	                        Exception last = null;
	                        Object[] args = new Object[arguments.size()];
	                        while (arguments.load(args, 0)) {
	                            try {
	                                _logger.fine(S.fine(_logger) ? "to create " + info.name + " with args: " + args.length + args(args) : null);
	                                info.data = _factory.create(info.name, args);
	                                info.type = info.data.getClass();
	                                break;
	                            } catch (InvocationTargetException x) {
	                                throw x;
	                            } catch (Exception x) {
	                                last = x;
                                    _logger.fine("failure in creating " + info.name + ": probing for other constructors");
	                            }
	                        }
	
	                        if (info.data == null) {
	                            throw last;
	                        }
	                    }
	                } else {
	                    _logger.fine("Create " + info.name + " with the default constructor");
	                    info.data = _factory.create(info.name);
	                    info.type = info.data.getClass();
	                }
	            }
            } catch (ClassNotFoundException x) {
                // no class by the element name is found, assumed String
                if (!_lenient) {
                    throw new BeanAssemblyException("No class associated with element " + q);
                } else {
					_logger.log(Level.WARNING, "can't find class " + info.name, x);
				}
            }
            _stack.add(info);
            //_logger.fine(">>ElementInfo: " + info.type.getName() + " in " + info);
            // all other exceptions indicate mismatches between the beans and the XML schema
        } catch (Exception x) {
            if (!_lenient) {
                throw new BeanAssemblyException("Failed to assemble bean from element " + q, x);
            } else {
				_logger.log(Level.SEVERE, "can't create object for this element", x);
			}
        }
    }

    /**
     * Receive notification of the end of an element.
     */
    @Override
    public void endElement(String uri, String l, String q) {
        /*
         * 1. If current element is a String, update its value from the string buffer.
         * 2. Add the element to parent.
         */
        ElementInfo element = _stack.remove(_stack.size()-1);
_logger.fine("endElement " + element);
        if (element.type == null) {
			_logger.warning("Element " + element.name + " not created ");
            return;
        } else if (_chars.length() > 0) {
            try {
                injectProperty(element.data, String.class, _chars.toString(), null, null);
            } catch (Exception x) {
				if (!_lenient) {
					throw new BeanAssemblyException("Failed to set characters to object " + element.type.getName(), x);
				} else {
					_logger.warning("Failed to set characters to parent " + element.data);
				}
            }
        }
        _chars.setLength(0);
        _logger.fine("<<ElementInfo: " + element.type.getName() + " in " + element +
                   "\n    @to is " + element.inst.get("@to") +
                   "\n    @as is " + element.inst.get("@as") +
                   "\n    @id is " + element.inst.get("@id"));

        if (List.class.isAssignableFrom(element.data.getClass()) && element.name.endsWith("...")) {
        	List<?> list = (List<?>)element.data;
        	Object array = Array.newInstance(element.type, list.size());
        	for (int i = 0; i < list.size(); ++i) {
        		Array.set(array, i, list.get(i));
        	}
        	element.data = array;
        }

        String id = element.inst.get("@id");
        if (id != null) {
            // locally stored object - not added to the parent
            _local.put(id, element);
        } else if (!_stack.isEmpty()) {
            // inject into the parent as a property
            ElementInfo parent = _stack.get(_stack.size()-1);
            _logger.fine("Parent is " + parent.data.getClass().getName());
            try {
                Object target = parent.data;
                String to = element.inst.get("@to");
                if (to != null) {
                    target = parent.data.getClass().getMethod("get" + Strings.toCamelCase(to, '-', false)).invoke(parent.data);
                }
                String as = element.inst.get("@as");
                if (as != null) {
                    injectProperty(target, element.type, element.data, Strings.toCamelCase(as, '-', false), element.args.complete());
                } else {
                    injectProperty(target, element.type, element.data, null, element.args.complete());
                }
            } catch (Exception x) {
				if (!_lenient) {
					throw new BeanAssemblyException("Failed to set value " + element.data + " to parent " + parent.data, x);
				} else {
					_logger.log(Level.WARNING, "Failed to set value " + element.data + " to parent " + parent.data, x);
				}
            }
        }
        _top = element.data;
    }

    /**
     * Receive notification of character data inside an element.
     */
    @Override
    public void characters (char ch[], int start, int len) {
        while (len > 0 && Character.isWhitespace(ch[start])) {
            ++start;
            --len;
        }
        while (len > 0 && Character.isWhitespace(ch[start+len-1])) {
            --len;
        }
        if (len > 0) {
            if (_chars.length() > 0) {
                _chars.append(' ');
            }
            _chars.append(ch, start, len);
        }
    }

    private static final Pattern PROCESSING_INSTRUCTION = Pattern.compile("(@?[\\w_]+) *= *\"([^\"]+)\"");

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        _logger.fine("Processing Instruction " + target);
        _logger.fine("Processing Instruction data: " + data);
        if (target.equals("assemble")) {
            if (!_stack.isEmpty()) {
                ElementInfo element = _stack.get(_stack.size()-1);
                Matcher matcher = PROCESSING_INSTRUCTION.matcher(data);
                while (matcher.find()) {
                    if (matcher.groupCount() == 2) {
                        String name = matcher.group(1);
                        if (name.charAt(0) == '@') {
                            element.inst.put(name, matcher.group(2));
                        } else {
                            element.args.add(guessUntypedValue(name, matcher.group(2)));
                        }
                        _logger.fine("Processing Instruction for " + element.data.getClass() +
                                   "\n\ttarget = " + target +
                                   "\n\t" + name + "=" + matcher.group(2));
                    }
                }
            }
        }
    }

    /**
     * Receive notification of a parser warning.
     */
    @Override
    public void warning (SAXParseException e) throws SAXException {
        throw e;
    }


    /**
     * Receive notification of a recoverable parser error.
     */
    @Override
    public void error (SAXParseException e) throws SAXException {
        throw e;
    }
    /**
     * Report a fatal XML parsing error.
     */
    @Override
    public void fatalError (SAXParseException e) throws SAXException {
        throw e;
    }
}
