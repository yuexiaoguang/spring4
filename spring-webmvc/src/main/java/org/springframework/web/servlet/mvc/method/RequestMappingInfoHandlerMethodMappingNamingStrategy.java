package org.springframework.web.servlet.mvc.method;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy;

/**
 * {@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
 * HandlerMethodMappingNamingStrategy}用于基于{@code RequestMappingInfo}的处理器方法映射.
 *
 * 如果设置了{@code RequestMappingInfo} name属性, 则使用其值.
 * 否则, 名称基于类名的大写字母, 后跟 "#"作为分隔符, 和方法名称.
 * 例如, "TC#getFoo"用于名为 TestController 的类以及方法 getFoo.
 */
public class RequestMappingInfoHandlerMethodMappingNamingStrategy
		implements HandlerMethodMappingNamingStrategy<RequestMappingInfo> {

	/** HandlerMethod映射名称的类型和方法级别部分之间的分隔符 */
	public static final String SEPARATOR = "#";


	@Override
	public String getName(HandlerMethod handlerMethod, RequestMappingInfo mapping) {
		if (mapping.getName() != null) {
			return mapping.getName();
		}
		StringBuilder sb = new StringBuilder();
		String simpleTypeName = handlerMethod.getBeanType().getSimpleName();
		for (int i = 0 ; i < simpleTypeName.length(); i++) {
			if (Character.isUpperCase(simpleTypeName.charAt(i))) {
				sb.append(simpleTypeName.charAt(i));
			}
		}
		sb.append(SEPARATOR).append(handlerMethod.getMethod().getName());
		return sb.toString();
	}

}
