package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link FactoryBean} that retrieves a specific ServletContext init parameter
 * (that is, a "context-param" defined in {@code web.xml}).
 * Exposes that ServletContext init parameter when used as bean reference,
 * effectively making it available as named Spring bean instance.
 *
 * <p><b>NOTE:</b> As of Spring 3.0, you may also use the "contextParameters" default
 * bean which is of type Map, and dereference it using an "#{contextParameters.myKey}"
 * expression to access a specific parameter by name.
 */
public class ServletContextParameterFactoryBean implements FactoryBean<String>, ServletContextAware {

	private String initParamName;

	private String paramValue;


	/**
	 * Set the name of the ServletContext init parameter to expose.
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
