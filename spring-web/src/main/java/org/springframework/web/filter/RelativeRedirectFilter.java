package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * 覆盖{@link HttpServletResponse#sendRedirect(String)}并通过设置HTTP状态和"Location" header来处理它,
 * 这使Servlet容器不会将相对重定向URL重写为绝对URL.
 * Servlet容器需要这样做, 但违反了建议
 * <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2"> RFC 7231 Section 7.1.2</a>,
 * 此外, 不一定要考虑"X-Forwarded" header.
 *
 * <p><strong>Note:</strong> 虽然RFC中建议使用相对重定向, 但在某些使用反向代理的配置下, 它们可能无法正常工作.
 */
public class RelativeRedirectFilter extends OncePerRequestFilter {

	private HttpStatus redirectStatus = HttpStatus.SEE_OTHER;


	/**
	 * 设置用于重定向的默认HTTP状态.
	 * <p>默认{@link HttpStatus#SEE_OTHER}.
	 * 
	 * @param status 要使用的3xx重定向状态
	 */
	public void setRedirectStatus(HttpStatus status) {
		Assert.notNull(status, "Property 'redirectStatus' is required");
		Assert.isTrue(status.is3xxRedirection(), "Not a redirect status code");
		this.redirectStatus = status;
	}

	/**
	 * 返回配置的重定向状态.
	 */
	public HttpStatus getRedirectStatus() {
		return this.redirectStatus;
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		response = RelativeRedirectResponseWrapper.wrapIfNecessary(response, this.redirectStatus);
		filterChain.doFilter(request, response);
	}

}
