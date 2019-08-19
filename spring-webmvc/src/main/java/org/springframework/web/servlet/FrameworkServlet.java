package org.springframework.web.servlet;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.CallableProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

/**
 * Spring的Web框架的基本servlet.
 * 在基于JavaBean的整体解决方案中提供与Spring应用程序上下文的集成.
 *
 * <p>该类提供以下功能:
 * <ul>
 * <li>管理每个servlet的{@link org.springframework.web.context.WebApplicationContext WebApplicationContext}实例.
 * servlet的配置由servlet命名空间中的bean确定.
 * <li>无论请求是否成功处理, 都会根据请求处理发布事件.
 * </ul>
 *
 * <p>子类必须实现{@link #doService}来处理请求.
 * 因为这会直接扩展{@link HttpServletBean}而不是HttpServlet, 所以bean属性会自动映射到它上面.
 * 子类可以覆盖{@link #initFrameworkServlet()}以进行自定义初始化.
 *
 * <p>检测servlet init-param级别的"contextClass"参数, 如果找不到则返回默认的上下文类
 * {@link org.springframework.web.context.support.XmlWebApplicationContext XmlWebApplicationContext}.
 * 请注意, 使用默认的{@code FrameworkServlet}, 自定义上下文类需要实现
 * {@link org.springframework.web.context.ConfigurableWebApplicationContext ConfigurableWebApplicationContext} SPI.
 *
 * <p>接受可选的"contextInitializerClasses" servlet init-param, 它指定一个或多个
 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}类.
 * 管理的Web应用程序上下文将被委托给这些初始化器, 从而允许进行额外的编程配置, e.g. 添加属性源或激活针对
 * {@linkplain org.springframework.context.ConfigurableApplicationContext#getEnvironment() 上下文环境}的配置文件.
 * 另请参阅{@link org.springframework.web.context.ContextLoader}, 它支持"contextInitializerClasses" context-param,
 * 其中"root" Web应用程序上下文具有相同的语义.
 *
 * <p>将"contextConfigLocation" servlet init-param传递给上下文实例, 将其解析为可能由多个逗号和空格分隔的多个文件路径,
 * 例如"test-servlet.xml, myServlet.xml".
 * 如果没有明确指定, 则上下文实现应该从servlet的命名空间构建一个默认位置.
 *
 * <p>Note: 在多个配置位置的情况下, 以后的bean定义将覆盖先前加载的文件中定义的那些,
 * 至少在使用Spring的默认ApplicationContext实现时.
 * 这可以用来通过额外的XML文件故意覆盖某些bean定义.
 *
 * <p>默认命名空间是"'servlet-name'-servlet", e.g. "test-servlet"的servlet名称为"test"
 * (通过XmlWebApplicationContext指向"/WEB-INF/test-servlet.xml"默认位置).
 * 也可以通过"namespace" servlet init-param显式设置命名空间.
 *
 * <p>从Spring 3.1开始, {@code FrameworkServlet}现在可以注入Web应用程序上下文, 而不是在内部创建自己的.
 * 这在Servlet 3.0+环境中很有用, 它支持servlet实例的编程注册.
 * See {@link #FrameworkServlet(WebApplicationContext)} Javadoc for details.
 */
@SuppressWarnings("serial")
public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {

	/**
	 * WebApplicationContext命名空间的后缀.
	 * 如果此类的servlet在上下文中被赋予名称"test", 则servlet使用的命名空间将解析为"test-servlet".
	 */
	public static final String DEFAULT_NAMESPACE_SUFFIX = "-servlet";

	/**
	 * FrameworkServlet的默认上下文类.
	 */
	public static final Class<?> DEFAULT_CONTEXT_CLASS = XmlWebApplicationContext.class;

	/**
	 * WebApplicationContext的ServletContext属性的前缀.
	 * 完整的是servlet名称.
	 */
	public static final String SERVLET_CONTEXT_PREFIX = FrameworkServlet.class.getName() + ".CONTEXT.";

	/**
	 * 在单个init-param字符串值中, 任何数量的这些字符都被视为多个值之间的分隔符.
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";


	/** 检查Servlet 3.0+ HttpServletResponse.getStatus() */
	private static final boolean responseGetStatusAvailable =
			ClassUtils.hasMethod(HttpServletResponse.class, "getStatus");


	/** 用于查找WebApplicationContext的ServletContext属性 */
	private String contextAttribute;

	/** 要创建的WebApplicationContext实现类 */
	private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;

	/** 要分配的WebApplicationContext id */
	private String contextId;

	/** 此servlet的命名空间 */
	private String namespace;

	/** 显式上下文配置位置 */
	private String contextConfigLocation;

	/** 要应用于上下文的实际ApplicationContextInitializer实例 */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>>();

	/** 通过init param设置的逗号分隔的ApplicationContextInitializer类名 */
	private String contextInitializerClasses;

	/** 是否应该将上下文作为ServletContext属性发布? */
	private boolean publishContext = true;

	/** 是否应该在每个请求结束时发布ServletRequestHandledEvent? */
	private boolean publishEvents = true;

	/** 将LocaleContext和RequestAttributes公开为子线程可继承? */
	private boolean threadContextInheritable = false;

	/** 是否应该分派HTTP OPTIONS请求到{@link #doService}? */
	private boolean dispatchOptionsRequest = false;

	/** 是否应该分派HTTP TRACE请求到{@link #doService}? */
	private boolean dispatchTraceRequest = false;

	/** 此servlet的WebApplicationContext */
	private WebApplicationContext webApplicationContext;

	/** 如果通过{@link #setApplicationContext}注入WebApplicationContext */
	private boolean webApplicationContextInjected = false;

	/** 用于检测是否已调用onRefresh的标志 */
	private boolean refreshEventReceived = false;


	/**
	 * 创建一个新的{@code FrameworkServlet},
	 * 它将根据servlet init-params提供的默认值和值创建自己的内部Web应用程序上下文.
	 * 通常在Servlet 2.5或更早版本的环境中使用, 其中servlet注册的唯一选项是通过{@code web.xml}, 这需要使用no-arg构造函数.
	 * <p>调用{@link #setContextConfigLocation} (init-param 'contextConfigLocation')
	 * 将指定{@linkplain #DEFAULT_CONTEXT_CLASS 默认的XmlWebApplicationContext}将加载哪些XML文件
	 * <p>调用{@link #setContextClass} (init-param 'contextClass')
	 * 会覆盖默认的{@code XmlWebApplicationContext}, 并允许指定替代类, 例如{@code AnnotationConfigWebApplicationContext}.
	 * <p>调用{@link #setContextInitializerClasses} (init-param 'contextInitializerClasses')
	 * 指示在refresh()之前应该使用哪些{@link ApplicationContextInitializer}类来进一步配置内部应用程序上下文.
	 */
	public FrameworkServlet() {
	}

	/**
	 * 这个构造函数在Servlet 3.0+环境中很有用, 在这些环境中,
	 * 通过{@link ServletContext#addServlet} API可以实现基于实例的servlet注册.
	 * <p>使用此构造函数表示将忽略以下属性 / init-params:
	 * <ul>
	 * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
	 * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
	 * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
	 * <li>{@link #setNamespace(String)} / 'namespace'</li>
	 * </ul>
	 * <p>给定的Web应用程序上下文不确定是否{@linkplain ConfigurableApplicationContext#refresh() 刷新}.
	 * 如果(a) 是{@link ConfigurableWebApplicationContext}的实现, 并且(b) <strong>还没有</strong>刷新 (推荐的方法),
	 * 那么将发生以下情况:
	 * <ul>
	 * <li>如果给定的上下文还没有{@linkplain ConfigurableApplicationContext#setParent parent},
	 * 则根应用程序上下文将被设置为父项.</li>
	 * <li>如果尚未为给定的上下文分配{@linkplain ConfigurableApplicationContext#setId id}, 则会为其分配一个</li>
	 * <li>{@code ServletContext}和{@code ServletConfig}对象将被委托给应用程序上下文</li>
	 * <li>将调用{@link #postProcessWebApplicationContext}</li>
	 * <li>将应用通过"contextInitializerClasses" init-param或通过{@link #setContextInitializers}属性
	 * 指定的任何{@link ApplicationContextInitializer}.</li>
	 * <li>将调用{@link ConfigurableApplicationContext#refresh refresh()}</li>
	 * </ul>
	 * 如果上下文已经刷新或未实现{@code ConfigurableWebApplicationContext},
	 * 则假设用户已根据其特定需求执行(或不执行)这些操作, 则不会发生上述任何情况.
	 * <p>有关用法示例, 请参阅{@link org.springframework.web.WebApplicationInitializer}.
	 * 
	 * @param webApplicationContext 要使用的上下文
	 */
	public FrameworkServlet(WebApplicationContext webApplicationContext) {
		this.webApplicationContext = webApplicationContext;
	}


	/**
	 * 设置ServletContext属性的名称, 该属性应该用于检索此servlet应该使用的{@link WebApplicationContext}.
	 */
	public void setContextAttribute(String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * 返回ServletContext属性的名称, 该属性应该用于检索此servlet应该使用的{@link WebApplicationContext}.
	 */
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * 设置自定义上下文类. 此类必须是{@link org.springframework.web.context.WebApplicationContext}类型.
	 * <p>使用默认的FrameworkServlet实现时, 上下文类还必须实现
	 * {@link org.springframework.web.context.ConfigurableWebApplicationContext}接口.
	 */
	public void setContextClass(Class<?> contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * 返回自定义上下文类.
	 */
	public Class<?> getContextClass() {
		return this.contextClass;
	}

	/**
	 * 指定自定义WebApplicationContext id, 以用作底层BeanFactory的序列化标识.
	 */
	public void setContextId(String contextId) {
		this.contextId = contextId;
	}

	/**
	 * 返回自定义 WebApplicationContext id.
	 */
	public String getContextId() {
		return this.contextId;
	}

	/**
	 * 设置此servlet的自定义命名空间, 以用于构建默认上下文配置位置.
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * 返回此servlet的命名空间, 如果未设置自定义命名空间, 则返回默认scheme: e.g. "test-servlet"的servlet名称为"test".
	 */
	public String getNamespace() {
		return (this.namespace != null ? this.namespace : getServletName() + DEFAULT_NAMESPACE_SUFFIX);
	}

	/**
	 * 显式设置上下文配置位置, 而不是依赖于从命名空间构建的默认位置.
	 * 此位置字符串可以包含由任意数量的逗号和空格分隔的多个位置.
	 */
	public void setContextConfigLocation(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * 返回显式上下文配置位置.
	 */
	public String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * 指定应该使用哪个{@link ApplicationContextInitializer}实例来初始化此{@code FrameworkServlet}使用的应用程序上下文.
	 */
	@SuppressWarnings("unchecked")
	public void setContextInitializers(ApplicationContextInitializer<?>... initializers) {
		if (initializers != null) {
			for (ApplicationContextInitializer<?> initializer : initializers) {
				this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
			}
		}
	}

	/**
	 * 根据可选的"contextInitializerClasses" servlet init-param,
	 * 指定一组完全限定的{@link ApplicationContextInitializer}类名.
	 */
	public void setContextInitializerClasses(String contextInitializerClasses) {
		this.contextInitializerClasses = contextInitializerClasses;
	}

	/**
	 * 设置是否将此servlet的上下文作为ServletContext属性发布, 可用于Web容器中的所有对象.
	 * 默认为"true".
	 * <p>这在测试期间尤其方便, 尽管让其他应用程序对象以这种方式访问​​上下文是否是一种好的做法值得商榷.
	 */
	public void setPublishContext(boolean publishContext) {
		this.publishContext = publishContext;
	}

	/**
	 * 设置此servlet是否应在每个请求结束时发布ServletRequestHandledEvent.
	 * 默认为"true"; 可以关闭以获得轻微的性能提升, 前提是没有ApplicationListeners依赖此类事件.
	 */
	public void setPublishEvents(boolean publishEvents) {
		this.publishEvents = publishEvents;
	}

	/**
	 * 设置是否将LocaleContext和RequestAttributes公开为子线程可继承 (使用{@link java.lang.InheritableThreadLocal}).
	 * <p>默认"false", 避免产生背景线程的副作用.
	 * 将其切换为"true"以启用在请求处理期间生成并仅用于此请求的自定义子线程的继承
	 * (也就是说, 在他们的初始任务结束后, 不重用线程).
	 * <p><b>WARNING:</b> 如果正在访问配置为按需添加新线程的线程池
	 * (e.g. a JDK {@link java.util.concurrent.ThreadPoolExecutor}),
	 * 不要对子线程使用继承, 因为这会将继承的上下文暴露给此池中的线程.
	 */
	public void setThreadContextInheritable(boolean threadContextInheritable) {
		this.threadContextInheritable = threadContextInheritable;
	}

	/**
	 * 设置此servlet是否应将HTTP OPTIONS请求分派给{@link #doService}方法.
	 * <p>{@code FrameworkServlet}中的默认值为"false", 应用{@link javax.servlet.http.HttpServlet}的默认行为
	 * (i.e. 将所有标准HTTP请求方法作为对OPTIONS请求的响应进行枚举).
	 * 但请注意, 从4.3开始, {@code DispatcherServlet}默认将此属性设置为"true", 因为它内置了对OPTIONS的支持.
	 * <p>如果更喜欢OPTIONS请求通过常规调度链, 请打开此标志, 就像其他HTTP请求一样.
	 * 这通常意味着控制器将接收这些请求; 确保这些端点实际上能够处理OPTIONS请求.
	 * <p>请注意, 如果控制器碰巧没有设置'Allow' header (根据OPTIONS响应的要求), HttpServlet的默认OPTIONS处理将适用于任何情况.
	 */
	public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
		this.dispatchOptionsRequest = dispatchOptionsRequest;
	}

	/**
	 * 设置此servlet是否应将HTTP TRACE请求分派给{@link #doService}方法.
	 * <p>默认为"false", 应用{@link javax.servlet.http.HttpServlet}的默认行为 (i.e. 反映收到的消息返回给客户端).
	 * <p>如果希望TRACE请求通过常规调度链, 就像其他HTTP请求一样, 打开此标志.
	 * 这通常意味着控制器将接收这些请求; 确保这些端点实际上能够处理TRACE请求.
	 * <p>请注意, 如果控制器碰巧没有生成内容类型'message/http'的响应 (根据TRACE响应的要求),
	 * 将在任何情况下应用HttpServlet的默认TRACE处理.
	 */
	public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
		this.dispatchTraceRequest = dispatchTraceRequest;
	}

	/**
	 * 由Spring通过{@link ApplicationContextAware}调用以注入当前的应用程序上下文.
	 * 此方法允许FrameworkServlet在现有{@link WebApplicationContext}中注册为Spring bean,
	 * 而不是{@link #findWebApplicationContext() 查找} {@link org.springframework.web.context.ContextLoaderListener bootstrapped}上下文.
	 * <p>主要用于支持嵌入式servlet容器.
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (this.webApplicationContext == null && applicationContext instanceof WebApplicationContext) {
			this.webApplicationContext = (WebApplicationContext) applicationContext;
			this.webApplicationContextInjected = true;
		}
	}


	/**
	 * 重写{@link HttpServletBean}的方法, 在设置任何bean属性后调用.
	 * 创建此servlet的WebApplicationContext.
	 */
	@Override
	protected final void initServletBean() throws ServletException {
		getServletContext().log("Initializing Spring FrameworkServlet '" + getServletName() + "'");
		if (this.logger.isInfoEnabled()) {
			this.logger.info("FrameworkServlet '" + getServletName() + "': initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			this.webApplicationContext = initWebApplicationContext();
			initFrameworkServlet();
		}
		catch (ServletException ex) {
			this.logger.error("Context initialization failed", ex);
			throw ex;
		}
		catch (RuntimeException ex) {
			this.logger.error("Context initialization failed", ex);
			throw ex;
		}

		if (this.logger.isInfoEnabled()) {
			long elapsedTime = System.currentTimeMillis() - startTime;
			this.logger.info("FrameworkServlet '" + getServletName() + "': initialization completed in " +
					elapsedTime + " ms");
		}
	}

	/**
	 * 初始化并发布此servlet的WebApplicationContext.
	 * <p>委托给{@link #createWebApplicationContext}实际创建上下文. 可以在子类中重写.
	 * 
	 * @return WebApplicationContext实例
	 */
	protected WebApplicationContext initWebApplicationContext() {
		WebApplicationContext rootContext =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		WebApplicationContext wac = null;

		if (this.webApplicationContext != null) {
			// 在构造时注入了一个上下文实例 -> 使用它
			wac = this.webApplicationContext;
			if (wac instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
				if (!cwac.isActive()) {
					// 上下文尚未刷新 -> 提供诸如设置父上下文, 设置应用程序上下文ID等服务
					if (cwac.getParent() == null) {
						// 在没有显式父级的情况下注入上下文实例 -> 将根应用程序上下文(如果有的话;可以为null)设置为父级
						cwac.setParent(rootContext);
					}
					configureAndRefreshWebApplicationContext(cwac);
				}
			}
		}
		if (wac == null) {
			// 在构造时没有注入上下文实例 -> 查看是否已在servlet上下文中注册了一个.
			// 如果存在, 则假设已经设置了父级上下文, 并且用户已经执行了初始化, 例如设置上下文id
			wac = findWebApplicationContext();
		}
		if (wac == null) {
			// 没有为此servlet定义上下文实例 -> 创建本地实例
			wac = createWebApplicationContext(rootContext);
		}

		if (!this.refreshEventReceived) {
			// 上下文不是具有刷新支持的ConfigurableApplicationContext, 或者在构造时注入的上下文已经刷新 -> 在这里手动触发初始onRefresh.
			onRefresh(wac);
		}

		if (this.publishContext) {
			// 将上下文发布为servlet上下文属性.
			String attrName = getServletContextAttributeName();
			getServletContext().setAttribute(attrName, wac);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Published WebApplicationContext of servlet '" + getServletName() +
						"' as ServletContext attribute with name [" + attrName + "]");
			}
		}

		return wac;
	}

	/**
	 * 使用{@link #setContextAttribute 配置的名称}从{@code ServletContext}属性中检索{@code WebApplicationContext}.
	 * 在初始化(或调用)此servlet之前, {@code WebApplicationContext}必须已加载并存储在{@code ServletContext}中.
	 * <p>子类可以重写此方法以提供不同的{@code WebApplicationContext}检索策略.
	 * 
	 * @return 此servlet的WebApplicationContext, 或{@code null}
	 */
	protected WebApplicationContext findWebApplicationContext() {
		String attrName = getContextAttribute();
		if (attrName == null) {
			return null;
		}
		WebApplicationContext wac =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
		}
		return wac;
	}

	/**
	 * 实例化此servlet的WebApplicationContext,
	 * 使用默认{@link org.springframework.web.context.support.XmlWebApplicationContext}
	 * 或{@link #setContextClass 自定义上下文类}.
	 * <p>此实现期望自定义上下文实现{@link org.springframework.web.context.ConfigurableWebApplicationContext}接口.
	 * 可以在子类中重写.
	 * <p>不要忘记在创建的上下文中将此servlet实例注册为应用程序监听器 (用于触发其{@link #onRefresh 回调},
	 * 并在返回上下文实例之前调用{@link org.springframework.context.ConfigurableApplicationContext#refresh()}).
	 * 
	 * @param parent 要使用的父级ApplicationContext, 或{@code null}
	 * 
	 * @return 此servlet的WebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
		Class<?> contextClass = getContextClass();
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Servlet with name '" + getServletName() +
					"' will try to create custom WebApplicationContext context of class '" +
					contextClass.getName() + "'" + ", using parent context [" + parent + "]");
		}
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException(
					"Fatal initialization error in servlet with name '" + getServletName() +
					"': custom WebApplicationContext class [" + contextClass.getName() +
					"] is not of type ConfigurableWebApplicationContext");
		}
		ConfigurableWebApplicationContext wac =
				(ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);

		wac.setEnvironment(getEnvironment());
		wac.setParent(parent);
		wac.setConfigLocation(getContextConfigLocation());

		configureAndRefreshWebApplicationContext(wac);

		return wac;
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// 应用程序上下文ID仍设置为其原始默认值 -> 根据可用信息分配更有用的ID
			if (this.contextId != null) {
				wac.setId(this.contextId);
			}
			else {
				// 生成默认id...
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
			}
		}

		wac.setServletContext(getServletContext());
		wac.setServletConfig(getServletConfig());
		wac.setNamespace(getNamespace());
		wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));

		// 在刷新上下文时, 将在任何情况下调用wac环境的 #initPropertySources;
		// 在这里实时地确保servlet属性源适用于在 #refresh 之前发生的任何后处理或初始化
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
		}

		postProcessWebApplicationContext(wac);
		applyInitializers(wac);
		wac.refresh();
	}

	/**
	 * 实例化此servlet的WebApplicationContext,
	 * 使用默认{@link org.springframework.web.context.support.XmlWebApplicationContext}
	 * 或{@link #setContextClass 自定义上下文类}.
	 * 委托给 #createWebApplicationContext(ApplicationContext).
	 * 
	 * @param parent 要使用的父级WebApplicationContext, 或{@code null}
	 * 
	 * @return 此servlet的WebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
		return createWebApplicationContext((ApplicationContext) parent);
	}

	/**
	 * 在给定的WebApplicationContext刷新并作为此servlet的上下文之前, 对其进行后处理.
	 * <p>默认实现为空. 此方法返回后, 将自动调用{@code refresh()}.
	 * <p>请注意, 此方法旨在允许子类修改应用程序上下文,
	 * 而{@link #initWebApplicationContext}旨在允许最终用户通过使用{@link ApplicationContextInitializer}来修改上下文.
	 * 
	 * @param wac 配置的WebApplicationContext (尚未刷新)
	 */
	protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
	}

	/**
	 * 委托WebApplicationContext,
	 * 在将其刷新到由"contextInitializerClasses" servlet init-param
	 * 指定的任何{@link ApplicationContextInitializer}实例之前.
	 * <p>另请参阅{@link #postProcessWebApplicationContext}, 它旨在允许子类 (而不是最终用户)修改应用程序上下文,
	 * 并在此方法之前立即调用.
	 * 
	 * @param wac 配置的WebApplicationContext (尚未刷新)
	 */
	protected void applyInitializers(ConfigurableApplicationContext wac) {
		String globalClassNames = getServletContext().getInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM);
		if (globalClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		if (this.contextInitializerClasses != null) {
			for (String className : StringUtils.tokenizeToStringArray(this.contextInitializerClasses, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	@SuppressWarnings("unchecked")
	private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(
			String className, ConfigurableApplicationContext wac) {
		try {
			Class<?> initializerClass = ClassUtils.forName(className, wac.getClassLoader());
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"framework servlet: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			return BeanUtils.instantiateClass(initializerClass, ApplicationContextInitializer.class);
		}
		catch (ClassNotFoundException ex) {
			throw new ApplicationContextException(String.format("Could not load class [%s] specified " +
					"via 'contextInitializerClasses' init-param", className), ex);
		}
	}

	/**
	 * 返回此servlet的WebApplicationContext的ServletContext属性名称.
	 * <p>默认实现返回{@code SERVLET_CONTEXT_PREFIX + servlet name}.
	 */
	public String getServletContextAttributeName() {
		return SERVLET_CONTEXT_PREFIX + getServletName();
	}

	/**
	 * 返回此servlet的WebApplicationContext.
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	/**
	 * 在设置任何bean属性并加载WebApplicationContext之后, 将调用此方法.
	 * 默认实现为空; 子类可以重写此方法以执行它们所需的初始化.
	 * 
	 * @throws ServletException 初始化失败
	 */
	protected void initFrameworkServlet() throws ServletException {
	}

	/**
	 * 刷新此servlet的应用程序上下文以及servlet的依赖状态.
	 */
	public void refresh() {
		WebApplicationContext wac = getWebApplicationContext();
		if (!(wac instanceof ConfigurableApplicationContext)) {
			throw new IllegalStateException("WebApplicationContext does not support refresh: " + wac);
		}
		((ConfigurableApplicationContext) wac).refresh();
	}

	/**
	 * 从此servlet的WebApplicationContext接收刷新事件的回调.
	 * <p>默认实现调用{@link #onRefresh}, 触发刷新此servlet的依赖于上下文的状态.
	 * 
	 * @param event 传入的ApplicationContext事件
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.refreshEventReceived = true;
		onRefresh(event.getApplicationContext());
	}

	/**
	 * 可以重写的模板方法, 以添加特定于servlet的刷新工作.
	 * 成功刷新上下文后调用.
	 * <p>此实现为空.
	 * 
	 * @param context 当前WebApplicationContext
	 */
	protected void onRefresh(ApplicationContext context) {
		// For subclasses: do nothing by default.
	}

	/**
	 * 关闭此servlet的WebApplicationContext.
	 */
	@Override
	public void destroy() {
		getServletContext().log("Destroying Spring FrameworkServlet '" + getServletName() + "'");
		// 如果在本地管理, 则仅在WebApplicationContext上调用 close()...
		if (this.webApplicationContext instanceof ConfigurableApplicationContext && !this.webApplicationContextInjected) {
			((ConfigurableApplicationContext) this.webApplicationContext).close();
		}
	}


	/**
	 * 覆盖父类实现以拦截PATCH请求.
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
		if (httpMethod == HttpMethod.PATCH || httpMethod == null) {
			processRequest(request, response);
		}
		else {
			super.service(request, response);
		}
	}

	/**
	 * 将GET请求委托给 processRequest/doService.
	 * <p>也将由HttpServlet的{@code doHead}的默认实现调用, 其中 {@code NoBodyResponse}只捕获内容长度.
	 */
	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将 POST 请求委托给{@link #processRequest}.
	 */
	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将 PUT请求委托给{@link #processRequest}.
	 */
	@Override
	protected final void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将 DELETE请求委托给{@link #processRequest}.
	 */
	@Override
	protected final void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将 OPTIONS 请求委托给{@link #processRequest}.
	 * <p>否则应用HttpServlet的标准OPTIONS处理, 如果在调度后仍然没有设置'Allow' header.
	 */
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (this.dispatchOptionsRequest || CorsUtils.isPreFlightRequest(request)) {
			processRequest(request, response);
			if (response.containsHeader("Allow")) {
				// 正确的OPTIONS响应来自处理器 - 已经完成了.
				return;
			}
		}

		// 对于不存在 getHeader() 方法的Servlet 2.5兼容性, 使用响应包装器
		super.doOptions(request, new HttpServletResponseWrapper(response) {
			@Override
			public void setHeader(String name, String value) {
				if ("Allow".equals(name)) {
					value = (StringUtils.hasLength(value) ? value + ", " : "") + HttpMethod.PATCH.name();
				}
				super.setHeader(name, value);
			}
		});
	}

	/**
	 * 将 TRACE 请求委托给{@link #processRequest}.
	 * <p>否则应用HttpServlet的标准TRACE处理.
	 */
	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (this.dispatchTraceRequest) {
			processRequest(request, response);
			if ("message/http".equals(response.getContentType())) {
				// 来自处理器的正确TRACE响应 - 已经完成了.
				return;
			}
		}
		super.doTrace(request, response);
	}

	/**
	 * 处理此请求, 无论结果如何都发布事件.
	 * <p>实际的事件处理由抽象的 {@link #doService}模板方法执行.
	 */
	protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		long startTime = System.currentTimeMillis();
		Throwable failureCause = null;

		LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
		LocaleContext localeContext = buildLocaleContext(request);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

		initContextHolders(request, localeContext, requestAttributes);

		try {
			doService(request, response);
		}
		catch (ServletException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (IOException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (Throwable ex) {
			failureCause = ex;
			throw new NestedServletException("Request processing failed", ex);
		}

		finally {
			resetContextHolders(request, previousLocaleContext, previousAttributes);
			if (requestAttributes != null) {
				requestAttributes.requestCompleted();
			}

			if (logger.isDebugEnabled()) {
				if (failureCause != null) {
					this.logger.debug("Could not complete request", failureCause);
				}
				else {
					if (asyncManager.isConcurrentHandlingStarted()) {
						logger.debug("Leaving response open for concurrent processing");
					}
					else {
						this.logger.debug("Successfully completed request");
					}
				}
			}

			publishRequestHandledEvent(request, response, startTime, failureCause);
		}
	}

	/**
	 * 为给定请求构建LocaleContext, 将请求的主要区域设置公开为当前区域设置.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 相应的LocaleContext, 或{@code null}
	 */
	protected LocaleContext buildLocaleContext(HttpServletRequest request) {
		return new SimpleLocaleContext(request.getLocale());
	}

	/**
	 * 为给定的请求构建ServletRequestAttributes (可能还包含对响应的引用), 考虑预绑定属性 (及其类型).
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param previousAttributes 预绑定的RequestAttributes实例
	 * 
	 * @return 要绑定的ServletRequestAttributes, 或{@code null}以保留以前绑定的实例 (或者如果之前没有绑定, 则不绑定任何实例)
	 */
	protected ServletRequestAttributes buildRequestAttributes(
			HttpServletRequest request, HttpServletResponse response, RequestAttributes previousAttributes) {

		if (previousAttributes == null || previousAttributes instanceof ServletRequestAttributes) {
			return new ServletRequestAttributes(request, response);
		}
		else {
			return null;  // 保留以前绑定的 RequestAttributes实例
		}
	}

	private void initContextHolders(
			HttpServletRequest request, LocaleContext localeContext, RequestAttributes requestAttributes) {

		if (localeContext != null) {
			LocaleContextHolder.setLocaleContext(localeContext, this.threadContextInheritable);
		}
		if (requestAttributes != null) {
			RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Bound request context to thread: " + request);
		}
	}

	private void resetContextHolders(HttpServletRequest request,
			LocaleContext prevLocaleContext, RequestAttributes previousAttributes) {

		LocaleContextHolder.setLocaleContext(prevLocaleContext, this.threadContextInheritable);
		RequestContextHolder.setRequestAttributes(previousAttributes, this.threadContextInheritable);
		if (logger.isTraceEnabled()) {
			logger.trace("Cleared thread-bound request context: " + request);
		}
	}

	private void publishRequestHandledEvent(
			HttpServletRequest request, HttpServletResponse response, long startTime, Throwable failureCause) {

		if (this.publishEvents) {
			// 无论是否成功, 都要发布一个事件.
			long processingTime = System.currentTimeMillis() - startTime;
			int statusCode = (responseGetStatusAvailable ? response.getStatus() : -1);
			this.webApplicationContext.publishEvent(
					new ServletRequestHandledEvent(this,
							request.getRequestURI(), request.getRemoteAddr(),
							request.getMethod(), getServletConfig().getServletName(),
							WebUtils.getSessionId(request), getUsernameForRequest(request),
							processingTime, failureCause, statusCode));
		}
	}

	/**
	 * 确定给定请求的用户名.
	 * <p>默认实现采用UserPrincipal的名称.
	 * 可以在子类中重写.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 用户名, 或{@code null}
	 */
	protected String getUsernameForRequest(HttpServletRequest request) {
		Principal userPrincipal = request.getUserPrincipal();
		return (userPrincipal != null ? userPrincipal.getName() : null);
	}


	/**
	 * 子类必须实现此方法来执行请求处理工作, 接收 GET, POST, PUT, DELETE的集中回调.
	 * <p>该约定基本上与HttpServlet需要重写{@code doGet}或{@code doPost}方法的约定相同.
	 * <p>此类拦截调用以确保发生异常处理和事件发布.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @throws Exception 在任何处理失败的情况下
	 */
	protected abstract void doService(HttpServletRequest request, HttpServletResponse response)
			throws Exception;


	/**
	 * ApplicationListener端点, 仅接收来自此servlet的WebApplicationContext的事件,
	 * 委托给FrameworkServlet实例上的{@code onApplicationEvent}.
	 */
	private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			FrameworkServlet.this.onApplicationEvent(event);
		}
	}


	/**
	 * CallableProcessingInterceptor实现, 初始化和重置FrameworkServlet的上下文保存器,
	 * i.e. LocaleContextHolder 和 RequestContextHolder.
	 */
	private class RequestBindingInterceptor extends CallableProcessingInterceptorAdapter {

		@Override
		public <T> void preProcess(NativeWebRequest webRequest, Callable<T> task) {
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			if (request != null) {
				HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
				initContextHolders(request, buildLocaleContext(request),
						buildRequestAttributes(request, response, null));
			}
		}
		@Override
		public <T> void postProcess(NativeWebRequest webRequest, Callable<T> task, Object concurrentResult) {
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			if (request != null) {
				resetContextHolders(request, null, null);
			}
		}
	}
}
