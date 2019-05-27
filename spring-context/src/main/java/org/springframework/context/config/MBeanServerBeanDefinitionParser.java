package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.WebSphereMBeanServerFactoryBean;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * &lt;context:mbean-server/&gt; 元素的解析器.
 *
 * <p>在上下文中注册{@link org.springframework.jmx.export.annotation.AnnotationMBeanExporter}的实例.
 */
class MBeanServerBeanDefinitionParser extends AbstractBeanDefinitionParser {

	private static final String MBEAN_SERVER_BEAN_NAME = "mbeanServer";

	private static final String AGENT_ID_ATTRIBUTE = "agent-id";


	private static final boolean weblogicPresent = ClassUtils.isPresent(
			"weblogic.management.Helper", MBeanServerBeanDefinitionParser.class.getClassLoader());

	private static final boolean webspherePresent = ClassUtils.isPresent(
			"com.ibm.websphere.management.AdminServiceFactory", MBeanServerBeanDefinitionParser.class.getClassLoader());


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		String id = element.getAttribute(ID_ATTRIBUTE);
		return (StringUtils.hasText(id) ? id : MBEAN_SERVER_BEAN_NAME);
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String agentId = element.getAttribute(AGENT_ID_ATTRIBUTE);
		if (StringUtils.hasText(agentId)) {
			RootBeanDefinition bd = new RootBeanDefinition(MBeanServerFactoryBean.class);
			bd.getPropertyValues().add("agentId", agentId);
			return bd;
		}
		AbstractBeanDefinition specialServer = findServerForSpecialEnvironment();
		if (specialServer != null) {
			return specialServer;
		}
		RootBeanDefinition bd = new RootBeanDefinition(MBeanServerFactoryBean.class);
		bd.getPropertyValues().add("locateExistingServerIfPossible", Boolean.TRUE);

		// 标记为基础结构bean并附加源位置.
		bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		bd.setSource(parserContext.extractSource(element));
		return bd;
	}

	static AbstractBeanDefinition findServerForSpecialEnvironment() {
		if (weblogicPresent) {
			RootBeanDefinition bd = new RootBeanDefinition(JndiObjectFactoryBean.class);
			bd.getPropertyValues().add("jndiName", "java:comp/env/jmx/runtime");
			return bd;
		}
		else if (webspherePresent) {
			return new RootBeanDefinition(WebSphereMBeanServerFactoryBean.class);
		}
		else {
			return null;
		}
	}

}
