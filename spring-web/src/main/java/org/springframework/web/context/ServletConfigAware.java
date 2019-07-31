package org.springframework.web.context;

import javax.servlet.ServletConfig;

import org.springframework.beans.factory.Aware;

/**
 * 由任何希望收到{@link ServletConfig} (通常由{@link WebApplicationContext}确定)通知的对象实现的接口.
 *
 * <p>Note: 仅在特定于Servlet的WebApplicationContext中实际运行时才满足. 否则, 不会设置ServletConfig.
 */
public interface ServletConfigAware extends Aware {

	/**
	 * 设置此对象运行的{@link ServletConfig}.
	 * <p>在普通bean属性填充之后但在初始化回调之前调用, 例如InitializingBean的{@code afterPropertiesSet}或自定义init方法.
	 * 在ApplicationContextAware的{@code setApplicationContext}之后调用.
	 * 
	 * @param servletConfig 此对象使用的ServletConfig对象
	 */
	void setServletConfig(ServletConfig servletConfig);

}
