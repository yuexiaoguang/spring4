package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link org.springframework.web.servlet.HandlerMapping}实现的抽象基类.
 * 支持排序, 默认处理器, 处理器拦截器, 包括由路径模式映射的处理器拦截器.
 *
 * <p>Note: 这个基类<i>不</i>支持公开{@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}.
 * 对此属性的支持取决于具体的子类, 通常基于请求URL映射.
 */
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport implements HandlerMapping, Ordered {

	private Object defaultHandler;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final List<Object> interceptors = new ArrayList<Object>();

	private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<HandlerInterceptor>();

	private final UrlBasedCorsConfigurationSource globalCorsConfigSource = new UrlBasedCorsConfigurationSource();

	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * 设置此处理器映射的默认处理器.
	 * 如果未找到特定映射, 则将返回此处理器.
	 * <p>默认为 {@code null}, 表示没有默认处理器.
	 */
	public void setDefaultHandler(Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * 返回此处理器映射的默认处理器, 或{@code null}.
	 */
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 * 设置URL查找是否应始终使用当前servlet上下文中的完整路径.
	 * 否则, 如果适用, 则使用当前servlet映射中的路径 (即, 在web.xml中的".../*" servlet映射的情况下).
	 * <p>默认为"false".
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
		this.globalCorsConfigSource.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * 设置是否应对上下文路径和请求URI进行URL解码. 与servlet路径相比, Servlet API都返回<i>未解码</i>.
	 * <p>根据Servlet规范 (ISO-8859-1)使用请求编码或默认编码.
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
		this.globalCorsConfigSource.setUrlDecode(urlDecode);
	}

	/**
	 * 设置是否应从请求URI中删除";" (分号)内容.
	 * <p>默认为{@code true}.
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
		this.globalCorsConfigSource.setRemoveSemicolonContent(removeSemicolonContent);
	}

	/**
	 * 设置用于查找路径的解析的UrlPathHelper.
	 * <p>使用此选项可以使用自定义子类覆盖默认的UrlPathHelper,
	 * 或者在多个HandlerMapping和MethodNameResolver之间共享常用的UrlPathHelper设置.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
		this.globalCorsConfigSource.setUrlPathHelper(urlPathHelper);
	}

	/**
	 * 返回用于解析查找路径的UrlPathHelper实现.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return urlPathHelper;
	}

	/**
	 * 设置用于匹配已注册的URL模式的URL路径的PathMatcher实现.
	 * 默认为 AntPathMatcher.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		this.globalCorsConfigSource.setPathMatcher(pathMatcher);
	}

	/**
	 * 返回用于匹配已注册的URL模式的URL路径的PathMatcher实现.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 设置应用于此处理器映射映射的所有处理器的拦截器.
	 * <p>支持的拦截器类型是 HandlerInterceptor, WebRequestInterceptor, 和MappedInterceptor.
	 * 映射的拦截器仅适用于与其路径模式匹配的请求URL.
	 * 初始化期间也会按类型检测映射的拦截器bean.
	 * 
	 * @param interceptors 处理器拦截器数组
	 */
	public void setInterceptors(Object... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 * 根据URL模式设置"全局" CORS配置.
	 * 默认情况下, 第一个匹配的URL模式与处理器的CORS配置相结合.
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		this.globalCorsConfigSource.setCorsConfigurations(corsConfigurations);
	}

	/**
	 * 获取"global"CORS配置.
	 */
	public Map<String, CorsConfiguration> getCorsConfigurations() {
		return this.globalCorsConfigSource.getCorsConfigurations();
	}

	/**
	 * 配置自定义{@link CorsProcessor}, 用于为请求应用匹配的{@link CorsConfiguration}.
	 * <p>默认使用{@link DefaultCorsProcessor}.
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * 返回配置的{@link CorsProcessor}.
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}

	/**
	 * 指定此HandlerMapping bean的顺序值.
	 * <p>默认为{@code Ordered.LOWEST_PRECEDENCE}, 没有顺序.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 初始化拦截器.
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		extendInterceptors(this.interceptors);
		detectMappedInterceptors(this.adaptedInterceptors);
		initInterceptors();
	}

	/**
	 * 在给定配置的拦截器的情况下, 子类可以重写以注册其他拦截器的扩展钩子 (see {@link #setInterceptors}).
	 * <p>将在{@link #initInterceptors()}将指定的拦截器适配为{@link HandlerInterceptor}实例之前调用.
	 * <p>默认实现为空.
	 * 
	 * @param interceptors 配置的拦截器列表 (never {@code null}), 允许在现有拦截器之前和之后添加更多拦截器
	 */
	protected void extendInterceptors(List<Object> interceptors) {
	}

	/**
	 * 检测{@link MappedInterceptor}类型的bean, 并将它们添加到映射的拦截器列表中.
	 * <p>除了可能通过{@link #setInterceptors}提供的{@link MappedInterceptor}之外, 还调用此方法,
	 * 默认情况下, 从当前上下文及其祖先添加{@link MappedInterceptor}类型的所有bean.
	 * 子类可以覆盖和优化此策略.
	 * 
	 * @param mappedInterceptors 一个空列表, 用于添加{@link MappedInterceptor}实例
	 */
	protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
		mappedInterceptors.addAll(
				BeanFactoryUtils.beansOfTypeIncludingAncestors(
						getApplicationContext(), MappedInterceptor.class, true, false).values());
	}

	/**
	 * 初始化指定的拦截器, 检查{@link MappedInterceptor},
	 * 并在必要时适配{@link HandlerInterceptor}和{@link WebRequestInterceptor}.
	 */
	protected void initInterceptors() {
		if (!this.interceptors.isEmpty()) {
			for (int i = 0; i < this.interceptors.size(); i++) {
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				this.adaptedInterceptors.add(adaptInterceptor(interceptor));
			}
		}
	}

	/**
	 * 将给定的拦截器对象适配为{@link HandlerInterceptor}接口.
	 * <p>默认支持的拦截器类型是{@link HandlerInterceptor}和{@link WebRequestInterceptor}.
	 * 每个给定的{@link WebRequestInterceptor}都将包装在{@link WebRequestHandlerInterceptorAdapter}中.
	 * 可以在子类中重写.
	 * 
	 * @param interceptor 指定的拦截器对象
	 * 
	 * @return 包装为HandlerInterceptor的拦截器
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
		}
		else {
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 * 返回适配的拦截器.
	 * 
	 * @return {@link HandlerInterceptor}数组, 或{@code null}
	 */
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		int count = this.adaptedInterceptors.size();
		return (count > 0 ? this.adaptedInterceptors.toArray(new HandlerInterceptor[count]) : null);
	}

	/**
	 * 返回所有配置的{@link MappedInterceptor}.
	 * 
	 * @return {@link MappedInterceptor}数组, 或{@code null}
	 */
	protected final MappedInterceptor[] getMappedInterceptors() {
		List<MappedInterceptor> mappedInterceptors = new ArrayList<MappedInterceptor>(this.adaptedInterceptors.size());
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				mappedInterceptors.add((MappedInterceptor) interceptor);
			}
		}
		int count = mappedInterceptors.size();
		return (count > 0 ? mappedInterceptors.toArray(new MappedInterceptor[count]) : null);
	}


	/**
	 * 查找给定请求的处理器, 如果找不到特定的请求, 则返回默认处理器.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 相应的处理器实例或默认处理器
	 */
	@Override
	public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = getApplicationContext().getBean(handlerName);
		}

		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
		if (CorsUtils.isCorsRequest(request)) {
			CorsConfiguration globalConfig = this.globalCorsConfigSource.getCorsConfiguration(request);
			CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
			CorsConfiguration config = (globalConfig != null ? globalConfig.combine(handlerConfig) : handlerConfig);
			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
		}
		return executionChain;
	}

	/**
	 * 查找给定请求的处理器, 如果找不到特定的处理器, 则返回{@code null}.
	 * 这个方法由{@link #getHandler}调用; 返回{@code null}将导致默认处理器.
	 * <p>在CORS pre-flight请求中, 此方法应返回不是针对pre-flight请求的匹配, 而是针对基于URL路径的预期实际请求的匹配,
	 * 来自"Access-Control-Request-Method" header的HTTP方法, 以及来自"Access-Control-Request-Headers" header的header,
	 * 从而允许通过{@link #getCorsConfigurations}获取CORS配置.
	 * <p>Note: 此方法还可以返回预构建的{@link HandlerExecutionChain}, 将处理器对象与动态确定的拦截器组合在一起.
	 * 静态指定的拦截器将合并到这样的现有链中.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 相应的处理器实例, 或{@code null}
	 * @throws Exception
	 */
	protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

	/**
	 * 为给定的处理器构建{@link HandlerExecutionChain}, 包括适用的拦截器.
	 * <p>默认实现使用给定的处理器, 处理器映射的公共拦截器, 以及与当前请求URL匹配的{@link MappedInterceptor}
	 * 构建标准{@link HandlerExecutionChain}.
	 * 拦截器按其注册顺序添加. 子类可以覆盖它以扩展/重新排列拦截器列表.
	 * <p><b>NOTE:</b> 传入的处理器对象可以是原始处理器或预构建的{@link HandlerExecutionChain}.
	 * 这个方法应该明确地处理这两种情况, 要么建立一个新的{@link HandlerExecutionChain}, 要么扩展现有的链.
	 * <p>要简单地在自定义子类中添加拦截器, 考虑调用{@code super.getHandlerExecutionChain(handler, request)}
	 * 并在返回的链对象上调用{@link HandlerExecutionChain#addInterceptor}.
	 * 
	 * @param handler 已解析的处理器实例 (never {@code null})
	 * @param request 当前的HTTP请求
	 * 
	 * @return the HandlerExecutionChain (never {@code null})
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
					chain.addInterceptor(mappedInterceptor.getInterceptor());
				}
			}
			else {
				chain.addInterceptor(interceptor);
			}
		}
		return chain;
	}

	/**
	 * 检索给定处理器的CORS配置.
	 * 
	 * @param handler 要检查的处理器 (never {@code null}).
	 * @param request 当前的HTTP请求
	 * 
	 * @return 处理器的CORS配置, 或{@code null}
	 */
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		Object resolvedHandler = handler;
		if (handler instanceof HandlerExecutionChain) {
			resolvedHandler = ((HandlerExecutionChain) handler).getHandler();
		}
		if (resolvedHandler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);
		}
		return null;
	}

	/**
	 * 更新HandlerExecutionChain以进行与CORS相关的处理.
	 * <p>对于pre-flight请求, 默认实现使用简单的HttpRequestHandler替换选定的处理器,
	 * 该HttpRequestHandler调用配置的{@link #setCorsProcessor}.
	 * <p>对于实际请求, 默认实现插入HandlerInterceptor,
	 * 该HandlerInterceptor进行与CORS相关的检查并添加CORS header.
	 * 
	 * @param request 当前的请求
	 * @param chain 处理器链
	 * @param config 适用的CORS配置 (possibly {@code null})
	 */
	protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,
			HandlerExecutionChain chain, CorsConfiguration config) {

		if (CorsUtils.isPreFlightRequest(request)) {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			chain = new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
		}
		else {
			chain.addInterceptor(new CorsInterceptor(config));
		}
		return chain;
	}


	private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {

		private final CorsConfiguration config;

		public PreFlightHandler(CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}


	private class CorsInterceptor extends HandlerInterceptorAdapter implements CorsConfigurationSource {

		private final CorsConfiguration config;

		public CorsInterceptor(CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
				throws Exception {

			return corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}

}
