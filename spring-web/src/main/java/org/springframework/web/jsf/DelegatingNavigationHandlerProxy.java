package org.springframework.web.jsf;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.WebApplicationContext;

/**
 * JSF NavigationHandler实现, 它委托给从Spring root WebApplicationContext获取的NavigationHandler bean.
 *
 * <p>在{@code faces-config.xml}文件中配置此处理器代理, 如下所示:
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;navigation-handler&gt;
 * 	   org.springframework.web.jsf.DelegatingNavigationHandlerProxy
 *   &lt;/navigation-handler&gt;
 *   ...
 * &lt;/application&gt;</pre>
 *
 * 默认, 将在bean名称"jsfNavigationHandler"下搜索Spring ApplicationContext以查找NavigationHandler.
 * 在最简单的情况下, 这是一个如下所示的简单的Spring bean定义.
 * 但是, 所有Spring的bean配置能力都可以应用于这样的bean, 特别是所有类型的依赖注入.
 *
 * <pre class="code">
 * &lt;bean name="jsfNavigationHandler" class="mypackage.MyNavigationHandler"&gt;
 *   &lt;property name="myProperty" ref="myOtherBean"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 目标NavigationHandler bean通常会扩展标准的JSF NavigationHandler类.
 * 但是, 请注意, 在这种情况下, 不支持装饰原始的NavigationHandler (JSF提供者的默认处理器),
 * 因为我们无法以标准JSF样式注入原始处理器 (即, 作为构造函数参数).
 *
 * <p>对于<b>装饰原始NavigationHandler</b>, 确保目标bean扩展了Spring的<b>DecoratingNavigationHandler</b>类.
 * 这允许将原始处理器作为方法参数传递, 此代理自动检测该参数.
 * 请注意, DecoratingNavigationHandler子类仍将作为标准JSF NavigationHandler工作!
 *
 * <p>此代理可以子类化, 更改用于搜索导航处理器的bean名称, 更改用于获取目标处理器的策略,
 * 或更改用于访问ApplicationContext的策略 (通常通过{@link FacesContextUtils#getWebApplicationContext(FacesContext)}获取).
 */
public class DelegatingNavigationHandlerProxy extends NavigationHandler {

	/**
	 * Spring应用程序上下文中目标bean的默认名称: "jsfNavigationHandler"
	 */
	public final static String DEFAULT_TARGET_BEAN_NAME = "jsfNavigationHandler";

	private NavigationHandler originalNavigationHandler;


	public DelegatingNavigationHandlerProxy() {
	}

	/**
	 * @param originalNavigationHandler 原始NavigationHandler
	 */
	public DelegatingNavigationHandlerProxy(NavigationHandler originalNavigationHandler) {
		this.originalNavigationHandler = originalNavigationHandler;
	}


	/**
	 * 通过委托给Spring应用程序上下文中的目标bean, 处理由指定参数隐含的导航请求.
	 * <p>目标bean需要扩展JSF NavigationHandler类.
	 * 如果它扩展了Spring的DecoratingNavigationHandler,
	 * 那么将使用带有原始NavigationHandler作为参数的重载{@code handleNavigation}方法.
	 * 否则, 将调用标准的{@code handleNavigation}方法.
	 */
	@Override
	public void handleNavigation(FacesContext facesContext, String fromAction, String outcome) {
		NavigationHandler handler = getDelegate(facesContext);
		if (handler instanceof DecoratingNavigationHandler) {
			((DecoratingNavigationHandler) handler).handleNavigation(
					facesContext, fromAction, outcome, this.originalNavigationHandler);
		}
		else {
			handler.handleNavigation(facesContext, fromAction, outcome);
		}
	}

	/**
	 * 返回委托给的目标NavigationHandler.
	 * <p>默认情况下, 对于每次调用, 都会从Spring root WebApplicationContext获取名为"jsfNavigationHandler"的bean.
	 * 
	 * @param facesContext 当前的JSF上下文
	 * 
	 * @return 要委托给的目标NavigationHandler
	 */
	protected NavigationHandler getDelegate(FacesContext facesContext) {
		String targetBeanName = getTargetBeanName(facesContext);
		return getBeanFactory(facesContext).getBean(targetBeanName, NavigationHandler.class);
	}

	/**
	 * 返回BeanFactory中目标NavigationHandler bean的名称.
	 * 默认为"jsfNavigationHandler".
	 * 
	 * @param facesContext 当前的JSF上下文
	 * 
	 * @return 目标bean的名称
	 */
	protected String getTargetBeanName(FacesContext facesContext) {
		return DEFAULT_TARGET_BEAN_NAME;
	}

	/**
	 * 检索将bean名称解析委托给的Spring BeanFactory.
	 * <p>默认实现委托给{@code getWebApplicationContext}.
	 * 可以重写以提供任意BeanFactory引用来解析; 通常, 这将是一个完整的Spring ApplicationContext.
	 * 
	 * @param facesContext 当前的JSF上下文
	 * 
	 * @return the Spring BeanFactory (never {@code null})
	 */
	protected BeanFactory getBeanFactory(FacesContext facesContext) {
		return getWebApplicationContext(facesContext);
	}

	/**
	 * 检索将bean名称解析委托给的Web应用程序上下文.
	 * <p>默认实现委托给FacesContextUtils.
	 * 
	 * @param facesContext 当前的JSF上下文
	 * 
	 * @return Spring Web应用程序上下文 (never {@code null})
	 */
	protected WebApplicationContext getWebApplicationContext(FacesContext facesContext) {
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
