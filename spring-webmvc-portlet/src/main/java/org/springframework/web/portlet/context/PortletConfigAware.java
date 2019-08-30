package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;

import org.springframework.beans.factory.Aware;

/**
 * 希望获得在其中运行的PortletConfig通知的任何对象实现的接口 (通常由PortletApplicationContext确定).
 */
public interface PortletConfigAware extends Aware {

	/**
	 * 设置此对象运行的PortletConfig.
	 * <p>在普通bean属性的填充之后但在初始化回调之前调用, 例如InitializingBean的afterPropertiesSet或自定义init方法.
	 * 在ApplicationContextAware的setApplicationContext之后调用.
	 * 
	 * @param portletConfig 此对象使用的PortletConfig对象
	 */
	void setPortletConfig(PortletConfig portletConfig);

}
