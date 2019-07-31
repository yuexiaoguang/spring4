package org.springframework.web.multipart.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * 访问多部分请求的一部分的{@link ServerHttpRequest}实现.
 * 如果使用{@link MultipartResolver}配置, 则可通过{@link MultipartFile}访问该part.
 * 或者, 如果使用Servlet 3.0 multipart处理, 则可以通过{@code ServletRequest.getPart}访问该part.
 */
public class RequestPartServletServerHttpRequest extends ServletServerHttpRequest {

	private final MultipartHttpServletRequest multipartRequest;

	private final String partName;

	private final HttpHeaders headers;


	/**
	 * @param request 当前的servlet请求
	 * @param partName 要适配{@link ServerHttpRequest}约定的part名称
	 * 
	 * @throws MissingServletRequestPartException 如果无法找到请求part
	 * @throws MultipartException 如果无法初始化MultipartHttpServletRequest
	 */
	public RequestPartServletServerHttpRequest(HttpServletRequest request, String partName)
			throws MissingServletRequestPartException {

		super(request);

		this.multipartRequest = MultipartResolutionDelegate.asMultipartHttpServletRequest(request);
		this.partName = partName;

		this.headers = this.multipartRequest.getMultipartHeaders(this.partName);
		if (this.headers == null) {
			throw new MissingServletRequestPartException(partName);
		}
	}


	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}


	@Override
	public InputStream getBody() throws IOException {
		if (this.multipartRequest instanceof StandardMultipartHttpServletRequest) {
			try {
				return this.multipartRequest.getPart(this.partName).getInputStream();
			}
			catch (Exception ex) {
				throw new MultipartException("Could not parse multipart servlet request", ex);
			}
		}
		else {
			MultipartFile file = this.multipartRequest.getFile(this.partName);
			if (file != null) {
				return file.getInputStream();
			}
			else {
				String paramValue = this.multipartRequest.getParameter(this.partName);
				return new ByteArrayInputStream(paramValue.getBytes(determineEncoding()));
			}
		}
	}

	private String determineEncoding() {
		MediaType contentType = getHeaders().getContentType();
		if (contentType != null) {
			Charset charset = contentType.getCharset();
			if (charset != null) {
				return charset.name();
			}
		}
		String encoding = this.multipartRequest.getCharacterEncoding();
		return (encoding != null ? encoding : FORM_CHARSET);
	}

}
