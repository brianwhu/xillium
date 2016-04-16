package org.xillium.core.spring;

import java.util.logging.*;
//import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
//import org.springframework.util.StringUtils;
import org.w3c.dom.*;
import org.xillium.base.beans.Beans;



public class XilliumPropertyDecorator implements BeanDefinitionDecorator {
    private static final Logger _logger = Logger.getLogger(XilliumPropertyDecorator.class.getName());

    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext context) {
        NamedNodeMap attrs = node.getAttributes();
        String name = attrs.getNamedItem("name").getTextContent();
        _logger.info("Injecting, node.hasAttributes = " + node.hasAttributes());
        _logger.info("           node.getAttributes = " + node.getAttributes());
        _logger.info("           node.hasChildNodes = " + node.hasChildNodes());
        _logger.info("           node.name = " + node.getNodeName());
        _logger.info("           node.text = " + node.getTextContent());
        _logger.info("           bean = " + definition);
        _logger.info("        context = " + context);
        _logger.info("  property.name = " + name);
        _logger.info("          value = " + definition.getBeanDefinition().getPropertyValues());
        _logger.info("          value = " + attrs.getNamedItem("value"));
        _logger.info("            ref = " + attrs.getNamedItem("ref"));

        Node value;

        if ((value = attrs.getNamedItem("value")) != null) {
            _logger.info("\tvalue = " + value.getTextContent());
        } else if ((value = attrs.getNamedItem("ref")) != null) {
            _logger.info("\t  ref = " + value.getTextContent());
        } else if (node.hasChildNodes()) {
            _logger.info("\t  ref = " + node.getFirstChild().getTextContent());
        } else {
            _logger.info("\tvalue = " + node.getTextContent());
        }
        return definition;
    }
}

