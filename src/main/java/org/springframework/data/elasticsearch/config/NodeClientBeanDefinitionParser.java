package org.springframework.data.elasticsearch.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;
import org.w3c.dom.Element;

/**
 * @author: withccm
 * https://github.com/spring-projects/spring-data-elasticsearch/pull/170
 */
public class NodeClientBeanDefinitionParser extends AbstractBeanDefinitionParser {

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(NodeClientFactoryBean.class);
        setLocalSettings(element, builder);
        return getSourcedBeanDefinition(builder, element, parserContext);
    }

    private void setLocalSettings(Element element, BeanDefinitionBuilder builder) {
        builder.addPropertyValue("local", Boolean.valueOf(element.getAttribute("local")));
        builder.addPropertyValue("clusterName", element.getAttribute("cluster-name"));
        builder.addPropertyValue("enableHttp", Boolean.valueOf(element.getAttribute("http-enabled")));
        builder.addPropertyValue("pathData", element.getAttribute("path-data"));
        builder.addPropertyValue("pathHome", element.getAttribute("path-home"));
        builder.addPropertyValue("pathConfiguration", element.getAttribute("path-configuration"));
    }

    private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source,
                                                            ParserContext context) {
        AbstractBeanDefinition definition = builder.getBeanDefinition();
        definition.setSource(context.extractSource(source));
        return definition;
    }
}
