package org.springframework.web.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * Portlet MVC framework SPI interface, allowing parameterization of core MVC workflow.
 *
 * <p>Interface that must be implemented for each handler type to handle a request.
 * This interface is used to allow the DispatcherPortlet to be indefinitely
 * extensible. The DispatcherPortlet accesses all installed handlers through this
 * interface, meaning that it does not contain code specific to any handler type.
 *
 * <p>Note that a handler can be of type Object. This is to enable handlers from
 * other frameworks to be integrated with this framework without custom coding.
 *
 * <p>This interface is not intended for application developers. It is available
 * to handlers who want to develop their own web workflow.
 *
 * <p>Note: Implementations can implement the Ordered interface to be able to
 * specify a sorting order and thus a priority for getting applied by
 * DispatcherPortlet. Non-Ordered instances get treated as lowest priority.
 */
public interface HandlerAdapter {

	/**
	 * Given a handler instance, return whether or not this HandlerAdapter can
	 * support it. Typical HandlerAdapters will base the decision on the handler
	 * type. HandlerAdapters will usually only support one handler type each.
	 * <p>A typical implementation:
	 * <p>{@code
	 * return (handler instanceof MyHandler);
	 * }
	 * @param handler handler object to check
	 * @return whether or not this object can use the given handler
	 */
	boolean supports(Object handler);

	/**
	 * Use the given handler to handle this action request.
	 * The workflow that is required may vary widely.
	 * @param request current action request
	 * @param response current action response
	 * @param handler handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned true.
	 * @throws Exception in case of errors
	 */
	void handleAction(ActionRequest request, ActionResponse response, Object handler) throws Exception;

	/**
	 * Use the given handler to handle this render request.
	 * The workflow that is required may vary widely.
	 * @param request current render request
	 * @param response current render response
	 * @param handler handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned {@code true}.
	 * @throws Exception in case of errors
	 * @return ModelAndView object with the name of the view and the required
	 * model data, or {@code null} if the request has been handled directly
	 */
	ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler) throws Exception;

	/**
	 * Use the given handler to handle this resource request.
	 * The workflow that is required may vary widely.
	 * @param request current render request
	 * @param response current render response
	 * @param handler handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned {@code true}.
	 * @throws Exception in case of errors
	 * @return ModelAndView object with the name of the view and the required
	 * model data, or {@code null} if the request has been handled directly
	 */
	ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler) throws Exception;

	/**
	 * Use the given handler to handle this event request.
	 * The workflow that is required may vary widely.
	 * @param request current action request
	 * @param response current action response
	 * @param handler handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned true.
	 * @throws Exception in case of errors
	 */
	void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception;

}
