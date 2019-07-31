package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * {@link javax.servlet.Filter}处理CORS preflight请求并拦截CORS简单和实际请求,
 * 这要归功于{@link CorsProcessor}实现 (默认为{@link DefaultCorsProcessor}),
 * 为了使用提供的{@link CorsConfigurationSource}添加相关的CORS响应header (如{@code Access-Control-Allow-Origin})
 * (例如 {@link UrlBasedCorsConfigurationSource}实例).
 *
 * <p>这是Spring MVC Java配置和XML命名空间CORS配置的替代方案, 对于仅依赖于spring-web (而不是不是spring-webmvc)的应用程序
 * 或者需要在{@link javax.servlet.Filter}级别执行CORS检查的安全约束非常有用.
 *
 * <p>此过滤器可与{@link DelegatingFilterProxy}结合使用, 以帮助其初始化.
 */
public class CorsFilter extends OncePerRequestFilter {

	private final CorsConfigurationSource configSource;

	private CorsProcessor processor = new DefaultCorsProcessor();


	/**
	 * 构造函数接受过滤器使用的{@link CorsConfigurationSource}来查找用于每个传入请求的{@link CorsConfiguration}.
	 */
	public CorsFilter(CorsConfigurationSource configSource) {
		Assert.notNull(configSource, "CorsConfigurationSource must not be null");
		this.configSource = configSource;
	}


	/**
	 * 配置自定义{@link CorsProcessor}以用于为请求应用匹配的{@link CorsConfiguration}.
	 * <p>默认使用{@link DefaultCorsProcessor}.
	 */
	public void setCorsProcessor(CorsProcessor processor) {
		Assert.notNull(processor, "CorsProcessor must not be null");
		this.processor = processor;
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		if (CorsUtils.isCorsRequest(request)) {
			CorsConfiguration corsConfiguration = this.configSource.getCorsConfiguration(request);
			if (corsConfiguration != null) {
				boolean isValid = this.processor.processRequest(corsConfiguration, request, response);
				if (!isValid || CorsUtils.isPreFlightRequest(request)) {
					return;
				}
			}
		}

		filterChain.doFilter(request, response);
	}

}
