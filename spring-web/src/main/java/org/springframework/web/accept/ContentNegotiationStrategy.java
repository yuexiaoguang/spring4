package org.springframework.web.accept;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于解析请求的请求媒体类型的策略.
 */
public interface ContentNegotiationStrategy {

	/**
	 * 将给定请求解析为媒体类型列表.
	 * 返回的列表首先按特异性排序, 然后按质量参数排序.
	 * 
	 * @param webRequest 当前的请求
	 * 
	 * @return 请求的媒体类型或空列表 (never {@code null})
	 * @throws HttpMediaTypeNotAcceptableException 如果无法解析所请求的媒体类型
	 */
	List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
			throws HttpMediaTypeNotAcceptableException;

}
