package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Overrides {@link HttpServletResponse#sendRedirect(String)} and handles it by
 * setting the HTTP status and "Location" headers, which keeps the Servlet
 * container from re-writing relative redirect URLs into absolute ones.
 * Servlet containers are required to do that but against the recommendation of
 * <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2"> RFC 7231 Section 7.1.2</a>,
 * and furthermore not necessarily taking into account "X-Forwarded" headers.
 *
 * <p><strong>Note:</strong> While relative redirects are recommended in the
 * RFC, under some configurations with reverse proxies they may not work.
 */
public class RelativeRedirectFilter extends OncePerRequestFilter {

	private HttpStatus redirectStatus = HttpStatus.SEE_OTHER;


	/**
	 * Set the default HTTP Status to use for redirects.
	 * <p>By default this is {@link HttpStatus#SEE_OTHER}.
	 * @param status the 3xx redirect status to use
	 */
	public void setRedirectStatus(HttpStatus status) {
		Assert.notNull(status, "Property 'redirectStatus' is required");
		Assert.isTrue(status.is3xxRedirection(), "Not a redirect status code");
		this.redirectStatus = status;
	}

	/**
	 * Return the configured redirect status.
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
