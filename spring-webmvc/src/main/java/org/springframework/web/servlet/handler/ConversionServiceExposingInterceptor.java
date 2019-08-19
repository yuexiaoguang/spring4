package org.springframework.web.servlet.handler;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;

/**
 * 拦截器, 将已配置的{@link ConversionService}置于请求范围内, 以便在请求处理期间可用.
 * 请求属性名称是"org.springframework.core.convert.ConversionService", 
 * {@code ConversionService.class.getName()}的值.
 *
 * <p>主要用于JSP标签, 例如 spring:eval 标签.
 */
public class ConversionServiceExposingInterceptor extends HandlerInterceptorAdapter {

	private final ConversionService conversionService;


	/**
	 * @param conversionService 调用此拦截器时要转换为请求范围的转换服务
	 */
	public ConversionServiceExposingInterceptor(ConversionService conversionService) {
		Assert.notNull(conversionService, "The ConversionService may not be null");
		this.conversionService = conversionService;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {

		request.setAttribute(ConversionService.class.getName(), this.conversionService);
		return true;
	}

}
