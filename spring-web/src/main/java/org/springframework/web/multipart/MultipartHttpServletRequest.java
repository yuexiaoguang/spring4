package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * 提供用于处理servlet请求中的multipart内容的其他方法, 允许访问上传的文件.
 * 实现还需要覆盖参数访问的标准{@link javax.servlet.ServletRequest}方法, 使multipart参数可用.
 *
 * <p>具体的实现是
 * {@link org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest}.
 * 作为中间步骤, 可以对
 * {@link org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest}
 * 进行子类化.
 */
public interface MultipartHttpServletRequest extends HttpServletRequest, MultipartRequest {

	/**
	 * 返回此请求的方法.
	 */
	HttpMethod getRequestMethod();

	/**
	 * 返回此请求的header.
	 */
	HttpHeaders getRequestHeaders();

	/**
	 * 返回与multipart请求的指定部分关联的header.
	 * <p>如果底层实现支持对header的访问, 则返回所有header.
	 * 否则, 返回的header至少会包含一个'Content-Type' header.
	 */
	HttpHeaders getMultipartHeaders(String paramOrFileName);

}
