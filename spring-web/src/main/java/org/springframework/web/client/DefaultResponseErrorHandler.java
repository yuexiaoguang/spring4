package org.springframework.web.client;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;

/**
 * {@link ResponseErrorHandler}接口的Spring的默认实现.
 *
 * <p>此错误处理器检查{@link ClientHttpResponse}上的状态码:
 * 任何具有{@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR}
 * 或{@link org.springframework.http.HttpStatus.Series#SERVER_ERROR}系列的代码都被视为错误;
 * 可以通过覆盖{@link #hasError(HttpStatus)}方法来更改此行为.
 * {@link #hasError(ClientHttpResponse)}将忽略未知状态码.
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {

	/**
	 * 使用响应状态码委托给{@link #hasError(HttpStatus)}.
	 */
	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		int rawStatusCode = response.getRawStatusCode();
		for (HttpStatus statusCode : HttpStatus.values()) {
			if (statusCode.value() == rawStatusCode) {
				return hasError(statusCode);
			}
		}
		return false;
	}

	/**
	 * 从{@link #hasError(ClientHttpResponse)}调用的模板方法.
	 * <p>默认实现检查给定的状态码是否为
	 * {@link HttpStatus.Series#CLIENT_ERROR CLIENT_ERROR}
	 * 或{@link HttpStatus.Series#SERVER_ERROR SERVER_ERROR}.
	 * 可以在子类中重写.
	 * 
	 * @param statusCode HTTP状态码
	 * 
	 * @return {@code true} 如果响应有错误; 否则{@code false}
	 */
	protected boolean hasError(HttpStatus statusCode) {
		return (statusCode.series() == HttpStatus.Series.CLIENT_ERROR ||
				statusCode.series() == HttpStatus.Series.SERVER_ERROR);
	}

	/**
	 * 如果响应状态代码是{@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR},
	 * 则此默认实现抛出{@link HttpClientErrorException},
	 * 如果是{@link org.springframework.http.HttpStatus.Series#SERVER_ERROR},
	 * 则抛出{@link HttpServerErrorException},
	 * 在其他情况下抛出{@link RestClientException}.
	 */
	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		HttpStatus statusCode = getHttpStatusCode(response);
		switch (statusCode.series()) {
			case CLIENT_ERROR:
				throw new HttpClientErrorException(statusCode, response.getStatusText(),
						response.getHeaders(), getResponseBody(response), getCharset(response));
			case SERVER_ERROR:
				throw new HttpServerErrorException(statusCode, response.getStatusText(),
						response.getHeaders(), getResponseBody(response), getCharset(response));
			default:
				throw new UnknownHttpStatusCodeException(statusCode.value(), response.getStatusText(),
						response.getHeaders(), getResponseBody(response), getCharset(response));
		}
	}


	/**
	 * 确定给定响应的HTTP状态.
	 * <p>Note: 仅从{@link #handleError}调用, 而不是从{@link #hasError}调用.
	 * 
	 * @param response 要检查的响应
	 * 
	 * @return 关联的HTTP状态
	 * @throws IOException
	 * @throws UnknownHttpStatusCodeException 如果是未知状态码, 无法用{@link HttpStatus}枚举表示
	 */
	protected HttpStatus getHttpStatusCode(ClientHttpResponse response) throws IOException {
		try {
			return response.getStatusCode();
		}
		catch (IllegalArgumentException ex) {
			throw new UnknownHttpStatusCodeException(response.getRawStatusCode(), response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		}
	}

	/**
	 * 读取给定响应的主体 (包含在状态异常中).
	 * 
	 * @param response 要检查的响应
	 * 
	 * @return 响应主体, 如果无法读取主体, 则为空字节数组
	 */
	protected byte[] getResponseBody(ClientHttpResponse response) {
		try {
			return FileCopyUtils.copyToByteArray(response.getBody());
		}
		catch (IOException ex) {
			// ignore
		}
		return new byte[0];
	}

	/**
	 * 确定响应的字符集 (包含在状态异常中).
	 * 
	 * @param response 要检查的响应
	 * 
	 * @return 关联的字符集, 或{@code null}
	 */
	protected Charset getCharset(ClientHttpResponse response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return (contentType != null ? contentType.getCharset() : null);
	}
}
