package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.Aware;

/**
 * 由任何希望获取其运行的{@link ServletContext} (通常由{@link WebApplicationContext}确定)的对象实现的接口.
 */
public interface ServletContextAware extends Aware {

	/**
	 * 设置此对象运行的{@link ServletContext}.
	 * <p>在普通bean属性填充之后但在初始化回调之前调用, 例如InitializingBean的{@code afterPropertiesSet}或自定义init方法.
	 * 在ApplicationContextAware的{@code setApplicationContext}之后调用.
	 * 
	 * @param servletContext 此对象使用的ServletContext对象
	 */
	void setServletContext(ServletContext servletContext);

}
