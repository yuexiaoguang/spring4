package org.springframework.web.method.support;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 通过查看方法参数和参数值并决定应更新目标URL的哪个部分, 用于构建{@link UriComponents}的策略.
 */
public interface UriComponentsContributor {

	/**
	 * 此策略是否支持给定的方法参数.
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 处理给定的方法参数并更新{@link UriComponentsBuilder}, 或使用URI变量添加到Map, 以用于在处理完所有参数后展开URI.
	 * 
	 * @param parameter 控制器方法参数 (never {@code null})
	 * @param value 参数值 (possibly {@code null})
	 * @param builder 要更新的构建器 (never {@code null})
	 * @param uriVariables 要添加URI变量的Map (never {@code null})
	 * @param conversionService 将值格式化为字符串的ConversionService
	 */
	void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
			Map<String, Object> uriVariables, ConversionService conversionService);

}
