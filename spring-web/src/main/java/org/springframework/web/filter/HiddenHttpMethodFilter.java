package org.springframework.web.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link javax.servlet.Filter}将发布的方法参数转换为HTTP方法, 可通过{@link HttpServletRequest#getMethod()}检索.
 * 由于浏览器目前仅支持GET和POST, 因此Prototype库使用的常用技术是,
 * 使用普通POST和其他隐藏表单字段({@code _method})来传递"真正的" HTTP方法.
 * 此过滤器读取该参数并相应地更改{@link HttpServletRequestWrapper#getMethod()}返回值.
 * 只允许{@code "PUT"}, {@code "DELETE"}和{@code "PATCH"} HTTP方法.
 *
 * <p>请求参数的名称默认为{@code _method}, 但可以通过{@link #setMethodParam(String) methodParam}属性进行调整.
 *
 * <p><b>NOTE: 由于需要检查POST正文参数, 因此在multipart POST请求的情况下, 此过滤器需要在multipart处理之后运行.</b>
 * 通常, 在{@code web.xml}过滤器链中的这个HiddenHttpMethodFilter之前,
 * 放一个Spring {@link org.springframework.web.multipart.support.MultipartFilter}.
 */
public class HiddenHttpMethodFilter extends OncePerRequestFilter {

	private static final List<String> ALLOWED_METHODS =
			Collections.unmodifiableList(Arrays.asList(HttpMethod.PUT.name(),
					HttpMethod.DELETE.name(), HttpMethod.PATCH.name()));

	/** 默认方法参数: {@code _method} */
	public static final String DEFAULT_METHOD_PARAM = "_method";

	private String methodParam = DEFAULT_METHOD_PARAM;


	/**
	 * 设置查找HTTP方法的参数名称.
	 */
	public void setMethodParam(String methodParam) {
		Assert.hasText(methodParam, "'methodParam' must not be empty");
		this.methodParam = methodParam;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		HttpServletRequest requestToUse = request;

		if ("POST".equals(request.getMethod()) && request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
			String paramValue = request.getParameter(this.methodParam);
			if (StringUtils.hasLength(paramValue)) {
				String method = paramValue.toUpperCase(Locale.ENGLISH);
				if (ALLOWED_METHODS.contains(method)) {
					requestToUse = new HttpMethodRequestWrapper(request, method);
				}
			}
		}

		filterChain.doFilter(requestToUse, response);
	}


	/**
	 * 简单的{@link HttpServletRequest}包装器, 它返回{@link HttpServletRequest#getMethod()}提供的方法.
	 */
	private static class HttpMethodRequestWrapper extends HttpServletRequestWrapper {

		private final String method;

		public HttpMethodRequestWrapper(HttpServletRequest request, String method) {
			super(request);
			this.method = method;
		}

		@Override
		public String getMethod() {
			return this.method;
		}
	}

}
