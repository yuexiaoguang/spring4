package org.springframework.web.portlet.multipart;

import javax.portlet.ActionRequest;

import org.springframework.web.multipart.MultipartException;

/**
 * Spring的multipart解析策略的Portlet版本,
 * 用于<a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>中定义的文件上传.
 *
 * <p>实现通常可在任何应用程序上下文中使用, 也可以单独使用.
 *
 * <p>Spring中包含一个具体的实现:
 * <ul>
 * <li>用于Apache Commons FileUpload的{@link org.springframework.web.multipart.commons.CommonsMultipartResolver}
 * </ul>
 *
 * <p>没有用于Spring {@link org.springframework.web.portlet.DispatcherPortlet DispatcherPortlets}
 * 的默认解析器实现, 因为应用程序可能会选择解析其multipart请求本身.
 * 要定义实现, 请在{@code DispatcherPortlet}的应用程序上下文中创建一个id为"portletMultipartResolver"的bean.
 * 这样的解析器应用于{@code DispatcherPortlet}处理的所有请求.
 *
 * <p>如果{@code DispatcherPortlet}检测到multipart请求, 它将通过配置的
 * {@link org.springframework.web.portlet.multipart.PortletMultipartResolver}
 * 解析它并传递一个包装好的Portlet {@link ActionRequest}.
 * 然后, 控制器可以将其给定的请求转发到{@link MultipartActionRequest}接口, 以便能够访问{@code MultipartFiles}.
 * 请注意, 仅在实际的multipart请求的情况下才支持此强制转换.
 *
 * <pre class="code"> public void handleActionRequest(ActionRequest request, ActionResponse response) {
 *   MultipartActionRequest multipartRequest = (MultipartActionRequest) request;
 *   MultipartFile multipartFile = multipartRequest.getFile("image");
 *   ...
 * }</pre>
 *
 * 而不是直接访问, 命令或表单控制器可以将
 * {@link org.springframework.web.multipart.support.ByteArrayMultipartFileEditor}
 * 或{@link org.springframework.web.multipart.support.StringMultipartFileEditor}
 * 与其数据绑定器一起注册, 以自动应用multipart内容到表单bean属性.
 *
 * <p>Note: 几乎不需要从应用程序代码访问{@code MultipartResolver}本身.
 * 它将在幕后完成其工作, 使{@code MultipartActionRequests}可供控制器使用.
 */
public interface PortletMultipartResolver {

	/**
	 * 确定给定的请求是否包含multipart内容.
	 * <p>通常会检查内容类型"{@code multipart/form-data}", 但实际接受的请求可能取决于解析器实现的功能.
	 * 
	 * @param request 要评估的portlet请求
	 * 
	 * @return 请求是否包含multipart内容
	 */
	boolean isMultipart(ActionRequest request);

	/**
	 * 将给定的portlet请求解析为multipart文件和参数, 并将请求包装在MultipartActionRequest对象中,
	 * 该对象提供对文件描述符的访问, 并通过标准PortletRequest方法访问包含的参数.
	 * 
	 * @param request 要包装的portlet请求 (必须是multipart内容类型)
	 * 
	 * @return 包装的portlet请求
	 * @throws org.springframework.web.multipart.MultipartException
	 * 如果portlet请求不是multipart, 或者遇到特定于实现的问题 (例如超出文件大小限制)
	 */
	MultipartActionRequest resolveMultipart(ActionRequest request) throws MultipartException;

	/**
	 * 清除用于multipart处理的所有资源, 例如任何上传文件的存储.
	 * 
	 * @param request 要清理资源的请求
	 */
	void cleanupMultipart(MultipartActionRequest request);

}
