package org.springframework.web.portlet.context;

import javax.portlet.PortletContext;

import org.springframework.beans.factory.Aware;

/**
 * 希望获得在其中运行的PortletContext通知的对象 (通常由PortletApplicationContext确定).
 */
public interface PortletContextAware extends Aware {

	/**
	 * 设置此对象运行的PortletContext.
	 * <p>在普通bean属性的填充之后但在初始化回调之前调用, 例如InitializingBean的afterPropertiesSet或自定义init方法.
	 * 在ApplicationContextAware的setApplicationContext之后调用.
	 * 
	 * @param portletContext 此对象使用的PortletContext对象
	 */
	void setPortletContext(PortletContext portletContext);

}
