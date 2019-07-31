package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link FactoryBean}获取特定的现有ServletContext属性.
 * 在用作bean引用时公开ServletContext属性, 有效地使其可用作命名的Spring bean实例.
 *
 * <p>旨在链接Spring应用程序上下文启动之前存在的ServletContext属性.
 * 通常, 这些属性将由第三方Web框架提供.
 * 在纯粹基于Spring的Web应用程序中, 不需要ServletContext属性的这种链接.
 *
 * <p><b>NOTE:</b> 从Spring 3.0开始, 还可以使用Map类型的"contextAttributes"默认bean,
 * 并使用"#{contextAttributes.myKey}"表达式取消引用它, 以按名称访问特定属性.
 */
public class ServletContextAttributeFactoryBean implements FactoryBean<Object>, ServletContextAware {

	private String attributeName;

	private Object attribute;


	/**
	 * 设置要公开的ServletContext属性的名称.
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.attributeName == null) {
			throw new IllegalArgumentException("Property 'attributeName' is required");
		}
		this.attribute = servletContext.getAttribute(this.attributeName);
		if (this.attribute == null) {
			throw new IllegalStateException("No ServletContext attribute '" + this.attributeName + "' found");
		}
	}


	@Override
	public Object getObject() throws Exception {
		return this.attribute;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.attribute != null ? this.attribute.getClass() : null);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
