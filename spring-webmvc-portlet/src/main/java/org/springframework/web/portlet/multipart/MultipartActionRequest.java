package org.springframework.web.portlet.multipart;

import javax.portlet.ActionRequest;

import org.springframework.web.multipart.MultipartRequest;

/**
 * 提供了在portlet请求中处理multipart内容的其他方法, 允许访问上传的文件.
 * 实现还需要覆盖参数访问的标准ActionRequest方法, 使multipart参数可用.
 *
 * <p>具体的实现是{@link DefaultMultipartActionRequest}.
 */
public interface MultipartActionRequest extends ActionRequest, MultipartRequest {

}
