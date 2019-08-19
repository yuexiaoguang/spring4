package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 通过指向Spring MVC控制器上的{@code @RequestMapping}方法创建{@link org.springframework.web.util.UriComponentsBuilder}的实例.
 *
 * <p>有几组方法:
 * <ul>
 * <li>静态{@code fromXxx(...)}方法使用来自当前请求的信息来准备链接, 这些信息是通过调用
 * {@link org.springframework.web.servlet.support.ServletUriComponentsBuilder#fromCurrentServletMapping()}确定的.
 * <li>在请求的上下文之外操作时, 静态{@code fromXxx(UriComponentsBuilder,...)}方法可以被赋予baseUrl.
 * <li>基于实例的{@code withXxx(...)}方法, 其中MvcUriComponentsBuilder的实例是通过
 * {@link #relativeTo(org.springframework.web.util.UriComponentsBuilder)}使用baseUrl创建的.
 * </ul>
 *
 * <p><strong>Note:</strong> 此类使用"Forwarded"
 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
 * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" header中的值,
 * 以反映客户端发起的协议和地址.
 * 考虑使用{@code ForwardedHeaderFilter}从中心位置选择是否提取和使用, 或丢弃此类 header.
 * 有关此过滤器的更多信息, 请参阅Spring Framework参考.
 */
public class MvcUriComponentsBuilder {

	/**
	 * bean工厂中{@link CompositeUriComponentsContributor}对象的众所周知的名称.
	 */
	public static final String MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME = "mvcUriComponentsContributor";


	private static final Log logger = LogFactory.getLog(MvcUriComponentsBuilder.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	private static final PathMatcher pathMatcher = new AntPathMatcher();

	private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private static final CompositeUriComponentsContributor defaultUriComponentsContributor;

	static {
		defaultUriComponentsContributor = new CompositeUriComponentsContributor(
				new PathVariableMethodArgumentResolver(), new RequestParamMethodArgumentResolver(false));
	}

	private final UriComponentsBuilder baseUrl;


	protected MvcUriComponentsBuilder(UriComponentsBuilder baseUrl) {
		Assert.notNull(baseUrl, "'baseUrl' is required");
		this.baseUrl = baseUrl;
	}


	/**
	 * 使用基本URL创建此类的实例.
	 * 之后调用基于实例的{@code withXxx(...}}方法之一将创建相对于给定基本URL的URL.
	 */
	public static MvcUriComponentsBuilder relativeTo(UriComponentsBuilder baseUrl) {
		return new MvcUriComponentsBuilder(baseUrl);
	}


	/**
	 * 从控制器类的映射和包括Servlet映射的当前请求信息创建{@link UriComponentsBuilder}.
	 * 如果控制器包含多个映射, 则仅使用第一个映射.
	 * <p><strong>Note:</strong> 此方法从"Forwarded" 和 "X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param controllerType 为其构建URI的控制器
	 * 
	 * @return UriComponentsBuilder实例 (never {@code null})
	 */
	public static UriComponentsBuilder fromController(Class<?> controllerType) {
		return fromController(null, controllerType);
	}

	/**
	 * {@link #fromController(Class)}的替代方法, 它接受表示基本URL的{@code UriComponentsBuilder}.
	 * 在处理请求的上下文之外使用MvcUriComponentsBuilder或应用与当前请求不匹配的自定义baseUrl时, 这很有用.
	 * <p><strong>Note:</strong> 此方法从"Forwarded" 和 "X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param builder 基本URL的构建器; 构建器将被克隆, 因此不会被修改, 可以重新用于进一步调用.
	 * @param controllerType 为其构建URI的控制器
	 * 
	 * @return UriComponentsBuilder实例 (never {@code null})
	 */
	public static UriComponentsBuilder fromController(UriComponentsBuilder builder,
			Class<?> controllerType) {

		builder = getBaseUrlToUse(builder);
		String mapping = getTypeRequestMapping(controllerType);
		return builder.path(mapping);
	}

	/**
	 * 从控制器方法和方法参数值数组的映射创建{@link UriComponentsBuilder}.
	 * 此方法委托给{@link #fromMethod(Class, Method, Object...)}.
	 * <p><strong>Note:</strong> 此方法从"Forwarded" 和 "X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param controllerType 控制器
	 * @param methodName 方法名称
	 * @param args 参数值
	 * 
	 * @return UriComponentsBuilder实例, never {@code null}
	 * @throws IllegalArgumentException 如果没有匹配或者有多个匹配方法
	 */
	public static UriComponentsBuilder fromMethodName(Class<?> controllerType,
			String methodName, Object... args) {

		Method method = getMethod(controllerType, methodName, args);
		return fromMethodInternal(null, controllerType, method, args);
	}

	/**
	 * {@link #fromMethodName(Class, String, Object...)}的替代方法, 它接受表示基本URL的{@code UriComponentsBuilder}.
	 * 在处理请求的上下文之外使用MvcUriComponentsBuilder或应用与当前请求不匹配的自定义baseUrl时, 这很有用.
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param builder 基本URL的构建器; 构建器将被克隆, 因此不会被修改, 可以重新用于进一步调用.
	 * @param controllerType 控制器
	 * @param methodName 方法名称
	 * @param args 参数值
	 * 
	 * @return UriComponentsBuilder实例, never {@code null}
	 * @throws IllegalArgumentException 如果没有匹配或者有多个匹配方法
	 */
	public static UriComponentsBuilder fromMethodName(UriComponentsBuilder builder,
			Class<?> controllerType, String methodName, Object... args) {

		Method method = getMethod(controllerType, methodName, args);
		return fromMethodInternal(builder, controllerType, method, args);
	}

	/**
	 * 通过调用"mock"控制器方法创建{@link UriComponentsBuilder}.
	 * 然后使用控制器方法和提供的参数值委托给{@link #fromMethod(Class, Method, Object...)}.
	 * <p>例如, 给定此控制器:
	 * <pre class="code">
	 * &#064;RequestMapping("/people/{id}/addresses")
	 * class AddressController {
	 *
	 *   &#064;RequestMapping("/{country}")
	 *   public HttpEntity<Void> getAddressesForCountry(&#064;PathVariable String country) { ... }
	 *
	 *   &#064;RequestMapping(value="/", method=RequestMethod.POST)
	 *   public void addAddress(Address address) { ... }
	 * }
	 * </pre>
	 * 可以创建UriComponentsBuilder:
	 * <pre class="code">
	 * // Inline style with static import of "MvcUriComponentsBuilder.on"
	 *
	 * MvcUriComponentsBuilder.fromMethodCall(
	 * 		on(AddressController.class).getAddressesForCountry("US")).buildAndExpand(1);
	 *
	 * // Longer form useful for repeated invocation (and void controller methods)
	 *
	 * AddressController controller = MvcUriComponentsBuilder.on(AddressController.class);
	 * controller.addAddress(null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(controller);
	 * controller.getAddressesForCountry("US")
	 * builder = MvcUriComponentsBuilder.fromMethodCall(controller);
	 * </pre>
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param info 调用之后从"mock"控制器调用返回的值或"mock"控制器本身
	 * 
	 * @return UriComponents实例
	 */
	public static UriComponentsBuilder fromMethodCall(Object info) {
		Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");
		MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;
		Class<?> controllerType = invocationInfo.getControllerType();
		Method method = invocationInfo.getControllerMethod();
		Object[] arguments = invocationInfo.getArgumentValues();
		return fromMethodInternal(null, controllerType, method, arguments);
	}

	/**
	 * {@link #fromMethodCall(Object)}的替代方法, 它接受表示基本URL的{@code UriComponentsBuilder}.
	 * 在处理请求的上下文之外使用MvcUriComponentsBuilder或应用与当前请求不匹配的自定义baseUrl时, 这很有用.
	 * <p><strong>Note:</strong> 此方法从"Forwarded" 和 "X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param builder 基本URL的构建器; 构建器将被克隆, 因此不会被修改, 可以重新用于进一步调用.
	 * @param info 调用之后从"mock"控制器调用返回的值或"mock"控制器本身
	 * 
	 * @return UriComponents实例
	 */
	public static UriComponentsBuilder fromMethodCall(UriComponentsBuilder builder, Object info) {
		Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");
		MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;
		Class<?> controllerType = invocationInfo.getControllerType();
		Method method = invocationInfo.getControllerMethod();
		Object[] arguments = invocationInfo.getArgumentValues();
		return fromMethodInternal(builder, controllerType, method, arguments);
	}

	/**
	 * 从Spring MVC控制器方法的请求映射的名称创建URL.
	 * <p>配置的
	 * {@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
	 * HandlerMethodMappingNamingStrategy}确定启动时控制器方法请求映射的名称.
	 * 默认情况下, 所有映射都根据类名的大写字母分配名称, 后跟"#"作为分隔符, 然后是方法名称.
	 * 例如, 对于名为PersonController的类, 使用方法getPerson的"PC#getPerson".
	 * 如果命名约定不产生唯一结果, 则可以通过{@code @RequestMapping}注解的name属性指定显式名称.
	 * <p>这主要用于视图渲染技术和EL表达式.
	 * Spring URL标记库将此方法注册为名为"mvcUrl"的函数.
	 * <p>例如, 给定此控制器:
	 * <pre class="code">
	 * &#064;RequestMapping("/people")
	 * class PersonController {
	 *
	 *   &#064;RequestMapping("/{id}")
	 *   public HttpEntity<Void> getPerson(&#064;PathVariable String id) { ... }
	 *
	 * }
	 * </pre>
	 *
	 * JSP可以为控制器方法准备URL, 如下所示:
	 *
	 * <pre class="code">
	 * <%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
	 *
	 * &lt;a href="${s:mvcUrl('PC#getPerson').arg(0,"123").build()}"&gt;Get Person&lt;/a&gt;
	 * </pre>
	 * <p>请注意, 没有必要指定所有参数.
	 * 只有准备URL所需的那些, 主要是{@code @RequestParam}和{@code @PathVariable}).
	 *
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 *
	 * @param mappingName 映射名称
	 * 
	 * @return 用于准备URI字符串的构建器
	 * @throws IllegalArgumentException 如果找不到映射名称或者没有唯一匹配
	 */
	public static MethodArgumentBuilder fromMappingName(String mappingName) {
		return fromMappingName(null, mappingName);
	}

	/**
	 * {@link #fromMappingName(String)}的替代方法, 它接受表示基本URL的{@code UriComponentsBuilder}.
	 * 在处理请求的上下文之外使用MvcUriComponentsBuilder或应用与当前请求不匹配的自定义baseUrl时, 这很有用.
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param builder 基本URL的构建器; 构建器将被克隆, 因此不会被修改, 可以重新用于进一步调用.
	 * @param name 映射名称
	 * 
	 * @return 用于准备URI字符串的构建器
	 * @throws IllegalArgumentException 如果找不到映射名称或者没有唯一匹配
	 */
	public static MethodArgumentBuilder fromMappingName(UriComponentsBuilder builder, String name) {
		RequestMappingInfoHandlerMapping handlerMapping = getRequestMappingInfoHandlerMapping();
		List<HandlerMethod> handlerMethods = handlerMapping.getHandlerMethodsForMappingName(name);
		if (handlerMethods == null) {
			throw new IllegalArgumentException("Mapping mappingName not found: " + name);
		}
		if (handlerMethods.size() != 1) {
			throw new IllegalArgumentException("No unique match for mapping mappingName " +
					name + ": " + handlerMethods);
		}
		HandlerMethod handlerMethod = handlerMethods.get(0);
		Class<?> controllerType = handlerMethod.getBeanType();
		Method method = handlerMethod.getMethod();
		return new MethodArgumentBuilder(builder, controllerType, method);
	}

	/**
	 * 从控制器方法和方法参数值数组的映射创建{@link UriComponentsBuilder}.
	 * 值数组必须与控制器方法的签名匹配.
	 * {@code @RequestParam}和{@code @PathVariable}的值用于构建URI
	 * (通过{@link org.springframework.web.method.support.UriComponentsContributor
	 * UriComponentsContributor}的实现), 而剩余的参数值被忽略, 可以是{@code null}.
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param controllerType 控制器类型
	 * @param method 控制器方法
	 * @param args 控制器方法的参数值
	 * 
	 * @return UriComponentsBuilder实例, never {@code null}
	 */
	public static UriComponentsBuilder fromMethod(Class<?> controllerType, Method method, Object... args) {
		return fromMethodInternal(null, controllerType, method, args);
	}

	/**
	 * {@link #fromMethod(Class, Method, Object...)}的替代方法, 它接受表示基本URL的{@code UriComponentsBuilder}.
	 * 在处理请求的上下文之外使用MvcUriComponentsBuilder或应用与当前请求不匹配的自定义baseUrl时, 这很有用.
	 * <p><strong>Note:</strong> 此方法从"Forwarded" 和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param baseUrl 基本URL的构建器; 构建器将被克隆, 因此不会被修改, 可以重新用于进一步调用.
	 * @param controllerType 控制器类型
	 * @param method 控制器方法
	 * @param args 控制器方法的参数值
	 * 
	 * @return UriComponentsBuilder实例 (never {@code null})
	 */
	public static UriComponentsBuilder fromMethod(UriComponentsBuilder baseUrl,
			Class<?> controllerType, Method method, Object... args) {

		return fromMethodInternal(baseUrl,
				(controllerType != null ? controllerType : method.getDeclaringClass()), method, args);
	}

	/**
	 * @deprecated 从4.2开始, 使用也接受controllerType参数的重载方法
	 */
	@Deprecated
	public static UriComponentsBuilder fromMethod(Method method, Object... args) {
		return fromMethodInternal(null, method.getDeclaringClass(), method, args);
	}

	private static UriComponentsBuilder fromMethodInternal(UriComponentsBuilder baseUrl,
			Class<?> controllerType, Method method, Object... args) {

		baseUrl = getBaseUrlToUse(baseUrl);
		String typePath = getTypeRequestMapping(controllerType);
		String methodPath = getMethodRequestMapping(method);
		String path = pathMatcher.combine(typePath, methodPath);
		baseUrl.path(path);
		UriComponents uriComponents = applyContributors(baseUrl, method, args);
		return UriComponentsBuilder.newInstance().uriComponents(uriComponents);
	}

	private static UriComponentsBuilder getBaseUrlToUse(UriComponentsBuilder baseUrl) {
		if (baseUrl != null) {
			return baseUrl.cloneBuilder();
		}
		else {
			return ServletUriComponentsBuilder.fromCurrentServletMapping();
		}
	}

	private static String getTypeRequestMapping(Class<?> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(controllerType, RequestMapping.class);
		if (requestMapping == null) {
			return "/";
		}
		String[] paths = requestMapping.path();
		if (ObjectUtils.isEmpty(paths) || StringUtils.isEmpty(paths[0])) {
			return "/";
		}
		if (paths.length > 1 && logger.isWarnEnabled()) {
			logger.warn("Multiple paths on controller " + controllerType.getName() + ", using first one");
		}
		return paths[0];
	}

	private static String getMethodRequestMapping(Method method) {
		Assert.notNull(method, "'method' must not be null");
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
		if (requestMapping == null) {
			throw new IllegalArgumentException("No @RequestMapping on: " + method.toGenericString());
		}
		String[] paths = requestMapping.path();
		if (ObjectUtils.isEmpty(paths) || StringUtils.isEmpty(paths[0])) {
			return "/";
		}
		if (paths.length > 1 && logger.isWarnEnabled()) {
			logger.warn("Multiple paths on method " + method.toGenericString() + ", using first one");
		}
		return paths[0];
	}

	private static Method getMethod(Class<?> controllerType, final String methodName, final Object... args) {
		MethodFilter selector = new MethodFilter() {
			@Override
			public boolean matches(Method method) {
				String name = method.getName();
				int argLength = method.getParameterTypes().length;
				return (name.equals(methodName) && argLength == args.length);
			}
		};
		Set<Method> methods = MethodIntrospector.selectMethods(controllerType, selector);
		if (methods.size() == 1) {
			return methods.iterator().next();
		}
		else if (methods.size() > 1) {
			throw new IllegalArgumentException(String.format(
					"Found two methods named '%s' accepting arguments %s in controller %s: [%s]",
					methodName, Arrays.asList(args), controllerType.getName(), methods));
		}
		else {
			throw new IllegalArgumentException("No method named '" + methodName + "' with " + args.length +
					" arguments found in controller " + controllerType.getName());
		}
	}

	private static UriComponents applyContributors(UriComponentsBuilder builder, Method method, Object... args) {
		CompositeUriComponentsContributor contributor = getConfiguredUriComponentsContributor();
		if (contributor == null) {
			logger.debug("Using default CompositeUriComponentsContributor");
			contributor = defaultUriComponentsContributor;
		}

		int paramCount = method.getParameterTypes().length;
		int argCount = args.length;
		if (paramCount != argCount) {
			throw new IllegalArgumentException("Number of method parameters " + paramCount +
					" does not match number of argument values " + argCount);
		}

		final Map<String, Object> uriVars = new HashMap<String, Object>();
		for (int i = 0; i < paramCount; i++) {
			MethodParameter param = new SynthesizingMethodParameter(method, i);
			param.initParameterNameDiscovery(parameterNameDiscoverer);
			contributor.contributeMethodArgument(param, args[i], builder, uriVars);
		}

		// 可能没有所有URI 变量值, 只扩展现在拥有的值
		return builder.build().expand(new UriComponents.UriTemplateVariables() {
			@Override
			public Object getValue(String name) {
				return uriVars.containsKey(name) ? uriVars.get(name) : UriComponents.UriTemplateVariables.SKIP_VALUE;
			}
		});
	}

	private static CompositeUriComponentsContributor getConfiguredUriComponentsContributor() {
		WebApplicationContext wac = getWebApplicationContext();
		if (wac == null) {
			return null;
		}
		try {
			return wac.getBean(MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME, CompositeUriComponentsContributor.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No CompositeUriComponentsContributor bean with name '" +
						MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME + "'");
			}
			return null;
		}
	}

	private static RequestMappingInfoHandlerMapping getRequestMappingInfoHandlerMapping() {
		WebApplicationContext wac = getWebApplicationContext();
		Assert.notNull(wac, "Cannot lookup handler method mappings without WebApplicationContext");
		try {
			return wac.getBean(RequestMappingInfoHandlerMapping.class);
		}
		catch (NoUniqueBeanDefinitionException ex) {
			throw new IllegalStateException("More than one RequestMappingInfoHandlerMapping beans found", ex);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new IllegalStateException("No RequestMappingInfoHandlerMapping bean", ex);
		}
	}

	private static WebApplicationContext getWebApplicationContext() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes == null) {
			logger.debug("No request bound to the current thread: not in a DispatcherServlet request?");
			return null;
		}

		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
		WebApplicationContext wac = (WebApplicationContext)
				request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (wac == null) {
			logger.debug("No WebApplicationContext found: not in a DispatcherServlet request?");
			return null;
		}
		return wac;
	}

	/**
	 * 返回"mock"控制器实例.
	 * 当调用控制器上的{@code @RequestMapping}方法时, 会记住提供的参数值,
	 * 然后可以使用结果通过{@link #fromMethodCall(Object)}创建{@code UriComponentsBuilder}.
	 * <p>请注意, 这是{@link #controller(Class)}的简写版本, 用于内联使用 (使用静态导入), 例如:
	 * <pre class="code">
	 * MvcUriComponentsBuilder.fromMethodCall(on(FooController.class).getFoo(1)).build();
	 * </pre>
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 *
	 * @param controllerType 目标控制器
	 */
	public static <T> T on(Class<T> controllerType) {
		return controller(controllerType);
	}

	/**
	 * 返回"mock"控制器实例.
	 * 当调用控制器上的{@code @RequestMapping}方法时, 会记住提供的参数值,
	 * 然后可以使用结果通过{@link #fromMethodCall(Object)}创建{@code UriComponentsBuilder}.
	 * <p>这是{@link #on(Class)}的较长版本. 控制器方法需要返回void以及重复调用.
	 * <pre class="code">
	 * FooController fooController = controller(FooController.class);
	 *
	 * fooController.saveFoo(1, null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(fooController);
	 *
	 * fooController.saveFoo(2, null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(fooController);
	 * </pre>
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 * 
	 * @param controllerType 目标控制器
	 */
	public static <T> T controller(Class<T> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		return initProxy(controllerType, new ControllerMethodInvocationInterceptor(controllerType));
	}

	@SuppressWarnings("unchecked")
	private static <T> T initProxy(Class<?> type, ControllerMethodInvocationInterceptor interceptor) {
		if (type.isInterface()) {
			ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
			factory.addInterface(type);
			factory.addInterface(MethodInvocationInfo.class);
			factory.addAdvice(interceptor);
			return (T) factory.getProxy();
		}

		else {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(type);
			enhancer.setInterfaces(new Class<?>[] {MethodInvocationInfo.class});
			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
			enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);

			Class<?> proxyClass = enhancer.createClass();
			Object proxy = null;

			if (objenesis.isWorthTrying()) {
				try {
					proxy = objenesis.newInstance(proxyClass, enhancer.getUseCache());
				}
				catch (ObjenesisException ex) {
					logger.debug("Unable to instantiate controller proxy using Objenesis, " +
							"falling back to regular construction", ex);
				}
			}

			if (proxy == null) {
				try {
					proxy = proxyClass.newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Unable to instantiate controller proxy using Objenesis, " +
							"and regular controller instantiation via default constructor fails as well", ex);
				}
			}

			((Factory) proxy).setCallbacks(new Callback[] {interceptor});
			return (T) proxy;
		}
	}

	/**
	 * {@link #fromController(Class)}的替代方法, 用于通过调用{@link #relativeTo}创建的此类的实例.
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 */
	public UriComponentsBuilder withController(Class<?> controllerType) {
		return fromController(this.baseUrl, controllerType);
	}

	/**
	 * {@link #fromMethodName(Class, String, Object...)}}的替代方法, 用于通过调用{@link #relativeTo}创建的此类的实例.
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 */
	public UriComponentsBuilder withMethodName(Class<?> controllerType, String methodName, Object... args) {
		return fromMethodName(this.baseUrl, controllerType, methodName, args);
	}

	/**
	 * {@link #fromMethodCall(Object)}的替代方法, 用于通过调用{@link #relativeTo}创建的此类的实例.
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 */
	public UriComponentsBuilder withMethodCall(Object invocationInfo) {
		return fromMethodCall(this.baseUrl, invocationInfo);
	}

	/**
	 * {@link #fromMappingName(String)}的替代方法, 用于通过调用{@link #relativeTo}创建的此类的实例.
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 */
	public MethodArgumentBuilder withMappingName(String mappingName) {
		return fromMappingName(this.baseUrl, mappingName);
	}

	/**
	 * {@link #fromMethod(Class, Method, Object...)}的替代方法, 用于通过调用{@link #relativeTo}创建的此类的实例.
	 * <p><strong>Note:</strong> 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 */
	public UriComponentsBuilder withMethod(Class<?> controllerType, Method method, Object... args) {
		return fromMethod(this.baseUrl, controllerType, method, args);
	}


	public interface MethodInvocationInfo {

		Class<?> getControllerType();

		Method getControllerMethod();

		Object[] getArgumentValues();
	}


	private static class ControllerMethodInvocationInterceptor
			implements org.springframework.cglib.proxy.MethodInterceptor, MethodInterceptor {

		private static final Method getControllerMethod =
				ReflectionUtils.findMethod(MethodInvocationInfo.class, "getControllerMethod");

		private static final Method getArgumentValues =
				ReflectionUtils.findMethod(MethodInvocationInfo.class, "getArgumentValues");

		private static final Method getControllerType =
				ReflectionUtils.findMethod(MethodInvocationInfo.class, "getControllerType");

		private final Class<?> controllerType;

		private Method controllerMethod;

		private Object[] argumentValues;

		ControllerMethodInvocationInterceptor(Class<?> controllerType) {
			this.controllerType = controllerType;
		}

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) {
			if (getControllerType.equals(method)) {
				return this.controllerType;
			}
			else if (getControllerMethod.equals(method)) {
				return this.controllerMethod;
			}
			else if (getArgumentValues.equals(method)) {
				return this.argumentValues;
			}
			else if (ReflectionUtils.isObjectMethod(method)) {
				return ReflectionUtils.invokeMethod(method, obj, args);
			}
			else {
				this.controllerMethod = method;
				this.argumentValues = args;
				Class<?> returnType = method.getReturnType();
				try {
					return (returnType == void.class ? null : returnType.cast(initProxy(returnType, this)));
				}
				catch (Throwable ex) {
					throw new IllegalStateException(
							"Failed to create proxy for controller method return type: " + method, ex);
				}
			}
		}

		@Override
		public Object invoke(org.aopalliance.intercept.MethodInvocation inv) throws Throwable {
			return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), null);
		}
	}


	public static class MethodArgumentBuilder {

		private final Class<?> controllerType;

		private final Method method;

		private final Object[] argumentValues;

		private final UriComponentsBuilder baseUrl;

		public MethodArgumentBuilder(Class<?> controllerType, Method method) {
			this(null, controllerType, method);
		}

		public MethodArgumentBuilder(UriComponentsBuilder baseUrl, Class<?> controllerType, Method method) {
			Assert.notNull(controllerType, "'controllerType' is required");
			Assert.notNull(method, "'method' is required");
			this.baseUrl = (baseUrl != null ? baseUrl : initBaseUrl());
			this.controllerType = controllerType;
			this.method = method;
			this.argumentValues = new Object[method.getParameterTypes().length];
			for (int i = 0; i < this.argumentValues.length; i++) {
				this.argumentValues[i] = null;
			}
		}

		/**
		 * @deprecated 从4.2开始, 使用接受controllerType参数的替代构造函数
		 */
		@Deprecated
		public MethodArgumentBuilder(Method method) {
			this(method.getDeclaringClass(), method);
		}

		private static UriComponentsBuilder initBaseUrl() {
			UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentServletMapping();
			return UriComponentsBuilder.fromPath(builder.build().getPath());
		}

		public MethodArgumentBuilder arg(int index, Object value) {
			this.argumentValues[index] = value;
			return this;
		}

		public String build() {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method,
					this.argumentValues).build(false).encode().toUriString();
		}

		public String buildAndExpand(Object... uriVars) {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method,
					this.argumentValues).build(false).expand(uriVars).encode().toString();
		}
	}
}
