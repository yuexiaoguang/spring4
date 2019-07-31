package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.web.context.ServletContextAware;

/**
 * 检索特定的ServletContext init参数的{@link FactoryBean} (即{@code web.xml}中定义的"context-param").
 * 在用作bean引用时公开ServletContext init参数, 有效地使其可用作命名的Spring bean实例.
 *
 * <p><b>NOTE:</b> 从Spring 3.0开始, 还可以使用Map类型的"contextParameters"默认bean,
 * 并使用"#{contextParameters.myKey}"表达式取消引用它, 以按名称访问特定参数.
 */
public class ServletContextParameterFactoryBean implements FactoryBean<String>, ServletContextAware {

	private String initParamName;

	private String paramValue;


	/**
	 * 设置要公开的ServletContext init参数的名称.
	 */
	public void setInitParamName(String initParamName) {
		this.initParamName = initParamName;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.initParamName == null) {
			throw new IllegalArgumentException("initParamName is required");
		}
		this.paramValue = servletContext.getInitParameter(this.initParamName);
		if (this.paramValue == null) {
			throw new IllegalStateException("No ServletContext init parameter '" + this.initParamName + "' found");
		}
	}


	@Override
	public String getObject() {
		return this.paramValue;
	}

	@Override
	public Class<String> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
