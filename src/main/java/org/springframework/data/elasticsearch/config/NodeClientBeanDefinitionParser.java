/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.elasticsearch.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;
import org.w3c.dom.Element;

/**
 * NodeClientBeanDefinitionParser
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
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
	}

	private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source,
															ParserContext context) {
		AbstractBeanDefinition definition = builder.getBeanDefinition();
		definition.setSource(context.extractSource(source));
		return definition;
	}
}
