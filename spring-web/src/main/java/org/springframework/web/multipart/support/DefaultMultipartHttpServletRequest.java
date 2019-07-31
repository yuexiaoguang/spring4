package org.springframework.web.multipart.support;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}接口的默认实现.
 * 提供对预生成参数值的管理.
 *
 * <p>由{@link org.springframework.web.multipart.commons.CommonsMultipartResolver}使用.
 */
public class DefaultMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

	private static final String CONTENT_TYPE = "Content-Type";

	private Map<String, String[]> multipartParameters;

	private Map<String, String> multipartParameterContentTypes;


	/**
	 * @param request 要包装的servlet请求
	 * @param mpFiles multipart文件的映射
	 * @param mpParams 要公开的参数的Map
	 */
	public DefaultMultipartHttpServletRequest(HttpServletRequest request, MultiValueMap<String, MultipartFile> mpFiles,
			Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {

		super(request);
		setMultipartFiles(mpFiles);
		setMultipartParameters(mpParams);
		setMultipartParameterContentTypes(mpParamContentTypes);
	}

	/**
	 * @param request 要包装的servlet请求
	 */
	public DefaultMultipartHttpServletRequest(HttpServletRequest request) {
		super(request);
	}


	@Override
	public String getParameter(String name) {
		String[] values = getMultipartParameters().get(name);
		if (values != null) {
			return (values.length > 0 ? values[0] : null);
		}
		return super.getParameter(name);
	}

	@Override
	public String[] getParameterValues(String name) {
		String[] values = getMultipartParameters().get(name);
		if (values != null) {
			return values;
		}
		return super.getParameterValues(name);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		Map<String, String[]> multipartParameters = getMultipartParameters();
		if (multipartParameters.isEmpty()) {
			return super.getParameterNames();
		}

		Set<String> paramNames = new LinkedHashSet<String>();
		Enumeration<String> paramEnum = super.getParameterNames();
		while (paramEnum.hasMoreElements()) {
			paramNames.add(paramEnum.nextElement());
		}
		paramNames.addAll(multipartParameters.keySet());
		return Collections.enumeration(paramNames);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> multipartParameters = getMultipartParameters();
		if (multipartParameters.isEmpty()) {
			return super.getParameterMap();
		}

		Map<String, String[]> paramMap = new LinkedHashMap<String, String[]>();
		paramMap.putAll(super.getParameterMap());
		paramMap.putAll(multipartParameters);
		return paramMap;
	}

	@Override
	public String getMultipartContentType(String paramOrFileName) {
		MultipartFile file = getFile(paramOrFileName);
		if (file != null) {
			return file.getContentType();
		}
		else {
			return getMultipartParameterContentTypes().get(paramOrFileName);
		}
	}

	@Override
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		String contentType = getMultipartContentType(paramOrFileName);
		if (contentType != null) {
			HttpHeaders headers = new HttpHeaders();
			headers.add(CONTENT_TYPE, contentType);
			return headers;
		}
		else {
			return null;
		}
	}


	/**
	 * 将参数名称作为键, 将字符串数组对象作为值.
	 * 在初始化时由子类调用.
	 */
	protected final void setMultipartParameters(Map<String, String[]> multipartParameters) {
		this.multipartParameters = multipartParameters;
	}

	/**
	 * 获取用于检索的multipart参数Map, 如有必要, 可以延迟初始化它.
	 */
	protected Map<String, String[]> getMultipartParameters() {
		if (this.multipartParameters == null) {
			initializeMultipart();
		}
		return this.multipartParameters;
	}

	/**
	 * 将参数名称作为键, 将内容类型字符串作为值.
	 * 在初始化时由子类调用.
	 */
	protected final void setMultipartParameterContentTypes(Map<String, String> multipartParameterContentTypes) {
		this.multipartParameterContentTypes = multipartParameterContentTypes;
	}

	/**
	 * 获取用于检索的multipart参数内容类型Map, 如有必要, 可以延迟初始化它.
	 */
	protected Map<String, String> getMultipartParameterContentTypes() {
		if (this.multipartParameterContentTypes == null) {
			initializeMultipart();
		}
		return this.multipartParameterContentTypes;
	}

}
