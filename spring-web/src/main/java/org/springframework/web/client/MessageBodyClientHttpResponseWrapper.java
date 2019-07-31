package org.springframework.web.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link ClientHttpResponse}的实现, 它不仅可以通过实际读取输入流,
 * 来检查响应是否具有消息主体, 还可以检查其长度是否为0 (i.e. 为空).
 */
class MessageBodyClientHttpResponseWrapper implements ClientHttpResponse {

	private final ClientHttpResponse response;

	private PushbackInputStream pushbackInputStream;


	public MessageBodyClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
		this.response = response;
	}


	/**
	 * 指示响应是否具有消息正文.
	 * <p>实现返回{@code false}, 如果:
	 * <ul>
	 * <li>{@code 1XX}, {@code 204}或{@code 304}响应状态</li>
	 * <li>{@code Content-Length} header为{@code 0}</li>
	 * </ul>
	 * 
	 * @return {@code true}如果响应有消息主体, 否则{@code false}
	 * @throws IOException
	 */
	public boolean hasMessageBody() throws IOException {
		try {
			HttpStatus status = getStatusCode();
			if (status != null && status.is1xxInformational() || status == HttpStatus.NO_CONTENT ||
					status == HttpStatus.NOT_MODIFIED) {
				return false;
			}
		}
		catch (IllegalArgumentException ex) {
			// Ignore - unknown HTTP status code...
		}
		if (getHeaders().getContentLength() == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 指示响应是否具有空消息正文.
	 * <p>实现尝试读取响应流的第一个字节:
	 * <ul>
	 * <li>如果没有可用的字节, 则消息正文为空</li>
	 * <li>否则它不为空, 并且流被重置为其开始位置, 以供进一步读取</li>
	 * </ul>
	 * 
	 * @return {@code true} 如果响应具有零长度消息主体, 否则{@code false}
	 * @throws IOException
	 */
	public boolean hasEmptyMessageBody() throws IOException {
		InputStream body = this.response.getBody();
		if (body == null) {
			return true;
		}
		else if (body.markSupported()) {
			body.mark(1);
			if (body.read() == -1) {
				return true;
			}
			else {
				body.reset();
				return false;
			}
		}
		else {
			this.pushbackInputStream = new PushbackInputStream(body);
			int b = this.pushbackInputStream.read();
			if (b == -1) {
				return true;
			}
			else {
				this.pushbackInputStream.unread(b);
				return false;
			}
		}
	}


	@Override
	public HttpHeaders getHeaders() {
		return this.response.getHeaders();
	}

	@Override
	public InputStream getBody() throws IOException {
		return (this.pushbackInputStream != null ? this.pushbackInputStream : this.response.getBody());
	}

	@Override
	public HttpStatus getStatusCode() throws IOException {
		return this.response.getStatusCode();
	}

	@Override
	public int getRawStatusCode() throws IOException {
		return this.response.getRawStatusCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.response.getStatusText();
	}

	@Override
	public void close() {
		this.response.close();
	}

}
