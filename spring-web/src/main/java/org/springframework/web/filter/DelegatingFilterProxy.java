package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 标准Servlet过滤器的代理, 委托给实现Filter接口的Spring管理的bean.
 * 支持{@code web.xml}中的"targetBeanName"过滤器init-param, 在Spring应用程序上下文中指定目标bean的名称.
 *
 * <p>{@code web.xml}通常包含{@code DelegatingFilterProxy}定义,
 * 其中指定的{@code filter-name}对应于Spring的根应用程序上下文中的bean名称.
 * 然后, 所有对过滤器代理的调用都将委托给Spring上下文中的那个bean, 这是实现标准Servlet过滤器接口所必需的.
 *
 * <p>这种方法对于具有复杂设置需求的Filter实现特别有用, 允许将完整的Spring bean定义机制应用于Filter实例.
 * 或者, 考虑标准的Filter设置以及从Spring root应用程序上下文中查找服务bean.
 *
 * <p><b>NOTE:</b> Servlet Filter接口定义的生命周期方法默认情况下<i>不</i>被委托给目标bean,
 * 依赖Spring应用程序上下文来管理该bean的生命周期.
 * 将"targetFilterLifecycle"过滤器init-param指定为"true"将强制在目标bean上调用 {@code Filter.init}
 * 和{@code Filter.destroy}生命周期方法, 让servlet容器管理过滤器生命周期.
 *
 * <p>从Spring 3.1开始, {@code DelegatingFilterProxy}已经更新,
 * 以便在使用Servlet 3.0的基于实例的过滤器注册方法时可选地接受构造函数参数,
 * 通常与Spring 3.1的{@link org.springframework.web.WebApplicationInitializer} SPI结合使用.
 * 这些构造函数允许直接提供委托Filter bean, 或者提供应用程序上下文和要获取的bean名称, 从而无需从ServletContext查找应用程序上下文.
 *
 * <p>这个类最初的灵感来自Spring Security的{@code FilterToBeanProxy}类, 由Ben Alex编写.
 */
public class DelegatingFilterProxy extends GenericFilterBean {

	private String contextAttribute;

	private WebApplicationContext webApplicationContext;

	private String targetBeanName;

	private boolean targetFilterLifecycle = false;

	private volatile Filter delegate;

	private final Object delegateMonitor = new Object();


	/**
	 * 对于{@code web.xml}中的传统 (pre-Servlet 3.0)使用.
	 */
	public DelegatingFilterProxy() {
	}

	/**
	 * 需要与Spring应用程序上下文交互, 指定{@linkplain #setTargetBeanName 目标bean名称}等.
	 * <p>用于Servlet 3.0+环境, 支持基于实例的过滤器注册.
	 * 
	 * @param delegate 此代理将委托给和管理生命周期的{@code Filter}实例 (不能是{@code null}).
	 */
	public DelegatingFilterProxy(Filter delegate) {
		Assert.notNull(delegate, "Delegate Filter must not be null");
		this.delegate = delegate;
	}

	/**
	 * 创建一个新的{@code DelegatingFilterProxy},
	 * 它将从{@code ServletContext}中的Spring {@code WebApplicationContext}中检索命名的目标bean
	 * ('root'应用程序上下文或{@link #setContextAttribute}命名的上下文).
	 * <p>用于Servlet 3.0+环境, 支持基于实例的过滤器注册.
	 * <p>目标bean必须实现标准的Servlet过滤器.
	 * 
	 * @param targetBeanName 要在Spring应用程序上下文中查找的目标过滤器bean的名称 (不能是{@code null}).
	 */
	public DelegatingFilterProxy(String targetBeanName) {
		this(targetBeanName, null);
	}

	/**
	 * 创建一个新的{@code DelegatingFilterProxy}, 它将从给定的Spring {@code WebApplicationContext}中检索指定的目标bean.
	 * <p>用于Servlet 3.0+环境, 支持基于实例的过滤器注册.
	 * <p>目标bean必须实现标准的Servlet Filter接口.
	 * <p>传入时, 给定的{@code WebApplicationContext}可能会刷新也可能不会刷新.
	 * 如果没有, 并且上下文实现了{@link ConfigurableApplicationContext},
	 * 则在检索命名目标bean之前将尝试{@link ConfigurableApplicationContext#refresh() refresh()}.
	 * <p>此代理的{@code Environment}将继承自给定的{@code WebApplicationContext}.
	 * 
	 * @param targetBeanName Spring应用程序上下文中目标过滤器bean的名称 (不能是{@code null}).
	 * @param wac 将从中检索目标过滤器的应用程序上下文;
	 * 如果是{@code null}, 将从{@code ServletContext}中查找应用程序上下文作为后备.
	 */
	public DelegatingFilterProxy(String targetBeanName, WebApplicationContext wac) {
		Assert.hasText(targetBeanName, "Target Filter bean name must not be null or empty");
		this.setTargetBeanName(targetBeanName);
		this.webApplicationContext = wac;
		if (wac != null) {
			this.setEnvironment(wac.getEnvironment());
		}
	}

	/**
	 * 设置ServletContext属性的名称, 该属性应该用于检索从中加载委托{@link Filter} bean的{@link WebApplicationContext}.
	 */
	public void setContextAttribute(String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * 返回ServletContext属性的名称, 该属性应该用于检索从中加载委托{@link Filter} bean的{@link WebApplicationContext}.
	 */
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * 设置Spring应用程序上下文中的目标bean的名称.
	 * 目标bean必须实现标准的Servlet Filter接口.
	 * <p>默认情况下, 将使用为{@code web.xml}中的DelegatingFilterProxy指定的{@code filter-name}.
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * 返回Spring应用程序上下文中的目标bean的名称.
	 */
	protected String getTargetBeanName() {
		return this.targetBeanName;
	}

	/**
	 * 设置是否在目标bean上调用{@code Filter.init}和{@code Filter.destroy}生命周期方法.
	 * <p>默认"false"; 目标bean通常依赖Spring应用程序上下文来管理其生命周期.
	 * 将此标志设置为"true"意味着servlet容器将控制目标Filter的生命周期, 此代理将委托相应的调用.
	 */
	public void setTargetFilterLifecycle(boolean targetFilterLifecycle) {
		this.targetFilterLifecycle = targetFilterLifecycle;
	}

	/**
	 * 返回是否在目标bean上调用{@code Filter.init}和{@code Filter.destroy}生命周期方法.
	 */
	protected boolean isTargetFilterLifecycle() {
		return this.targetFilterLifecycle;
	}


	@Override
	protected void initFilterBean() throws ServletException {
		synchronized (this.delegateMonitor) {
			if (this.delegate == null) {
				// 如果未指定目标bean名称, 使用过滤器名称.
				if (this.targetBeanName == null) {
					this.targetBeanName = getFilterName();
				}
				// 如果可能, 获取Spring根应用程序上下文并尽早初始化委托.
				// 如果在此过滤器代理之后将启动根应用程序上下文, 则我们将不得不求助于延迟初始化.
				WebApplicationContext wac = findWebApplicationContext();
				if (wac != null) {
					this.delegate = initDelegate(wac);
				}
			}
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 如有必要, 可以延迟初始化代理.
		Filter delegateToUse = this.delegate;
		if (delegateToUse == null) {
			synchronized (this.delegateMonitor) {
				delegateToUse = this.delegate;
				if (delegateToUse == null) {
					WebApplicationContext wac = findWebApplicationContext();
					if (wac == null) {
						throw new IllegalStateException("No WebApplicationContext found: " +
								"no ContextLoaderListener or DispatcherServlet registered?");
					}
					delegateToUse = initDelegate(wac);
				}
				this.delegate = delegateToUse;
			}
		}

		// 让委托执行实际的doFilter操作.
		invokeDelegate(delegateToUse, request, response, filterChain);
	}

	@Override
	public void destroy() {
		Filter delegateToUse = this.delegate;
		if (delegateToUse != null) {
			destroyDelegate(delegateToUse);
		}
	}


	/**
	 * 返回在构造时传入的{@code WebApplicationContext}.
	 * 否则，尝试使用{@linkplain #setContextAttribute 配置的名称}从{@code ServletContext}属性检索{@code WebApplicationContext}.
	 * 否则，在众所周知的"root"应用程序上下文属性下查找{@code WebApplicationContext}.
	 * 在此过滤器初始化(或调用)之前, {@code WebApplicationContext}必须已加载并存储在{@code ServletContext}中.
	 * <p>子类可以重写此方法以提供不同的{@code WebApplicationContext}检索策略.
	 * 
	 * @return 此代理的{@code WebApplicationContext}, 或{@code null}
	 */
	protected WebApplicationContext findWebApplicationContext() {
		if (this.webApplicationContext != null) {
			// 用户在构造时注入了上下文 -> 使用它...
			if (this.webApplicationContext instanceof ConfigurableApplicationContext) {
				ConfigurableApplicationContext cac = (ConfigurableApplicationContext) this.webApplicationContext;
				if (!cac.isActive()) {
					// 上下文尚未刷新 -> 在返回之前这样做...
					cac.refresh();
				}
			}
			return this.webApplicationContext;
		}
		String attrName = getContextAttribute();
		if (attrName != null) {
			return WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		}
		else {
			return WebApplicationContextUtils.findWebApplicationContext(getServletContext());
		}
	}

	/**
	 * 初始化Filter委托, 定义为给定Spring应用程序上下文的bean.
	 * <p>默认实现从应用程序上下文中获取bean并在其上调用标准{@code Filter.init}方法, 传入此Filter代理的FilterConfig.
	 * 
	 * @param wac 根应用程序上下文
	 * 
	 * @return 初始化的委托Filter
	 * @throws ServletException 如果被过滤器抛出
	 */
	protected Filter initDelegate(WebApplicationContext wac) throws ServletException {
		Filter delegate = wac.getBean(getTargetBeanName(), Filter.class);
		if (isTargetFilterLifecycle()) {
			delegate.init(getFilterConfig());
		}
		return delegate;
	}

	/**
	 * 实际使用给定的请求和响应调用委托Filter.
	 * 
	 * @param delegate 委托Filter
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param filterChain 当前的FilterChain
	 * 
	 * @throws ServletException 如果被过滤器抛出
	 * @throws IOException 如果被过滤器抛出
	 */
	protected void invokeDelegate(
			Filter delegate, ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		delegate.doFilter(request, response, filterChain);
	}

	/**
	 * 销毁过滤器委托.
	 * 默认实现只需在其上调用{@code Filter.destroy}.
	 * 
	 * @param delegate Filter委托 (never {@code null})
	 */
	protected void destroyDelegate(Filter delegate) {
		if (isTargetFilterLifecycle()) {
			delegate.destroy();
		}
	}

}
