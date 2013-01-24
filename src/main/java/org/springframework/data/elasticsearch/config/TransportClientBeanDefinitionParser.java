package org.springframework.data.elasticsearch.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.elasticsearch.client.TransportClientFactoryBean;
import org.w3c.dom.Element;

import static org.apache.commons.lang.StringUtils.split;


public class TransportClientBeanDefinitionParser extends AbstractBeanDefinitionParser {

    private static final String SEPARATOR_CHARS = ",";

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TransportClientFactoryBean.class);
        setClusterNodes(element, builder);
        return getSourcedBeanDefinition(builder,element, parserContext);
    }

    private void setClusterNodes(Element element, BeanDefinitionBuilder builder){
        builder.addPropertyValue("clusterNodes", split(element.getAttribute("cluster-nodes"), SEPARATOR_CHARS));
    }

    private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source,
                                                            ParserContext context) {
        AbstractBeanDefinition definition = builder.getBeanDefinition();
        definition.setSource(context.extractSource(source));
        return definition;
    }
}
