package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;

/**
 * 支持last-modified HTTP请求以方便内容缓存.
 * 与Servlet API的{@code getLastModified}方法相同的约定.
 *
 * <p>由{@link org.springframework.web.servlet.HandlerAdapter#getLastModified}实现委托.
 * Spring默认框架中的任何Controller或HttpRequestHandler都可以实现此接口以启用last-modified的检查.
 *
 * <p><b>Note:</b> 替代处理器实现方法具有不同的last-modified处理样式.
 * 例如, Spring 2.5的带注解的控制器方法 (使用{@code @RequestMapping})
 * 通过{@link org.springframework.web.context.request.WebRequest#checkNotModified}方法提供last-modified的支持,
 * 允许在主处理器方法中检查last-modified.
 */
public interface LastModified {

	/**
	 * 与HttpServlet的{@code getLastModified}方法相同的约定.
	 * 在请求处理<b>之前</b>调用.
	 * <p>返回值将作为Last-Modified header发送到HTTP客户端, 并与客户端发回的If-Modified-Since header进行比较.
	 * 只有在进行修改后才会重新生成内容.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 上次修改底层资源的时间, 或-1表示必须始终重新生成内容
	 */
	long getLastModified(HttpServletRequest request);

}
