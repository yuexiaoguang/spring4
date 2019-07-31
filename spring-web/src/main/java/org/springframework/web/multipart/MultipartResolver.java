package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

/**
 * 根据<a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>进行multipart文件上传解析的策略接口.
 * 实现通常可以在应用程序上下文中使用, 也可以单独使用.
 *
 * <p>从Spring 3.1开始, Spring中包含两个具体实现:
 * <ul>
 * <li>用于Apache Commons FileUpload的{@link org.springframework.web.multipart.commons.CommonsMultipartResolver}
 * <li>用于Servlet 3.0+ Part API的{@link org.springframework.web.multipart.support.StandardServletMultipartResolver}
 * </ul>
 *
 * <p>没有用于Spring {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlets}的默认解析器实现,
 * 因为应用程序可能会选择解析其multipart请求本身.
 * 要定义实现, 在{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}的
 * 应用程序上下文中创建一个id为"multipartResolver"的bean.
 * 这样的解析器应用于{@link org.springframework.web.servlet.DispatcherServlet}处理的所有请求.
 *
 * <p>如果{@link org.springframework.web.servlet.DispatcherServlet}检测到multipart请求,
 * 它将通过配置的{@link MultipartResolver}解析它并传递一个包装的{@link javax.servlet.http.HttpServletRequest}.
 * 然后, 控制器可以将其给定的请求转发到{@link MultipartHttpServletRequest}接口,
 * 该接口允许访问任何{@link MultipartFile MultipartFiles}.
 * 请注意, 仅在实际的multipart请求的情况下才支持此强制转换.
 *
 * <pre class="code">
 * public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
 *   MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
 *   MultipartFile multipartFile = multipartRequest.getFile("image");
 *   ...
 * }</pre>
 *
 * 不直接访问, 命令或表单控制器可以将
 * {@link org.springframework.web.multipart.support.ByteArrayMultipartFileEditor}
 * 或{@link org.springframework.web.multipart.support.StringMultipartFileEditor}
 * 与其数据绑定器一起注册, 以自动应用multipart内容到表单bean属性.
 *
 * <p>作为将{@link MultipartResolver}
 * 与{@link org.springframework.web.servlet.DispatcherServlet}一起使用的替代方法,
 * 可以在{@code web.xml}中注册{@link org.springframework.web.multipart.support.MultipartFilter}.
 * 它将委托给根应用程序上下文中的相应{@link MultipartResolver} bean.
 * 这主要适用于不使用Spring自己的Web MVC框架的应用程序.
 *
 * <p>Note: 几乎不需要从应用程序代码访问{@link MultipartResolver}本身.
 * 它将在幕后完成其工作,
 * 使{@link MultipartHttpServletRequest MultipartHttpServletRequests}可用于控制器.
 */
public interface MultipartResolver {

	/**
	 * 确定给定的请求是否包含multipart内容.
	 * <p>通常会检查内容类型"multipart/form-data", 但实际接受的请求可能取决于解析器实现的功能.
	 * 
	 * @param request 要评估的servlet请求
	 * 
	 * @return 请求是否包含multipart内容
	 */
	boolean isMultipart(HttpServletRequest request);

	/**
	 * 将给定的HTTP请求解析为multipart文件和参数,
	 * 并将请求包装在
	 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}对象中,
	 * 该对象提供对文件描述符的访问, 并使包含的参数可通过标准ServletRequest方法访问.
	 * 
	 * @param request 要包装的servlet请求 (必须是multipart内容类型)
	 * 
	 * @return 包装的servlet请求
	 * @throws MultipartException 如果servlet请求不是multipart, 或者遇到特定于实现的问题 (例如超出文件大小限制)
	 */
	MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException;

	/**
	 * 清除用于multipart处理的所有资源, 例如上载文件的存储.
	 * 
	 * @param request 用于清理资源的请求
	 */
	void cleanupMultipart(MultipartHttpServletRequest request);

}
