package org.springframework.web.bind;

import javax.servlet.ServletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.util.WebUtils;

/**
 * 从ServletRequest中的参数创建的PropertyValues实现.
 * 可以查找以特定前缀和前缀分隔符 (默认 "_")开头的所有属性值.
 *
 * <p>例如, 使用前缀"spring", "spring_param1"和"spring_param2"会产生一个带有"param1"和"param2"作为键的Map.
 *
 * <p>此类不是不可变的, 无法有效地删除应该忽略绑定的属性值.
 */
@SuppressWarnings("serial")
public class ServletRequestParameterPropertyValues extends MutablePropertyValues {

	/** 默认前缀分隔符 */
	public static final String DEFAULT_PREFIX_SEPARATOR = "_";


	/**
	 * 不使用前缀 (因此, 没有前缀分隔符).
	 * 
	 * @param request HTTP请求
	 */
	public ServletRequestParameterPropertyValues(ServletRequest request) {
		this(request, null, null);
	}

	/**
	 * 使用给定前缀和默认前缀分隔符 (下划线字符 "_").
	 * 
	 * @param request HTTP请求
	 * @param prefix 参数的前缀 (完整前缀将由它加上分隔符组成)
	 */
	public ServletRequestParameterPropertyValues(ServletRequest request, String prefix) {
		this(request, prefix, DEFAULT_PREFIX_SEPARATOR);
	}

	/**
	 * @param request HTTP请求
	 * @param prefix 参数的前缀 (完整前缀将由它加上分隔符组成)
	 * @param prefixSeparator 分隔符分隔前缀 (e.g. "spring")和参数名称的其余部分("param1", "param2")
	 */
	public ServletRequestParameterPropertyValues(ServletRequest request, String prefix, String prefixSeparator) {
		super(WebUtils.getParametersStartingWith(
				request, (prefix != null ? prefix + prefixSeparator : null)));
	}

}
