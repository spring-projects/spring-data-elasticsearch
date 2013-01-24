package org.springframework.data.elasticsearch.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;
import org.w3c.dom.Element;


public class NodeClientBeanDefinitionParser extends AbstractBeanDefinitionParser {

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(NodeClientFactoryBean.class);
        setLocalSettings(element,builder);
        return getSourcedBeanDefinition(builder, element, parserContext);
    }

    private void setLocalSettings(Element element, BeanDefinitionBuilder builder) {
        builder.addPropertyValue("local", Boolean.valueOf(element.getAttribute("local")));
    }


    private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source,
                                                            ParserContext context) {
        AbstractBeanDefinition definition = builder.getBeanDefinition();
        definition.setSource(context.extractSource(source));
        return definition;
    }
}
