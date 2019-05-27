package org.springframework.web.portlet;

import javax.portlet.PortletRequest;

/**
 * Interface to be implemented by objects that define a mapping between
 * requests and handler objects.
 *
 * <p>This class can be implemented by application developers, although this is not
 * necessary, as {@link org.springframework.web.portlet.handler.PortletModeHandlerMapping},
 * {@link org.springframework.web.portlet.handler.ParameterHandlerMapping} and
 * {@link org.springframework.web.portlet.handler.PortletModeParameterHandlerMapping}
 * are included in the framework. The first is the default if no HandlerMapping
 * bean is registered in the portlet application context.
 *
 * <p>HandlerMapping implementations can support mapped interceptors but do not
 * have to. A handler will always be wrapped in a {@link HandlerExecutionChain}
 * instance, optionally accompanied by some {@link HandlerInterceptor} instances.
 * The DispatcherPortlet will first call each HandlerInterceptor's
 * {@code preHandle} method in the given order, finally invoking the handler
 * itself if all {@code preHandle} methods have returned {@code true}.
 *
 * <p>The ability to parameterize this mapping is a powerful and unusual
 * capability of this Portlet MVC framework. For example, it is possible to
 * write a custom mapping based on session state, cookie state or many other
 * variables. No other MVC framework seems to be equally flexible.
 *
 * <p>Note: Implementations can implement the {@link org.springframework.core.Ordered}
 * interface to be able to specify a sorting order and thus a priority for getting
 * applied by DispatcherPortlet. Non-Ordered instances get treated as lowest priority.
 */
public interface HandlerMapping {

	/**
	 * Return a handler and any interceptors for this request. The choice may be made
	 * on portlet mode, session state, or any factor the implementing class chooses.
	 * <p>The returned HandlerExecutionChain contains a handler Object, rather than
	 * even a tag interface, so that handlers are not constrained in any way.
	 * For example, a HandlerAdapter could be written to allow another framework's
	 * handler objects to be used.
	 * <p>Returns {@code null} if no match was found. This is not an error.
	 * The DispatcherPortlet will query all registered HandlerMapping beans to find
	 * a match, and only decide there is an error if none can find a handler.
	 * @param request current portlet request
	 * @return a HandlerExecutionChain instance containing handler object and
	 * any interceptors, or null if no mapping found
	 * @throws Exception if there is an internal error
	 */
	HandlerExecutionChain getHandler(PortletRequest request) throws Exception;

}
