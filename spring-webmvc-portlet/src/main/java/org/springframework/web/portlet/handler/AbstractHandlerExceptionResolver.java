package org.springframework.web.portlet.handler;

import java.util.Set;
import javax.portlet.MimeResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.WindowState;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.web.portlet.HandlerExceptionResolver;
import org.springframework.web.portlet.ModelAndView;

/**
 * {@link HandlerExceptionResolver}实现的抽象基类.
 *
 * <p>提供解析器应映射到的一组映射的处理器, 以及{@link Ordered}实现.
 */
public abstract class AbstractHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.LOWEST_PRECEDENCE;

	private Set<?> mappedHandlers;

	private Class<?>[] mappedHandlerClasses;

	private Log warnLogger;

	private boolean renderWhenMinimized = false;


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
	 * 这意味着指定的默认错误视图将用作所有异常的回退; 在这种情况下, 链中的任何其他HandlerExceptionResolvers都将被忽略.
	 */
	public void setMappedHandlers(Set<?> mappedHandlers) {
		this.mappedHandlers = mappedHandlers;
	}

	/**
	 * 指定此异常解析器应应用于的类.
	 * <p>异常映射和默认错误视图仅适用于指定类型的处理器; 指定的类型也可以是处理器的接口或超类.
	 * <p>如果未设置处理器或处理器类, 则异常映射和默认错误视图将应用于所有处理器.
	 * 这意味着指定的默认错误视图将用作所有异常的回退; 在这种情况下, 链中的任何其他HandlerExceptionResolvers都将被忽略.
	 */
	public void setMappedHandlerClasses(Class<?>... mappedHandlerClasses) {
		this.mappedHandlerClasses = mappedHandlerClasses;
	}

	/**
	 * 设置警告日志记录的日志类别.
	 * 该名称将通过Commons Logging传递给底层记录器实现, 根据记录器的配置被解释为日志类别.
	 * <p>默认没有警告日志记录. 指定此设置可激活警告日志记录到特定类别.
	 * 或者, 覆盖{@link #logException}方法以进行自定义日志记录.
	 */
	public void setWarnLogCategory(String loggerName) {
		this.warnLogger = LogFactory.getLog(loggerName);
	}

	/**
	 * 设置当portlet处于最小化窗口时解析器是否应渲染视图.
	 * 默认为"false".
	 */
	public void setRenderWhenMinimized(boolean renderWhenMinimized) {
		this.renderWhenMinimized = renderWhenMinimized;
	}


	/**
	 * 检查是否应该应用此解析器 (i.e. 在已指定"mappedHandlers"的情况下处理器匹配), 然后委托给{@link #doResolveException}模板方法.
	 */
	@Override
	public ModelAndView resolveException(RenderRequest request, RenderResponse response, Object handler, Exception ex) {
		if (shouldApplyTo(request, handler)) {
			return doResolveException(request, response, handler, ex);
		}
		else {
			return null;
		}
	}

	@Override
	public ModelAndView resolveException(ResourceRequest request, ResourceResponse response, Object handler, Exception ex) {
		if (shouldApplyTo(request, handler)) {
			return doResolveException(request, response, handler, ex);
		}
		else {
			return null;
		}
	}

	/**
	 * 检查此解析器是否应该应用于给定的处理器.
	 * <p>默认实现检查指定的映射处理器和处理器类, 并检查窗口状态 (根据"renderWhenMinimize"属性).
	 * 
	 * @param request 当前的portlet请求
	 * @param handler 执行的处理器, 如果在异常时没有选择, 则为{@code null} (例如, 如果multipart解析失败)
	 * 
	 * @return 此解析是否应继续解析给定请求和处理器的异常
	 */
	protected boolean shouldApplyTo(PortletRequest request, Object handler) {
		// 如果portlet最小化, 我们不想渲染, 则返回null.
		if (WindowState.MINIMIZED.equals(request.getWindowState()) && !this.renderWhenMinimized) {
			return false;
		}
		// Check mapped handlers...
		if (handler != null) {
			if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
				return true;
			}
			if (this.mappedHandlerClasses != null) {
				for (Class<?> mappedClass : this.mappedHandlerClasses) {
					if (mappedClass.isInstance(handler)) {
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
	 * @param request 当前portlet请求 (对获取元数据很有用)
	 */
	protected void logException(Exception ex, PortletRequest request) {
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			this.warnLogger.warn(buildLogMessage(ex, request));
		}
	}

	/**
	 * 在处理给定请求期间构建给定异常的日志消息.
	 * 
	 * @param ex 在处理器执行期间抛出的异常
	 * @param request 当前portlet请求 (对获取元数据很有用)
	 * 
	 * @return 要使用的日志消息
	 */
	protected String buildLogMessage(Exception ex, PortletRequest request) {
		return "Handler execution resulted in exception: " + ex;
	}


	/**
	 * 实际解析在处理器执行期间抛出的给定异常, 返回表示特定错误页面的ModelAndView.
	 * <p>必须在子类中重写, 才能应用特定的异常检查.
	 * 请注意, 在检查此解析是否适用<i>之后</i> ("mappedHandlers" etc), 将调用此模板方法,
	 * 因此实现可能只是继续其实际的异常处理.
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * @param handler 执行的处理器, 如果在异常时没有选择, 则返回null (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 要转发的相应ModelAndView, 或null以默认处理
	 */
	protected abstract ModelAndView doResolveException(PortletRequest request, MimeResponse response,
			Object handler, Exception ex);

}
