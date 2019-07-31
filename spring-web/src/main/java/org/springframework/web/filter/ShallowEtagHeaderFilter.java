package org.springframework.web.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

/**
 * 根据响应内容生成{@code ETag}值的{@link javax.servlet.Filter}.
 * 将此ETag与请求的{@code If-None-Match} header进行比较.
 * 如果这些header相同, 则不会发送响应内容, 而是发送{@code 304 "Not Modified"}状态.
 *
 * <p>由于ETag基于响应内容, 因此仍会渲染响应 (e.g. {@link org.springframework.web.servlet.View}).
 * 因此, 此过滤器仅节省带宽, 而不是服务器性能.
 */
public class ShallowEtagHeaderFilter extends OncePerRequestFilter {

	private static final String HEADER_ETAG = "ETag";

	private static final String HEADER_IF_NONE_MATCH = "If-None-Match";

	private static final String HEADER_CACHE_CONTROL = "Cache-Control";

	private static final String DIRECTIVE_NO_STORE = "no-store";

	private static final String STREAMING_ATTRIBUTE = ShallowEtagHeaderFilter.class.getName() + ".STREAMING";


	/** 检查Servlet 3.0+ HttpServletResponse.getHeader(String) */
	private static final boolean servlet3Present =
			ClassUtils.hasMethod(HttpServletResponse.class, "getHeader", String.class);

	private boolean writeWeakETag = false;


	/**
	 * 根据RFC 7232, 设置写入响应的ETag值是否应该较弱.
	 * <p>应使用{@code <init-param>}为{@code web.xml}中的过滤器定义中的参数名称"writeWeakETag"进行配置.
	 */
	public void setWriteWeakETag(boolean writeWeakETag) {
		this.writeWeakETag = writeWeakETag;
	}

	/**
	 * 根据RFC 7232, 返回写入响应的ETag值是否应该较弱.
	 */
	public boolean isWriteWeakETag() {
		return this.writeWeakETag;
	}


	/**
	 * 默认值为{@code false}, 以便过滤器可以延迟生成ETag, 直到最后一个异步调度的线程.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		HttpServletResponse responseToUse = response;
		if (!isAsyncDispatch(request) && !(response instanceof ContentCachingResponseWrapper)) {
			responseToUse = new HttpStreamingAwareContentCachingResponseWrapper(response, request);
		}

		filterChain.doFilter(request, responseToUse);

		if (!isAsyncStarted(request) && !isContentCachingDisabled(request)) {
			updateResponse(request, responseToUse);
		}
	}

	private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ContentCachingResponseWrapper responseWrapper =
				WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
		Assert.notNull(responseWrapper, "ContentCachingResponseWrapper not found");
		HttpServletResponse rawResponse = (HttpServletResponse) responseWrapper.getResponse();
		int statusCode = responseWrapper.getStatusCode();

		if (rawResponse.isCommitted()) {
			responseWrapper.copyBodyToResponse();
		}
		else if (isEligibleForEtag(request, responseWrapper, statusCode, responseWrapper.getContentInputStream())) {
			String responseETag = generateETagHeaderValue(responseWrapper.getContentInputStream(), this.writeWeakETag);
			rawResponse.setHeader(HEADER_ETAG, responseETag);
			String requestETag = request.getHeader(HEADER_IF_NONE_MATCH);
			if (requestETag != null && ("*".equals(requestETag) || responseETag.equals(requestETag) ||
					responseETag.replaceFirst("^W/", "").equals(requestETag.replaceFirst("^W/", "")))) {
				if (logger.isTraceEnabled()) {
					logger.trace("ETag [" + responseETag + "] equal to If-None-Match, sending 304");
				}
				rawResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("ETag [" + responseETag + "] not equal to If-None-Match [" + requestETag +
							"], sending normal response");
				}
				responseWrapper.copyBodyToResponse();
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Response with status code [" + statusCode + "] not eligible for ETag");
			}
			responseWrapper.copyBodyToResponse();
		}
	}

	/**
	 * 指示给定的请求和响应是否符合ETag生成的条件.
	 * <p>如果所有条件都匹配, 则默认实现返回{@code true}:
	 * <ul>
	 * <li>{@code 2xx}系列中的响应状态码</li>
	 * <li>请求方法是 GET</li>
	 * <li>响应Cache-Control header未设置或不包含"no-store"指令</li>
	 * </ul>
	 * 
	 * @param request HTTP请求
	 * @param response HTTP响应
	 * @param responseStatusCode HTTP响应状态码
	 * @param inputStream 响应正文
	 * 
	 * @return {@code true} 如果符合ETag生成条件, 否则{@code false}
	 */
	protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
			int responseStatusCode, InputStream inputStream) {

		String method = request.getMethod();
		if (responseStatusCode >= 200 && responseStatusCode < 300 && HttpMethod.GET.matches(method)) {
			String cacheControl = null;
			if (servlet3Present) {
				cacheControl = response.getHeader(HEADER_CACHE_CONTROL);
			}
			if (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 从给定的响应主体字节数组生成ETag header值.
	 * <p>默认实现生成MD5哈希.
	 * 
	 * @param inputStream 响应正文
	 * @param isWeak 生成的ETag是否应该弱
	 * 
	 * @return ETag header值
	 */
	protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
		// length of W/ + " + 0 + 32bits md5 hash + "
		StringBuilder builder = new StringBuilder(37);
		if (isWeak) {
			builder.append("W/");
		}
		builder.append("\"0");
		DigestUtils.appendMd5DigestAsHex(inputStream, builder);
		builder.append('"');
		return builder.toString();
	}


	/**
	 * 此方法可用于禁用ShallowEtagHeaderFilter的内容缓存响应包装器.
	 * 这可以在HTTP流式传输开始之前完成, 例如, 响应将被异步写入, 而不是在Servlet容器线程的上下文中.
	 */
	public static void disableContentCaching(ServletRequest request) {
		Assert.notNull(request, "ServletRequest must not be null");
		request.setAttribute(STREAMING_ATTRIBUTE, true);
	}

	private static boolean isContentCachingDisabled(HttpServletRequest request) {
		return (request.getAttribute(STREAMING_ATTRIBUTE) != null);
	}


	private static class HttpStreamingAwareContentCachingResponseWrapper extends ContentCachingResponseWrapper {

		private final HttpServletRequest request;

		public HttpStreamingAwareContentCachingResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
			super(response);
			this.request = request;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return (useRawResponse() ? getResponse().getOutputStream() : super.getOutputStream());
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return (useRawResponse() ? getResponse().getWriter() : super.getWriter());
		}

		private boolean useRawResponse() {
			return isContentCachingDisabled(this.request);
		}
	}

}
