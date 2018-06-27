package org.xillium.base.model;

import java.util.Map;
import org.xillium.base.beans.BeanAssemblyPreprocessor;
import org.xillium.base.util.EnvironmentReference;
import org.xillium.base.util.EnvironmentAware;


/**
 * An xml-assembly oriented collection of Objects under unique names. A collection can be encoded in XML as following.
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <model:object-assembly xmlns:model="java://org.xillium.base.model" xmlns:my-ns="java://my.object.package">
 *   ...
 * </model:object-assembly>
 * }</pre>
 * Loading the assembly can be achived by the following Java code.
 * <pre>{@code
 * Map<String, Address> addresses = new HashMap<>();
 * XMLBeanAssembler addrAssembler = new XMLBeanAssembler(new BurnedInArgumentsObjectFactory(ObjectAssembly.class, addresses, "main"));
 * addrAssembler.build(ObjectAssemblyTest.class.getResourceAsStream("/address-assembly.xml"));
 * // use addresses ...
 * }</pre>
 */
public class ObjectAssembly<T> implements BeanAssemblyPreprocessor, EnvironmentAware {
    private final Map<String, T> _collection;
    private final String _namespace;
    private EnvironmentReference _reference;

    public ObjectAssembly(Map<String, T> collection, String namespace) {
        _collection = collection;
        _namespace = namespace;
    }

    public void add(T object, String name) {
        _collection.put(_namespace + '/' + name, object);
    }

    @Override
    public void setEnvironmentReference(EnvironmentReference reference) {
        _reference = reference;
    }

    @Override
    public Void invoke(Object component) {
        if (component instanceof EnvironmentAware) {
            EnvironmentAware.class.cast(component).setEnvironmentReference(_reference);
        }
        return null;
    }
}
