package org.xillium.data.validation;

import org.xillium.data.DataObject;
import java.lang.reflect.*;
import java.util.*;


/**
 * A Reifier of extended types for data reification and validation.
 */
@lombok.extern.log4j.Log4j2
public class Reifier {
    protected static final Map<Class<?>, Map<String, Validator>> _cached = new HashMap<Class<?>, Map<String, Validator>>();

    public static synchronized Validator cache(Class<?> type, String name, Validator validator) {
        Map<String, Validator> validators = _cached.get(type);
        if (validators == null) {
            _cached.put(type, validators = new HashMap<String, Validator>());
        }
        validators.put(name, validator);
        return validator;
    }

    public static synchronized Validator find(Class<?> type, String name) {
        Map<String, Validator> validators = _cached.get(type);
        return validators != null ? validators.get(name) : null;
    }

    Map<String, Validator> _named = new HashMap<String, Validator>();
    
    /**
     * Adds a set of data type specifications.
     * @param spec - a class that defines data types as member fields
     */
    public Reifier addTypeSet(Class<?> spec) {
        for (Field field: spec.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())) {
                String name = field.getName();
                try {
                    _named.put(name, new Validator(name, field.getType(), field));
                } catch (IllegalArgumentException x) {
                    _log.trace("Ignored {}: {}", name, x.getMessage());
                }
            } else {
                _log.trace("Ignored non-public field: {}", field.getName());
            }
        }

        return this;
    }

    public Map<String, Validator> getValidators() {
        return _named;
    }

    /**
     * Populates a data object by collecting and validating the named values from a Map&lt;String, String&gt;.
     *
     * @throws EmptyDataObjectException if a required member is missing and the data object is empty
     * @throws MissingParameterException if a required member is missing but the data object has other members
     * @throws DataValidationException if all other data validation fails
     * @throws SecurityException if the data object is inproperly designed
     */
    public <T extends DataObject> T collect(T data, Map<String, String> binder) throws SecurityException, DataValidationException {
        return collect(data, binder, null);
    }

    private final <T extends DataObject> T collect(T data, Map<String, String> binder, String prefix) throws SecurityException, DataValidationException {
        int present = 0;
        String absent = null;

        for (Field field: data.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;

            Class<?> ftype = field.getType();
            String name = field.getName();
            String qualified = prefix != null ? prefix + '.' + name : name;
            _log.trace("collect(): qualified = {}", qualified);

            if (ftype.isArray()) {
                _log.trace("collect(): field is an array");
                ArrayList<Object> list = new ArrayList<Object>();
                Class<?> ctype = ftype.getComponentType();
                
                if (DataObject.class.isAssignableFrom(ctype)) {
                    _log.trace("collect(): DataObject array");
                    for (int index = 0; true; ++index) {
                        try {
                            list.add(collect((DataObject)ctype.newInstance(), binder, qualified + '[' + index + ']'));
                        } catch (EmptyDataObjectException x) {
                            _log.trace("DataObject array '{}': no more elements", qualified);
                            break;
                        } catch (InstantiationException x) {
                            throw new ValidationSpecificationException("Impossible to instantiate " + ctype.getName(), x);
                        } catch (IllegalAccessException x) {
                            throw new ValidationSpecificationException("Impossible to instantiate " + ctype.getName(), x);
                        }
                    }
                } else {
                    _log.trace("collect(): simple array");
                    for (int index = 0; true; ++index) {
                        String text = binder.get(qualified + '[' + index + ']');
                        if (text != null) {
                            list.add(translate(field, name, text));
                        } else {
                            _log.trace("Simple array '{}': no more elements", qualified);
                            break;
                        }
                    }
                }
                _log.trace("collect(): array - get elements {}", list.size());
                if (list.size() > 0) {
                    _log.trace("Storing array '{}' with length {}", qualified, list.size());
                    try {
                        field.set(data, list.toArray((Object[])Array.newInstance(ctype, list.size())));
                    } catch (IllegalAccessException x) {
                        // should not happen
                        throw new RuntimeException("While setting array field " + field, x);
                    }
                    _log.trace("Array '{}' stored", qualified );
                } else if (field.getAnnotation(required.class) != null) {
                    throw new MissingParameterException(
                        name, (prefix != null ? prefix : "") + '(' + data.getClass().getName() + ')'
                    );
                } else {
                    continue;
                }
            } else if (DataObject.class.isAssignableFrom(ftype)) {
                try {
                    field.set(data, collect((DataObject)ftype.newInstance(), binder, qualified));
                } catch (EmptyDataObjectException x) {
                    if (isRequired(data, field, prefix, name, present)) {
                        absent = name;
                    }
                    continue;
                } catch (InstantiationException x) {
                    throw new ValidationSpecificationException("Impossible to instantiate " + ftype.getName(), x);
                } catch (IllegalAccessException x) {
                    throw new RuntimeException("While setting field " + field, x);
                }
            } else {
                String text = binder.get(qualified);
                if (text != null && text.length() > 0) {
                    if (absent != null) {
                        // now report missing required parameters
                        throw new MissingParameterException(
                            absent, (prefix != null ? prefix : "") + '('+data.getClass().getName()+')'
                        );
                    } else {
                        try {
                            field.set(data, translate(field, name, text));
                        } catch (IllegalAccessException x) {
                            throw new RuntimeException("While setting field " + field, x);
                        }
                    }
                } else {
                    Object prefill = null;
                    try {
                        prefill = field.get(data);
                    } catch (Throwable t) {}
                    if (prefill != null) {
                        // re-translate the value, passing it through validation
                        try {
                            translate(field, name, String.valueOf(prefill));
                        } catch (Exception x) {
                            prefill = null;
                        }
                    }
                    if (prefill == null && isRequired(data, field, prefix, name, present)) {
                        absent = name;
                    }
                    continue;
                }
            }

            _log.trace("Got {}", name);
            ++present;
        }

        // EmptyDataObjectException should never be thrown for the top-level object (where prefix == null)
        if (present == 0 && prefix != null) {
            throw new EmptyDataObjectException(prefix);
        } else if (prefix == null && absent != null) {
            throw new MissingParameterException(absent, "(" + data.getClass().getName()+')');
        } else {
            return data;
        }
    }

    /*!
     * Translates a text string to a value of the appropriate type for the given field.
     */
    private final Object translate(Field field, String name, String text) throws DataValidationException {
        Object value = null;

        try {
            // in-place validator first
            Class<?> type = field.getType();
            if (type.isArray()) {
                type = type.getComponentType();
            }

            Validator inplaceValidator = find(field.getDeclaringClass(), name);
            if (inplaceValidator == null) {
                _log.trace("New Validator for type {}", type);
                inplaceValidator = cache(field.getDeclaringClass(), name, new Validator(name, type, field));
            }
            /*
            _log.trace("New Validator for type " + type);
            Validator inplaceValidator = new Validator(name, type, field);
            */

            // validations in extended-type?
            subtype restriction = field.getAnnotation(subtype.class);
            Validator namedValidator = restriction != null ? _named.get(restriction.value()) : null;

            if (namedValidator != null) {
                inplaceValidator.preValidate(text);
                value = namedValidator.parse(text);
                inplaceValidator.postValidate(value);
            } else {
                value = inplaceValidator.parse(text);
            }
        } catch (IllegalArgumentException x) {
            throw new ValidationSpecificationException(field.getDeclaringClass().getSimpleName() + '.' + name, x);
        }

        return value;
    }

    /*!
     * Tests whether the specified field is a required field.
     *
     * @param data - the containing data object
     * @param field - the field
     * @param prefix - the current prefix indicating the name of the containing data object
     * @param name - the cached name of the field
     * @param present - the number of data members already having values
     *
     * @throws MissingParameterException if the field is required and there's already another member present (present > 0)
     * @return true if the field is required, false otherwise
     */
    private static final boolean isRequired(DataObject data, Field field, String prefix, String name, int present)
    throws MissingParameterException {
        if (field.getAnnotation(required.class) != null) {
            //if (present == 0) {
            if (present == 0 && prefix != null) { // Brian 3/9/2012
                // hold the exception report as long as the data object is empty
                _log.trace("Hold the exception report on field {}", name);
                return true;
            } else {
                _log.trace("Data object already has {} member values", present );
                throw new MissingParameterException(
                    name, (prefix != null ? prefix : "") + '(' + data.getClass().getName() + ')'
                );
            }
        } else {
            return false;
        }
    }
}
