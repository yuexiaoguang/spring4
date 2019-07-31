package org.springframework.web.filter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link javax.servlet.Filter}在HTTP PUT或PATCH请求期间
 * 通过{@code ServletRequest.getParameter*()}系列方法提供表单编码数据.
 *
 * <p>Servlet规范要求表单数据可用于HTTP POST, 但不能用于HTTP PUT或PATCH请求.
 * 此过滤器拦截内容类型为{@code 'application/x-www-form-urlencoded'}的HTTP PUT和PATCH请求,
 * 从请求主体读取表单编码内容, 并包装ServletRequest以使表单数据可用作请求参数, 就像它对HTTP POST请求一样.
 */
public class HttpPutFormContentFilter extends OncePerRequestFilter {

	private FormHttpMessageConverter formConverter = new AllEncompassingFormHttpMessageConverter();


	/**
	 * 设置用于解析表单内容的转换器.
	 * <p>默认{@link AllEncompassingFormHttpMessageConverter}实例.
	 */
	public void setFormConverter(FormHttpMessageConverter converter) {
		Assert.notNull(converter, "FormHttpMessageConverter is required.");
		this.formConverter = converter;
	}

	public FormHttpMessageConverter getFormConverter() {
		return this.formConverter;
	}

	/**
	 * 用于读取表单数据的默认字符集.
	 * 这是快捷方式:<br>
	 * {@code getFormConverter.setCharset(charset)}.
	 */
	public void setCharset(Charset charset) {
		this.formConverter.setCharset(charset);
	}


	@Override
	protected void doFilterInternal(final HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		if (("PUT".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) && isFormContentType(request)) {
			HttpInputMessage inputMessage = new ServletServerHttpRequest(request) {
				@Override
				public InputStream getBody() throws IOException {
					return request.getInputStream();
				}
			};
			MultiValueMap<String, String> formParameters = this.formConverter.read(null, inputMessage);
			if (!formParameters.isEmpty()) {
				HttpServletRequest wrapper = new HttpPutFormContentRequestWrapper(request, formParameters);
				filterChain.doFilter(wrapper, response);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private boolean isFormContentType(HttpServletRequest request) {
		String contentType = request.getContentType();
		if (contentType != null) {
			try {
				MediaType mediaType = MediaType.parseMediaType(contentType);
				return (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType));
			}
			catch (IllegalArgumentException ex) {
				return false;
			}
		}
		else {
			return false;
		}
	}


	private static class HttpPutFormContentRequestWrapper extends HttpServletRequestWrapper {

		private MultiValueMap<String, String> formParameters;

		public HttpPutFormContentRequestWrapper(HttpServletRequest request, MultiValueMap<String, String> parameters) {
			super(request);
			this.formParameters = (parameters != null ? parameters : new LinkedMultiValueMap<String, String>());
		}

		@Override
		public String getParameter(String name) {
			String queryStringValue = super.getParameter(name);
			String formValue = this.formParameters.getFirst(name);
			return (queryStringValue != null ? queryStringValue : formValue);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			Map<String, String[]> result = new LinkedHashMap<String, String[]>();
			Enumeration<String> names = getParameterNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				result.put(name, getParameterValues(name));
			}
			return result;
		}

		@Override
		public Enumeration<String> getParameterNames() {
			Set<String> names = new LinkedHashSet<String>();
			names.addAll(Collections.list(super.getParameterNames()));
			names.addAll(this.formParameters.keySet());
			return Collections.enumeration(names);
		}

		@Override
		public String[] getParameterValues(String name) {
			String[] parameterValues = super.getParameterValues(name);
			List<String> formParam = this.formParameters.get(name);
			if (formParam == null) {
				return parameterValues;
			}
			if (parameterValues == null || getQueryString() == null) {
				return StringUtils.toStringArray(formParam);
			}
			else {
				List<String> result = new ArrayList<String>(parameterValues.length + formParam.size());
				result.addAll(Arrays.asList(parameterValues));
				result.addAll(formParam);
				return StringUtils.toStringArray(result);
			}
		}
	}

}
