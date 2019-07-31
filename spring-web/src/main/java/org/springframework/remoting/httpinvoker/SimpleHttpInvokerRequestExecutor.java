package org.springframework.remoting.httpinvoker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.remoting.httpinvoker.HttpInvokerRequestExecutor}实现,
 * 使用标准Java工具执行POST请求, 不支持HTTP身份验证或高级配置选项.
 *
 * <p>专为轻松子类化而设计, 可自定义特定模板方法.
 * 但是, 请考虑{@code HttpComponentsHttpInvokerRequestExecutor}以获得更复杂的需求:
 * 标准{@link HttpURLConnection}类的功能相当有限.
 */
public class SimpleHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {

	private int connectTimeout = -1;

	private int readTimeout = -1;


	/**
	 * 设置底层URLConnection的连接超时 (以毫秒为单位).
	 * 值0指定无限超时.
	 * <p>默认值是系统的默认超时.
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 设置底层URLConnection的读取超时 (以毫秒为单位).
	 * 值0指定无限超时.
	 * <p>默认值是系统的默认超时.
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}


	/**
	 * 通过标准{@link HttpURLConnection}执行给定的请求.
	 * <p>此方法实现基本处理工作流程:
	 * 实际工作发生在这个类的模板方法中.
	 */
	@Override
	protected RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException, ClassNotFoundException {

		HttpURLConnection con = openConnection(config);
		prepareConnection(con, baos.size());
		writeRequestBody(config, con, baos);
		validateResponse(config, con);
		InputStream responseBody = readResponseBody(config, con);

		return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
	}

	/**
	 * 为给定的远程调用请求打开{@link HttpURLConnection}.
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * 
	 * @return 给定请求的HttpURLConnection
	 * @throws IOException
	 */
	protected HttpURLConnection openConnection(HttpInvokerClientConfiguration config) throws IOException {
		URLConnection con = new URL(config.getServiceUrl()).openConnection();
		if (!(con instanceof HttpURLConnection)) {
			throw new IOException(
					"Service URL [" + config.getServiceUrl() + "] does not resolve to an HTTP connection");
		}
		return (HttpURLConnection) con;
	}

	/**
	 * 准备给定的HTTP连接.
	 * <p>默认实现将POST指定为方法, 将"Content-Type" header指定为"application/x-java-serialized-object",
	 * 将给定内容长度指定为"Content-Length" header.
	 * 
	 * @param connection 要准备的HTTP连接
	 * @param contentLength 要发送的内容的长度
	 * 
	 * @throws IOException 如果被HttpURLConnection方法抛出
	 */
	protected void prepareConnection(HttpURLConnection connection, int contentLength) throws IOException {
		if (this.connectTimeout >= 0) {
			connection.setConnectTimeout(this.connectTimeout);
		}
		if (this.readTimeout >= 0) {
			connection.setReadTimeout(this.readTimeout);
		}

		connection.setDoOutput(true);
		connection.setRequestMethod(HTTP_METHOD_POST);
		connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, getContentType());
		connection.setRequestProperty(HTTP_HEADER_CONTENT_LENGTH, Integer.toString(contentLength));

		LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null) {
				connection.setRequestProperty(HTTP_HEADER_ACCEPT_LANGUAGE, StringUtils.toLanguageTag(locale));
			}
		}

		if (isAcceptGzipEncoding()) {
			connection.setRequestProperty(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}
	}

	/**
	 * 设置给定的序列化远程调用为请求正文.
	 * <p>默认实现只是将序列化调用写入HttpURLConnection的OutputStream.
	 * 例如, 可以覆盖此选项以写入特定编码, 并可能设置适当的HTTP请求 header.
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * @param con 将请求正文写入的HttpURLConnection
	 * @param baos 包含序列化RemoteInvocation对象的ByteArrayOutputStream
	 * 
	 * @throws IOException
	 */
	protected void writeRequestBody(
			HttpInvokerClientConfiguration config, HttpURLConnection con, ByteArrayOutputStream baos)
			throws IOException {

		baos.writeTo(con.getOutputStream());
	}

	/**
	 * 验证{@link HttpURLConnection}对象中包含的给定响应,
	 * 如果它与成功的HTTP响应不对应, 则抛出异常.
	 * <p>默认实现拒绝任何超过2xx的HTTP状态代码, 以避免解析响应正文并尝试从损坏的流中反序列化.
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * @param con 要验证的HttpURLConnection
	 * 
	 * @throws IOException 如果验证失败
	 */
	protected void validateResponse(HttpInvokerClientConfiguration config, HttpURLConnection con)
			throws IOException {

		if (con.getResponseCode() >= 300) {
			throw new IOException(
					"Did not receive successful HTTP response: status code = " + con.getResponseCode() +
					", status message = [" + con.getResponseMessage() + "]");
		}
	}

	/**
	 * 从给定的执行远程调用请求中提取响应主体.
	 * <p>默认实现只是从HttpURLConnection的InputStream中读取序列化调用.
	 * 如果响应被识别为GZIP响应, 则InputStream将被包装在GZIPInputStream中.
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * @param con 从中读取响应主体的HttpURLConnection
	 * 
	 * @return 响应主体的InputStream
	 * @throws IOException
	 */
	protected InputStream readResponseBody(HttpInvokerClientConfiguration config, HttpURLConnection con)
			throws IOException {

		if (isGzipResponse(con)) {
			// GZIP response found - need to unzip.
			return new GZIPInputStream(con.getInputStream());
		}
		else {
			// Plain response found.
			return con.getInputStream();
		}
	}

	/**
	 * 确定给定的响应是否是GZIP响应.
	 * <p>默认实现检查 HTTP "Content-Encoding" header是否包含"gzip" (在任何情况下).
	 * 
	 * @param con 要检查的HttpURLConnection
	 */
	protected boolean isGzipResponse(HttpURLConnection con) {
		String encodingHeader = con.getHeaderField(HTTP_HEADER_CONTENT_ENCODING);
		return (encodingHeader != null && encodingHeader.toLowerCase().contains(ENCODING_GZIP));
	}
}
