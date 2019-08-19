package org.springframework.web.servlet.config.annotation;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * 定义回调方法, 通过{@code @EnableWebMvc}自定义Spring MVC的基于Java的配置.
 *
 * <p>使用{@code @EnableWebMvc}注解的配置类可以实现此接口的回调, 并有机会自定义默认配置.
 * 考虑扩展{@link WebMvcConfigurerAdapter}, 它提供所有接口方法的存根实现.
 */
public interface WebMvcConfigurer {

	/**
	 * 帮助配置HandlerMappings路径匹配选项, 例如尾部斜杠匹配, 后缀注册, 路径匹配器和路径帮助器.
	 * 共享配置的路径匹配器和路径帮助器实例:
	 * <ul>
	 * <li>RequestMappings</li>
	 * <li>ViewControllerMappings</li>
	 * <li>ResourcesMappings</li>
	 * </ul>
	 */
	void configurePathMatch(PathMatchConfigurer configurer);

	/**
	 * 配置内容协商选项.
	 */
	void configureContentNegotiation(ContentNegotiationConfigurer configurer);

	/**
	 * 配置异步请求处理选项.
	 */
	void configureAsyncSupport(AsyncSupportConfigurer configurer);

	/**
	 * 配置默认的处理器, 通过转发到Servlet容器的"默认" servlet来委派未处理的请求.
	 * 一个常见的用例是当{@link DispatcherServlet}映射到 "/"时, 从而覆盖Servlet容器对静态资源的默认处理.
	 */
	void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer);

	/**
	 * 除了默认注册的那些之外, 还添加{@link Converter}和{@link Formatter}.
	 */
	void addFormatters(FormatterRegistry registry);

	/**
	 * 添加Spring MVC生命周期拦截器, 用于控制器方法调用的预处理和后处理.
	 * 可以注册应用于所有请求或限制URL模式的子集的拦截器.
	 * <p><strong>Note</strong> 此处注册的拦截器仅适用于控制器, 而不适用于资源处理器请求.
	 * 要拦截静态资源请求, 要么声明
	 * {@link org.springframework.web.servlet.handler.MappedInterceptor MappedInterceptor} bean,
	 * 要么通过扩展
	 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
	 * WebMvcConfigurationSupport}切换到高级配置模式, 然后覆盖{@code resourceHandlerMapping}.
	 */
	void addInterceptors(InterceptorRegistry registry);

	/**
	 * 添加提供静态资源的处理器, 例如Web应用程序根目录, 类路径等中的特定位置的图像, js和css文件.
	 */
	void addResourceHandlers(ResourceHandlerRegistry registry);

	/**
	 * 配置跨源请求处理.
	 */
	void addCorsMappings(CorsRegistry registry);

	/**
	 * 配置预配置了响应状态码和/或渲染响应主体的视图的简单自动控制器.
	 * 这在不需要自定义控制器逻辑的情况下很有用 -- e.g. 呈现主页,
	 * 执行简单的站点URL重定向, 返回带有HTML内容的404状态, 带有无内容的204等等.
	 */
	void addViewControllers(ViewControllerRegistry registry);

	/**
	 * 配置视图解析器, 将从控制器返回的基于字符串的视图名称
	 * 转换为具体的{@link org.springframework.web.servlet.View}实现, 以执行渲染.
	 */
	void configureViewResolvers(ViewResolverRegistry registry);

	/**
	 * 添加支持自定义控制器方法参数类型的解析器.
	 * <p>这不会覆盖对解析处理器方法参数的内置支持.
	 * 要自定义内置的参数解析支持, 直接配置{@link RequestMappingHandlerAdapter}.
	 * 
	 * @param argumentResolvers 最初是一个空列表
	 */
	void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers);

	/**
	 * 添加支持自定义控制器方法返回值类型的处理器.
	 * <p>使用此选项不会覆盖处理返回值的内置支持.
	 * 要自定义处理返回值的内置支持, 直接配置RequestMappingHandlerAdapter.
	 * 
	 * @param returnValueHandlers 最初是一个空列表
	 */
	void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers);

	/**
	 * 配置用于读取或写入请求或响应的正文的{@link HttpMessageConverter}.
	 * 如果未添加转换器, 则会注册默认的转换器列表.
	 * <p><strong>Note</strong> 将转换器添加到列表, 关闭默认转换器注册.
	 * 要简单地添加转换器而不影响默认注册, 考虑使用方法{@link #extendMessageConverters(java.util.List)}.
	 * 
	 * @param converters 最初是一个空的转换器列表
	 */
	void configureMessageConverters(List<HttpMessageConverter<?>> converters);

	/**
	 * 用于在配置转换器列表后, 扩展或修改转换器列表的挂钩.
	 * 例如, 这可能有用, 允许注册默认转换器, 然后通过此方法插入自定义转换器.
	 * 
	 * @param converters 要扩展的配置的转换器列表
	 */
	void extendMessageConverters(List<HttpMessageConverter<?>> converters);

	/**
	 * 配置异常解析器.
	 * <p>给定列表开始为空. 如果它为空, 则框架配置一组默认的解析器,
	 * 请参阅{@link WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List)}.
	 * 或者, 如果将任何异常解析器添加到列表中, 则应用程序将有效地接管并且必须提供完全初始化的异常解析器.
	 * <p>或者, 可以使用{@link #extendHandlerExceptionResolvers(List)},
	 * 它允许扩展或修改默认配置的异常解析器列表.
	 * 
	 * @param exceptionResolvers 最初是一个空列表
	 */
	void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers);

	/**
	 * 扩展或修改默认配置的异常解析器列表.
	 * 这对于插入自定义异常解析器, 而不干扰默认解析器非常有用.
	 * 
	 * @param exceptionResolvers 要扩展的已配置的解析器列表
	 */
	void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers);

	/**
	 * 提供自定义{@link Validator}, 而不是默认创建的自定义{@link Validator}.
	 * 假设JSR-303在类路径上, 默认实现是:
	 * {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}.
	 * 将返回值保留为{@code null}以保持默认值.
	 */
	Validator getValidator();

	/**
	 * 提供自定义{@link MessageCodesResolver}, 用于根据数据绑定和验证错误码构建消息码.
	 * 将返回值保留为{@code null}以保持默认值.
	 */
	MessageCodesResolver getMessageCodesResolver();

}
