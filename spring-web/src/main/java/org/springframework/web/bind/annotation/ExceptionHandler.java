package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于在特定处理器类和/或处理器方法中处理异常.
 * 在Servlet和Portlet环境之间提供一致的样式, 并使用适应具体环境的语义.
 *
 * <p>使用此注解的处理器方法允许具有非常灵活的签名.
 * 它们可以以任意顺序具有以下类型的参数:
 * <ul>
 * <li>异常参数: 声明为一般异常或更具体的异常.
 * 如果注解本身不通过其{@link #value()}缩小异常类型, 这也可以作为映射提示.
 * 
 * <li>请求和/或响应对象 (Servlet API 或Portlet API). 可以选择任何特定的请求/响应类型, e.g.
 * {@link javax.servlet.ServletRequest} / {@link javax.servlet.http.HttpServletRequest}
 * 或{@link javax.portlet.PortletRequest} / {@link javax.portlet.ActionRequest} / {@link javax.portlet.RenderRequest}.
 * 请注意, 在Portlet的情况下, 显式声明的 action/render参数也用于将特定请求类型映射到处理器方法
 * (如果没有给出区分action和render请求的其他信息).
 * 
 * <li>Session对象 (Servlet API 或 Portlet API):
 * {@link javax.servlet.http.HttpSession}或{@link javax.portlet.PortletSession}.
 * 此类型的参数将强制存在相应的会话. 因此, 这样的参数永远不会是{@code null}.
 * <i>请注意, 会话访问可能不是线程安全的, 特别是在Servlet环境中:
 * 如果允许多个请求同时访问会话, 将
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setSynchronizeOnSession
 * "synchronizeOnSession"}标志切换为"true".</i>
 * 
 * <li>{@link org.springframework.web.context.request.WebRequest}
 * 或{@link org.springframework.web.context.request.NativeWebRequest}.
 * 允许通用请求参数访问以及请求/会话属性访问, 而不与本机Servlet/Portlet API绑定.
 * 
 * <li>当前请求区域设置的{@link java.util.Locale}
 * (由可用的最具体的区域设置解析器确定, i.e. Servlet环境中配置的{@link org.springframework.web.servlet.LocaleResolver}
 * 和Portlet环境中的portal区域设置).
 * 
 * <li>用于访问请求的内容的{@link java.io.InputStream} / {@link java.io.Reader}.
 * 这将是Servlet/Portlet API公开的原始InputStream/Reader.
 * 
 * <li>用于生成响应的内容的{@link java.io.OutputStream} / {@link java.io.Writer}.
 * 这将是Servlet/Portlet API公开的原始 OutputStream/Writer.
 * 
 * <li>{@link org.springframework.ui.Model}作为从处理器方法返回model映射的替代方法.
 * 请注意, 提供的模型不预先填充常规模型属性, 因此始终为空, 以便为特定于异常的视图准备模型.
 * </ul>
 *
 * <p>处理器方法支持以下返回类型:
 * <ul>
 * <li>{@code ModelAndView}对象 (Servlet MVC 或 Portlet MVC).
 * 
 * <li>{@link org.springframework.ui.Model}对象,
 * 其视图名称通过{@link org.springframework.web.servlet.RequestToViewNameTranslator}隐式确定.
 * 
 * <li>{@link java.util.Map}对象, 用于公开模型,
 * 其视图名称通过 {@link org.springframework.web.servlet.RequestToViewNameTranslator}隐式确定.
 * 
 * <li>{@link org.springframework.web.servlet.View}对象.
 * 
 * <li>{@link String}值, 被解释为视图名称.
 * 
 * <li>带{@link ResponseBody @ResponseBody}注解的方法 (仅限Servlet), 设置响应内容.
 * 使用{@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}将返回值转换为响应流.
 * 
 * <li>{@link org.springframework.http.HttpEntity HttpEntity&lt;?&gt;}
 * 或{@link org.springframework.http.ResponseEntity ResponseEntity&lt;?&gt;}对象 (仅限Servlet), 设置响应header和内容.
 * ResponseEntity正文将被转换, 并使用
 * {@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}写入响应流.
 * 
 * <li>{@code void}, 如果方法处理响应本身 (通过直接写入响应内容, 声明
 * {@link javax.servlet.ServletResponse} / {@link javax.servlet.http.HttpServletResponse}
 * / {@link javax.portlet.RenderResponse}类型的参数)
 * 或者如果视图名称应该通过
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator}隐式确定
 * (不在处理器方法签名中声明响应参数; 仅适用于Servlet环境).
 * </ul>
 *
 * <p>在Servlet环境中, 可以将{@code ExceptionHandler}注解和{@link ResponseStatus @ResponseStatus}结合使用,
 * 以定义HTTP响应的响应状态.
 *
 * <p><b>Note:</b> 在Portlet环境中, 带{@code ExceptionHandler}注解的方法只会在渲染(render)和资源阶段被调用
 * - 就像{@link org.springframework.web.portlet.HandlerExceptionResolver} beans一样.
 * 在render阶段也会调用从action和event阶段结转的异常, 并且必须在控制器类上存在异常处理器方法,
 * 该控制器类定义适用的<i>render</i>方法.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionHandler {

	/**
	 * 带注解的方法处理的异常.
	 * 如果为空, 则默认为方法参数列表中列出的所有异常.
	 */
	Class<? extends Throwable>[] value() default {};

}
