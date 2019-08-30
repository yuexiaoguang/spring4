package org.springframework.web.portlet.bind;

import javax.portlet.PortletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * 从PortletRequest中的参数创建的PropertyValues实现.
 * 可以查找以特定前缀和前缀分隔符开头的所有属性值 (默认为 "_").
 *
 * <p>例如, 使用前缀"spring", "spring_param1"和"spring_param2"会产生一个带有"param1"和"param2"作为键的Map.
 *
 * <p>此类不是不可变的, 无法有效地删除应该忽略绑定的属性值.
 */
@SuppressWarnings("serial")
public class PortletRequestParameterPropertyValues extends MutablePropertyValues {

	/** 默认前缀分隔符 */
	public static final String DEFAULT_PREFIX_SEPARATOR = "_";


	/**
	 * 不使用前缀 (因此, 没有前缀分隔符).
	 * 
	 * @param request portlet请求
	 */
	public PortletRequestParameterPropertyValues(PortletRequest request) {
		this(request, null, null);
	}

	/**
	 * 使用给定前缀和默认前缀分隔符 (下划线字符"_").
	 * 
	 * @param request portlet请求
	 * @param prefix 参数的前缀 (完整前缀将由此加上分隔符组成)
	 */
	public PortletRequestParameterPropertyValues(PortletRequest request, String prefix) {
		this(request, prefix, DEFAULT_PREFIX_SEPARATOR);
	}

	/**
	 * @param request portlet请求
	 * @param prefix 参数的前缀 (完整前缀将由此加上分隔符组成)
	 * @param prefixSeparator 前缀的分隔符 (e.g. "spring") 和参数名称的其余部分 ("param1", "param2")
	 */
	public PortletRequestParameterPropertyValues(PortletRequest request, String prefix, String prefixSeparator) {
		super(PortletUtils.getParametersStartingWith(
				request, (prefix != null ? prefix + prefixSeparator : null)));
	}

}
