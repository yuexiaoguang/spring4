package org.springframework.web.servlet.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver}实现的抽象基类,
 * 支持处理类型为{@link HandlerMethod}的处理器的异常.
 */
public abstract class AbstractHandlerMethodExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * 检查处理器是否为{@link HandlerMethod},
	 * 然后委托给{@code #shouldApplyTo(HttpServletRequest, Object)}的基类实现传递{@code HandlerMethod}的bean.
	 * 否则返回{@code false}.
	 */
	@Override
	protected boolean shouldApplyTo(HttpServletRequest request, Object handler) {
		if (handler == null) {
			return super.shouldApplyTo(request, handler);
		}
		else if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			handler = handlerMethod.getBean();
			return super.shouldApplyTo(request, handler);
		}
		else {
			return false;
		}
	}

	@Override
	protected final ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

		return doResolveHandlerMethodException(request, response, (HandlerMethod) handler, ex);
	}

	/**
	 * 实际解析在处理器执行期间抛出的给定异常, 返回表示特定错误页面的ModelAndView.
	 * <p>可以在子类中重写, 以便应用特定的异常检查.
	 * 请注意, 在检查此解析是否适用("mappedHandlers" etc)<i>后</i>, 将调用此模板方法,
	 * 因此实现可能只是继续其实际的异常处理.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handlerMethod 执行的处理器方法, 或{@code null}, 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 转发到的响应ModelAndView, 或{@code null}进行默认处理
	 */
	protected abstract ModelAndView doResolveHandlerMethodException(
			HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod, Exception ex);

}
