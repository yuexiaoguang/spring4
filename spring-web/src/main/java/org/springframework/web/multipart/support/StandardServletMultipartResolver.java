package org.springframework.web.multipart.support;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.logging.LogFactory;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * {@link MultipartResolver}接口的标准实现, 基于Servlet 3.0 {@link javax.servlet.http.Part} API.
 * 要作为"multipartResolver" bean添加到Spring DispatcherServlet上下文中, 而不在bean级别进行任何额外配置 (see below).
 *
 * <p><b>Note:</b> 为了使用基于Servlet 3.0的multipart解析,
 * 需要在{@code web.xml}中使用"multipart-config"部分标记受影响的servlet,
 * 或者在编程servlet注册中使用{@link javax.servlet.MultipartConfigElement}标记受影响的servlet,
 * 或者(在自定义servlet类的情况下)可能在servlet类上有{@link javax.servlet.annotation.MultipartConfig}注解.
 * 需要在该servlet注册级别应用配置设置, 例如最大大小或存储位置; Servlet 3.0不允许在MultipartResolver级别设置它们.
 */
public class StandardServletMultipartResolver implements MultipartResolver {

	private boolean resolveLazily = false;


	/**
	 * 设置是否在文件或参数访问时延迟解析multipart请求.
	 * <p>默认为"false", 立即解析multipart元素, 在{@link #resolveMultipart}调用时抛出相应的异常.
	 * 对于延迟multipart解析, 将其切换为"true", 一旦应用程序尝试获取multipart文件或参数, 则抛出解析异常.
	 */
	public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}


	@Override
	public boolean isMultipart(HttpServletRequest request) {
		// Same check as in Commons FileUpload...
		if (!"post".equalsIgnoreCase(request.getMethod())) {
			return false;
		}
		String contentType = request.getContentType();
		return StringUtils.startsWithIgnoreCase(contentType, "multipart/");
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		return new StandardMultipartHttpServletRequest(request, this.resolveLazily);
	}

	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (!(request instanceof AbstractMultipartHttpServletRequest) ||
				((AbstractMultipartHttpServletRequest) request).isResolved()) {
			// 为了安全起见: 明确删除 part, 但仅删除实际文件部分 (用于Resin兼容性)
			try {
				for (Part part : request.getParts()) {
					if (request.getFile(part.getName()) != null) {
						part.delete();
					}
				}
			}
			catch (Throwable ex) {
				LogFactory.getLog(getClass()).warn("Failed to perform cleanup of multipart items", ex);
			}
		}
	}

}
