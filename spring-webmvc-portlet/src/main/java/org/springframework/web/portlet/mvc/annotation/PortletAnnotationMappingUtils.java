package org.springframework.web.portlet.mvc.annotation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.portlet.ClientDataRequest;
import javax.portlet.PortletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * 用于基于注解的请求映射的Helper类.
 */
abstract class PortletAnnotationMappingUtils {

	/**
	 * 将给定的{@code String}数组合并为一个, 每个元素只包含一个.
	 * <p>保留原始数组中元素的顺序 (重叠元素除外, 它们仅在第一次出现时包含).
	 * 
	 * @param array1 第一个数组 (can be {@code null})
	 * @param array2 第二个数组 (can be {@code null})
	 * 
	 * @return 新数组 ({@code null}, 如果给定的数组都是{@code null})
	 */
	public static String[] mergeStringArrays(String[] array1, String[] array2) {
		if (ObjectUtils.isEmpty(array1)) {
			return array2;
		}
		if (ObjectUtils.isEmpty(array2)) {
			return array1;
		}

		Set<String> result = new LinkedHashSet<String>();
		result.addAll(Arrays.asList(array1));
		result.addAll(Arrays.asList(array2));
		return StringUtils.toStringArray(result);
	}

	/**
	 * 检查给定的portlet模式是否与指定的类型级别模式匹配.
	 * 
	 * @param modes 要检查的映射的portlet模式
	 * @param typeLevelModes 要检查的类型级模式映射
	 */
	public static boolean validateModeMapping(String[] modes, String[] typeLevelModes) {
		if (!ObjectUtils.isEmpty(modes) && !ObjectUtils.isEmpty(typeLevelModes)) {
			for (String mode : modes) {
				boolean match = false;
				for (String typeLevelMode : typeLevelModes) {
					if (mode.equalsIgnoreCase(typeLevelMode)) {
						match = true;
					}
				}
				if (!match) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 检查给定的请求是否与指定的请求方法匹配.
	 * 
	 * @param methods 要检查的请求方法
	 * @param request 当前要检查的请求
	 */
	public static boolean checkRequestMethod(RequestMethod[] methods, PortletRequest request) {
		if (methods.length == 0) {
			return true;
		}
		if (!(request instanceof ClientDataRequest)) {
			return false;
		}
		String method = ((ClientDataRequest) request).getMethod();
		for (RequestMethod candidate : methods) {
			if (method.equals(candidate.name())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的请求是否与指定的请求方法匹配.
	 * 
	 * @param methods 要检查的请求方法
	 * @param request 当前要检查的请求
	 */
	public static boolean checkRequestMethod(Set<String> methods, PortletRequest request) {
		if (!methods.isEmpty()) {
			if (!(request instanceof ClientDataRequest)) {
				return false;
			}
			String method = ((ClientDataRequest) request).getMethod();
			if (!methods.contains(method)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 检查给定的请求是否与指定的参数条件匹配.
	 * 
	 * @param params 参数条件, {@link org.springframework.web.bind.annotation.RequestMapping#params()}之后
	 * @param request 当前要检查的请求
	 */
	public static boolean checkParameters(String[] params, PortletRequest request) {
		if (!ObjectUtils.isEmpty(params)) {
			for (String param : params) {
				int separator = param.indexOf('=');
				if (separator == -1) {
					if (param.startsWith("!")) {
						if (PortletUtils.hasSubmitParameter(request, param.substring(1))) {
							return false;
						}
					}
					else if (!PortletUtils.hasSubmitParameter(request, param)) {
						return false;
					}
				}
				else {
					String key = param.substring(0, separator);
					String value = param.substring(separator + 1);
					if (!value.equals(request.getParameter(key))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * 检查给定的请求是否与指定的header条件匹配.
	 * 
	 * @param headers header条件, {@link RequestMapping#headers()}之后
	 * @param request 要检查的当前HTTP请求
	 */
	public static boolean checkHeaders(String[] headers, PortletRequest request) {
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				int separator = header.indexOf('=');
				if (separator == -1) {
					if (header.startsWith("!")) {
						if (request.getProperty(header.substring(1)) != null) {
							return false;
						}
					}
					else if (request.getProperty(header) == null) {
						return false;
					}
				}
				else {
					String key = header.substring(0, separator);
					String value = header.substring(separator + 1);
					if (isMediaTypeHeader(key)) {
						List<MediaType> requestMediaTypes = MediaType.parseMediaTypes(request.getProperty(key));
						List<MediaType> valueMediaTypes = MediaType.parseMediaTypes(value);
						boolean found = false;
						for (Iterator<MediaType> valIter = valueMediaTypes.iterator(); valIter.hasNext() && !found;) {
							MediaType valueMediaType = valIter.next();
							for (Iterator<MediaType> reqIter = requestMediaTypes.iterator(); reqIter.hasNext() && !found;) {
								MediaType requestMediaType = reqIter.next();
								if (valueMediaType.includes(requestMediaType)) {
									found = true;
								}
							}

						}
						if (!found) {
							return false;
						}
					}
					else if (!value.equals(request.getProperty(key))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static boolean isMediaTypeHeader(String headerName) {
		return "Accept".equalsIgnoreCase(headerName) || "Content-Type".equalsIgnoreCase(headerName);
	}

}
