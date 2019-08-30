package org.springframework.web.portlet;

import javax.portlet.PortletException;

/**
 * 应该转发到使用特定模型的特定视图的错误条件上抛出的异常.
 *
 * <p>可以在处理器处理期间随时抛出. 这包括预构建控制器的任何模板方法.
 * 例如, 如果某些参数不允许继续正常的工作流, 则表单控制器可能会中止到特定的错误页面.
 */
@SuppressWarnings("serial")
public class ModelAndViewDefiningException extends PortletException {

	private ModelAndView modelAndView;


	/**
	 * @param modelAndView 具有要转发到的视图和要公开的模型的ModelAndView, 通常表示特定的错误页面.
	 */
	public ModelAndViewDefiningException(ModelAndView modelAndView) {
		if (modelAndView == null) {
			throw new IllegalArgumentException("modelAndView must not be null in ModelAndViewDefiningException");
		}
		this.modelAndView = modelAndView;
	}

	/**
	 * 返回此异常包含的要进行转发的ModelAndView.
	 */
	public ModelAndView getModelAndView() {
		return modelAndView;
	}

}
