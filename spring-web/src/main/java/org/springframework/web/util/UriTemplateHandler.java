package org.springframework.web.util;

import java.net.URI;
import java.util.Map;

/**
 * 扩展URI模板的策略, 完全控制URI模板语法和变量编码.
 * 也是预处理所有URI模板的便捷中心点, 例如插入公共基本路径.
 *
 * <p>作为{@code RestTemplate}上的属性以及{@code AsyncRestTemplate}支持.
 * {@link DefaultUriTemplateHandler}是通过{@link UriComponentsBuilder}基于Spring的URI模板支持构建的.
 * 可以使用替代实现来插入外部URI模板库.
 */
public interface UriTemplateHandler {

	/**
	 * 从URI变量的Map扩展给定的URI模板.
	 * 
	 * @param uriTemplate URI模板字符串
	 * @param uriVariables URI变量
	 * 
	 * @return 结果URI
	 */
	URI expand(String uriTemplate, Map<String, ?> uriVariables);

	/**
	 * 从URI变量数组中扩展给定的URI模板.
	 * 
	 * @param uriTemplate URI模板字符串
	 * @param uriVariables URI变量值
	 * 
	 * @return 结果URI
	 */
	URI expand(String uriTemplate, Object... uriVariables);

}
