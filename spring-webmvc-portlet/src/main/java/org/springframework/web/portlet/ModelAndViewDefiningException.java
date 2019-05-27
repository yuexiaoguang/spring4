package org.springframework.web.portlet;

import javax.portlet.PortletException;

/**
 * Exception to be thrown on error conditions that should forward
 * to a specific view with a specific model.
 *
 * <p>Can be thrown at any time during handler processing.
 * This includes any template methods of pre-built controllers.
 * For example, a form controller might abort to a specific error page
 * if certain parameters do not allow to proceed with the normal workflow.
 */
@SuppressWarnings("serial")
public class ModelAndViewDefiningException extends PortletException {

	private ModelAndView modelAndView;


	/**
	 * Create new ModelAndViewDefiningException with the given ModelAndView,
	 * typically representing a specific error page.
	 * @param modelAndView ModelAndView with view to forward to and model to expose
	 */
	public ModelAndViewDefiningException(ModelAndView modelAndView) {
		if (modelAndView == null) {
			throw new IllegalArgumentException("modelAndView must not be null in ModelAndViewDefiningException");
		}
		this.modelAndView = modelAndView;
	}

	/**
	 * Return the ModelAndView that this exception contains for forwarding to.
	 */
	public ModelAndView getModelAndView() {
		return modelAndView;
	}

}
