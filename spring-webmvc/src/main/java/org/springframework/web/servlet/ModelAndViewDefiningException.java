package org.springframework.web.servlet;

import javax.servlet.ServletException;

import org.springframework.util.Assert;

/**
 * 应该转发到特定模型的特定视图的错误条件抛出的异常.
 *
 * <p>可以在处理器处理期间随时抛出.
 * 这包括预构建控制器的任何模板方法.
 * 例如, 如果某些参数不允许继续正常的工作流, 则表单控制器可能会中止到特定的错误页面.
 */
@SuppressWarnings("serial")
public class ModelAndViewDefiningException extends ServletException {

	private ModelAndView modelAndView;


	/**
	 * 使用给定的ModelAndView, 通常表示特定的错误页面.
	 * 
	 * @param modelAndView 带有要转发到的视图和要公开的模型的ModelAndView
	 */
	public ModelAndViewDefiningException(ModelAndView modelAndView) {
		Assert.notNull(modelAndView, "ModelAndView must not be null in ModelAndViewDefiningException");
		this.modelAndView = modelAndView;
	}

	/**
	 * 返回此异常包含的ModelAndView以进行转发.
	 */
	public ModelAndView getModelAndView() {
		return modelAndView;
	}

}
