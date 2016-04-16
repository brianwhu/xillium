package org.xillium.core.spring;

import java.util.logging.*;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;


public class XilliumNamespaceHandler extends NamespaceHandlerSupport {
    private static final Logger _logger = Logger.getLogger(XilliumNamespaceHandler.class.getName());

    public void init() {
        _logger.info("registerBeanDefinitionDecorator: XilliumPropertyDecorator");
        registerBeanDefinitionDecorator("property", new XilliumPropertyDecorator());        
    }
}

