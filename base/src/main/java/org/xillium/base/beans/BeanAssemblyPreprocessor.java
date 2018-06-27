package org.xillium.base.beans;

import org.xillium.base.Functor;


/**
 * Implementations of BeanAssemblyPreprocessor will be invoked on every sub elements encountered during assembly.
 */
public interface BeanAssemblyPreprocessor extends Functor<Void, Object> {
}
