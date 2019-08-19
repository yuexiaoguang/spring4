package org.springframework.web.servlet.handler;

import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * {@link HandlerExceptionResolver}实现的抽象基类.
 *
 * <p>支持应用解析器并实现{@link Ordered}接口的映射的
 * {@linkplain #setMappedHandlers 处理器}和{@linkplain #setMappedHandlerClasses 处理器类}.
 */
public abstract class AbstractHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

	private static final String HEADER_CACHE_CONTROL = "Cache-Control";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.LOWEST_PRECEDENCE;

	private Set<?> mappedHandlers;

	private Class<?>[] mappedHandlerClasses;

	private Log warnLogger;

	private boolean preventResponseCaching = false;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 指定此异常解析器应应用于的处理器.
	 * <p>异常映射和默认错误视图仅适用于指定的处理器.
	 * <p>如果未设置处理器或处理器类, 则异常映射和默认错误视图将应用于所有处理器.
	 * 这意味着指定的默认错误视图将用作所有异常的回退; 在这种情况下, 链中的任何其他HandlerExceptionResolver都将被忽略.
	 */
	public void setMappedHandlers(Set<?> mappedHandlers) {
		this.mappedHandlers = mappedHandlers;
	}

	/**
	 * 指定此异常解析器应应用于的类.
	 * <p>异常映射和默认错误视图仅适用于指定类型的处理器; 指定的类型也可以是处理器的接口或超类.
	 * <p>如果未设置处理器或处理器类, 则异常映射和默认错误视图将应用于所有处理器.
	 * 这意味着指定的默认错误视图将用作所有异常的回退; 在这种情况下, 链中的任何其他HandlerExceptionResolver都将被忽略.
	 */
	public void setMappedHandlerClasses(Class<?>... mappedHandlerClasses) {
		this.mappedHandlerClasses = mappedHandlerClasses;
	}

	/**
	 * 设置警告日志记录的日志类别.
	 * 该名称将通过Commons Logging传递给底层记录器实现, 根据记录器的配置被解释为日志类别.
	 * <p>默认是没有警告日志记录. 指定此设置可激活警告日志到特定类别.
	 * 或者, 覆盖{@link #logException}方法以进行自定义日志.
	 */
	public void setWarnLogCategory(String loggerName) {
		this.warnLogger = LogFactory.getLog(loggerName);
	}

	/**
	 * 指定是否禁止此异常解析器解析的视图的HTTP响应缓存.
	 * <p>默认为{@code false}.
	 * 将其切换为{@code true}, 以便自动生成禁止响应缓存的HTTP响应header.
	 */
	public void setPreventResponseCaching(boolean preventResponseCaching) {
		this.preventResponseCaching = preventResponseCaching;
	}


	/**
	 * 检查是否应该应用此解析器 (i.e. 如果提供的处理器与任何已配置的{@linkplain #setMappedHandlers 处理器}
	 * 或{@linkplain #setMappedHandlerClasses 处理器类}匹配), 然后委托给{@link #doResolveException}模板方法.
	 */
	@Override
	public ModelAndView resolveException(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

		if (shouldApplyTo(request, handler)) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Resolving exception from handler [" + handler + "]: " + ex);
			}
			prepareResponse(ex, response);
			ModelAndView result = doResolveException(request, response, handler, ex);
			if (result != null) {
				logException(ex, request);
			}
			return result;
		}
		else {
			return null;
		}
	}

	/**
	 * 检查此解析器是否应该应用于给定的处理器.
	 * <p>默认实现检查配置的{@linkplain #setMappedHandlers 处理器}和{@linkplain #setMappedHandlerClasses 处理器类}.
	 * 
	 * @param request 当前的HTTP请求
	 * @param handler 执行的处理器, 或{@code null} 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * 
	 * @return 是否应继续解析给定请求和处理器的异常
	 */
	protected boolean shouldApplyTo(HttpServletRequest request, Object handler) {
		if (handler != null) {
			if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
				return true;
			}
			if (this.mappedHandlerClasses != null) {
				for (Class<?> handlerClass : this.mappedHandlerClasses) {
					if (handlerClass.isInstance(handler)) {
						return true;
					}
				}
			}
		}
		// 否则仅在没有显式处理器映射时才适用.
		return (this.mappedHandlers == null && this.mappedHandlerClasses == null);
	}

	/**
	 * 如果已通过{@link #setWarnLogCategory "warnLogCategory"}属性激活警告日志记录, 则在警告级别记录给定的异常.
	 * <p>调用{@link #buildLogMessage}以确定要记录的具体消息.
	 * 
	 * @param ex 在处理器执行期间抛出的异常
	 * @param request 当前HTTP请求 (对获取元数据很有用)
	 */
	protected void logException(Exception ex, HttpServletRequest request) {
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			this.warnLogger.warn(buildLogMessage(ex, request));
		}
	}

	/**
	 * 构建在处理给定请求期间发生的给定异常的日志消息.
	 * 
	 * @param ex 在处理器执行期间抛出的异常
	 * @param request 当前HTTP请求 (对获取元数据很有用)
	 * 
	 * @return 要使用的日志消息
	 */
	protected String buildLogMessage(Exception ex, HttpServletRequest request) {
		return "Resolved exception caused by Handler execution: " + ex;
	}

	/**
	 * 为异常情况准备响应.
	 * <p>如果{@link #setPreventResponseCaching "preventResponseCaching"} 属性已设置为"true",
	 * 则默认实现会阻止响应被缓存.
	 * 
	 * @param ex 在处理器执行期间抛出的异常
	 * @param response 当前的HTTP响应
	 */
	protected void prepareResponse(Exception ex, HttpServletResponse response) {
		if (this.preventResponseCaching) {
			preventCaching(response);
		}
	}

	/**
	 * 通过设置相应的HTTP {@code Cache-Control: no-store} header来阻止缓存响应.
	 * 
	 * @param response 当前的HTTP响应
	 */
	protected void preventCaching(HttpServletResponse response) {
		response.addHeader(HEADER_CACHE_CONTROL, "no-store");
	}


	/**
	 * 实际解析在处理器执行期间抛出的给定异常, 如果合适, 返回表示特定错误页面的{@link ModelAndView}.
	 * <p>可以在子类中重写, 以便应用特定的异常检查.
	 * 请注意, 在检查此解析是否适用("mappedHandlers" etc)<i>之后</i>,
	 * 将调用此模板方法, 因此实现可能只是继续其实际的异常处理.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器, 或{@code null} 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 要转发到的相应{@code ModelAndView}, 或{@code null}以进行解析链中的默认处理
	 */
	protected abstract ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);

}
