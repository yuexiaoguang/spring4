package org.springframework.web.bind.annotation;

/**
 * HTTP请求方法的Java 5枚举.
 * 用于{@link RequestMapping}注解的{@link RequestMapping#method()}属性.
 *
 * <p>请注意, 默认情况下, {@link org.springframework.web.servlet.DispatcherServlet}
 * 仅支持 GET, HEAD, POST, PUT, PATCH 和 DELETE.
 * DispatcherServlet将使用默认的HttpServlet行为处理TRACE和OPTIONS,
 * 除非明确告知它们也分派这些请求类型:
 * 查看"dispatchOptionsRequest"和"dispatchTraceRequest"属性, 必要时将它们切换为"true".
 */
public enum RequestMethod {

	GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE

}
