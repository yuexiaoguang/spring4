package org.springframework.web.cors;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A strategy that takes a request and a {@link CorsConfiguration} and updates
 * the response.
 *
 * <p>This component is not concerned with how a {@code CorsConfiguration} is
 * selected but rather takes follow-up actions such as applying CORS validation
 * checks and either rejecting the response or adding CORS headers to the
 * response.
 */
public interface CorsProcessor {

	/**
	 * Process a request given a {@code CorsConfiguration}.
	 * @param configuration the applicable CORS configuration (possibly {@code null})
	 * @param request the current request
	 * @param response the current response
	 * @return {@code false} if the request is rejected, {@code true} otherwise
	 */
	boolean processRequest(CorsConfiguration configuration, HttpServletRequest request,
			HttpServletResponse response) throws IOException;

}
