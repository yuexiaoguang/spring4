package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * 简单的控制器, 它总是返回预先配置的视图, 并可选择设置响应状态码.
 * 可以使用提供的配置属性配置视图和状态.
 */
public class ParameterizableViewController extends AbstractController {

	private Object view;

	private HttpStatus statusCode;

	private boolean statusOnly;


	public ParameterizableViewController() {
		super(false);
		setSupportedMethods(HttpMethod.GET.name(), HttpMethod.HEAD.name());
	}

	/**
	 * 设置要返回的ModelAndView的视图名称, 由DispatcherServlet通过ViewResolver解析.
	 * 将覆盖任何预先存在的视图名称或View.
	 */
	public void setViewName(String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回要委托给的视图的名称, 或{@code null} 如果使用View实例.
	 */
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * 设置要返回的ModelAndView的View对象.
	 * 将覆盖任何预先存在的视图名称或View.
	 */
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * 返回View对象, 或{@code null}, 如果使用视图名称由DispatcherServlet通过ViewResolver解析.
	 */
	public View getView() {
		return (this.view instanceof View ? (View) this.view : null);
	}

	/**
	 * 配置此控制器应在响应上设置的HTTP状态码.
	 * <p>当配置"redirect:"前缀的视图名称时, 不需要设置此属性, 因为RedirectView将执行此操作.
	 * 但是, 此属性仍可用于覆盖{@code RedirectView}的3xx状态代码.
	 * 要完全控制重定向, 提供{@code RedirectView}实例.
	 * <p>如果状态码为204且未配置视图, 则在控制器内完全处理请求.
	 */
	public void setStatusCode(HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * 返回配置的HTTP状态代码或{@code null}.
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}


	/**
	 * 该属性可用于指示请求是在控制器内完全处理的, 并且不应使用任何视图进行渲染.
	 * 与{@link #setStatusCode}结合使用.
	 * <p>默认为 {@code false}.
	 */
	public void setStatusOnly(boolean statusOnly) {
		this.statusOnly = statusOnly;
	}

	/**
	 * 请求是否在控制器内完全处理.
	 */
	public boolean isStatusOnly() {
		return this.statusOnly;
	}


	/**
	 * 返回具有指定视图名称的ModelAndView对象.
	 * <p>{@link RequestContextUtils#getInputFlashMap "input" FlashMap}的内容也添加到模型中.
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String viewName = getViewName();

		if (getStatusCode() != null) {
			if (getStatusCode().is3xxRedirection()) {
				request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, getStatusCode());
				viewName = (viewName != null && !viewName.startsWith("redirect:") ? "redirect:" + viewName : viewName);
			}
			else {
				response.setStatus(getStatusCode().value());
				if (isStatusOnly() || (getStatusCode().equals(HttpStatus.NO_CONTENT) && getViewName() == null)) {
					return null;
				}
			}
		}

		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addAllObjects(RequestContextUtils.getInputFlashMap(request));

		if (getViewName() != null) {
			modelAndView.setViewName(viewName);
		}
		else {
			modelAndView.setView(getView());
		}

		return (isStatusOnly() ? null : modelAndView);
	}

}
