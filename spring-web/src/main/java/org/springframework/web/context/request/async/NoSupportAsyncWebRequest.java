package org.springframework.web.context.request.async;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.ServletWebRequest;

/**
 * 在没有底层异步支持时使用的{@code AsyncWebRequest}.
 */
public class NoSupportAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest {

	public NoSupportAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}


	@Override
	public void addCompletionHandler(Runnable runnable) {
		// ignored
	}

	@Override
	public void setTimeout(Long timeout) {
		// ignored
	}

	@Override
	public void addTimeoutHandler(Runnable runnable) {
		// ignored
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}


	// Not supported

	@Override
	public void startAsync() {
		throw new UnsupportedOperationException("No async support in a pre-Servlet 3.0 runtime");
	}

	@Override
	public boolean isAsyncComplete() {
		throw new UnsupportedOperationException("No async support in a pre-Servlet 3.0 runtime");
	}

	@Override
	public void dispatch() {
		throw new UnsupportedOperationException("No async support in a pre-Servlet 3.0 runtime");
	}

}
