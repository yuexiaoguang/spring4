package org.springframework.web.servlet.mvc.multiaction;

import javax.servlet.http.HttpServletRequest;

/**
 * 使用<b>策略</b> GoF设计模式参数化MultiActionController类的接口,
 * 允许在不影响其他应用程序代码的情况下改变从传入请求到处理器方法名称的映射.
 *
 * <p>说明委托如何比子类化更灵活.
 *
 * @deprecated 从4.3开始, 使用注解驱动的处理器方法
 */
@Deprecated
public interface MethodNameResolver {

	/**
	 * Return a method name that can handle this request. Such
	 * mappings are typically, but not necessarily, based on URL.
	 * @param request current HTTP request
	 * @return a method name that can handle this request.
	 * Never returns {@code null}; throws exception if not resolvable.
	 * @throws NoSuchRequestHandlingMethodException if no handler method
	 * can be found for the given request
	 */
	String getHandlerMethodName(HttpServletRequest request) throws NoSuchRequestHandlingMethodException;

}
