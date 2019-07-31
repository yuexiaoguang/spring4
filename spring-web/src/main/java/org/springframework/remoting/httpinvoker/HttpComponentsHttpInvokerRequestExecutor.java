package org.springframework.remoting.httpinvoker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.remoting.httpinvoker.HttpInvokerRequestExecutor}实现,
 * 使用<a href="http://hc.apache.org/httpcomponents-client-ga/httpclient/">Apache HttpComponents HttpClient</a>
 * 执行POST请求.
 *
 * <p>允许使用预先配置的{@link org.apache.http.client.HttpClient}实例, 可能使用身份验证, HTTP连接池等.
 * 还为易于子类化而设计, 提供特定的模板方法.
 *
 * <p>从Spring 4.1开始, 此请求执行器需要Apache HttpComponents 4.3或更高版本.
 */
public class HttpComponentsHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {

	private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 100;

	private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 5;

	private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (60 * 1000);


	private static Class<?> abstractHttpClientClass;

	static {
		try {
			// 查找AbstractHttpClient类 (deprecated as of HttpComponents 4.3)
			abstractHttpClientClass = ClassUtils.forName("org.apache.http.impl.client.AbstractHttpClient",
					HttpComponentsHttpInvokerRequestExecutor.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// Probably removed from HttpComponents in the meantime...
		}
	}


	private HttpClient httpClient;

	private RequestConfig requestConfig;


	/**
	 * 使用默认的{@link HttpClient}, 它使用默认的{@code org.apache.http.impl.conn.PoolingClientConnectionManager}.
	 */
	public HttpComponentsHttpInvokerRequestExecutor() {
		this(createDefaultHttpClient(), RequestConfig.custom()
				.setSocketTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS).build());
	}

	/**
	 * @param httpClient 用于此请求执行器的HttpClient实例
	 */
	public HttpComponentsHttpInvokerRequestExecutor(HttpClient httpClient) {
		this(httpClient, null);
	}

	private HttpComponentsHttpInvokerRequestExecutor(HttpClient httpClient, RequestConfig requestConfig) {
		this.httpClient = httpClient;
		this.requestConfig = requestConfig;
	}


	private static HttpClient createDefaultHttpClient() {
		Registry<ConnectionSocketFactory> schemeRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", SSLConnectionSocketFactory.getSocketFactory())
				.build();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(schemeRegistry);
		connectionManager.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
		connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);

		return HttpClientBuilder.create().setConnectionManager(connectionManager).build();
	}


	/**
	 * 设置用于此请求执行器的{@link HttpClient}实例.
	 */
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * 返回此请求执行器使用的{@link HttpClient}实例.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * 设置底层HttpClient的连接超时.
	 * 值0指定无限超时.
	 * <p>可以通过在自定义{@link HttpClient}上指定{@link RequestConfig}实例来配置其他属性.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = cloneRequestConfig().setConnectTimeout(timeout).build();
		setLegacyConnectionTimeout(getHttpClient(), timeout);
	}

	/**
	 * 将指定的连接超时应用于已弃用的{@link HttpClient}实现.
	 * <p>从HttpClient 4.3开始, 默认参数必须通过{@link RequestConfig}实例公开, 而不是在客户端上设置参数.
	 * 不幸的是, 这种行为不向后兼容, 旧的{@link HttpClient}实现将忽略上下文中设置的{@link RequestConfig}对象.
	 * <p>如果指定的客户端是较旧的实现, 通过弃用的API设置自定义连接超时.
	 * 否则, 只是通过{@link RequestConfig}与较新的客户端一起返回.
	 * 
	 * @param client 要配置的客户端
	 * @param timeout 自定义连接超时
	 */
	@SuppressWarnings("deprecation")
	private void setLegacyConnectionTimeout(HttpClient client, int timeout) {
		if (abstractHttpClientClass != null && abstractHttpClientClass.isInstance(client)) {
			client.getParams().setIntParameter(org.apache.http.params.CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
		}
	}

	/**
	 * 设置使用底层HttpClient从连接管理器请求连接时使用的超时, 以毫秒为单位.
	 * 值0指定无限超时.
	 * <p>可以通过在自定义{@link HttpClient}上指定{@link RequestConfig}实例来配置其他属性.
	 * 
	 * @param connectionRequestTimeout 请求连接的超时值, 以毫秒为单位
	 */
	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.requestConfig = cloneRequestConfig().setConnectionRequestTimeout(connectionRequestTimeout).build();
	}

	/**
	 * 设置底层HttpClient的套接字读取超时.
	 * 值0指定无限超时.
	 * <p>可以通过在自定义{@link HttpClient}上指定{@link RequestConfig}实例来配置其他属性.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = cloneRequestConfig().setSocketTimeout(timeout).build();
		setLegacySocketTimeout(getHttpClient(), timeout);
	}

	/**
	 * 将指定的套接字超时应用于已弃用的{@link HttpClient}实现.
	 * See {@link #setLegacyConnectionTimeout}.
	 * 
	 * @param client 要配置的客户端
	 * @param timeout 自定义套接字超时
	 */
	@SuppressWarnings("deprecation")
	private void setLegacySocketTimeout(HttpClient client, int timeout) {
		if (abstractHttpClientClass != null && abstractHttpClientClass.isInstance(client)) {
			client.getParams().setIntParameter(org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT, timeout);
		}
	}

	private RequestConfig.Builder cloneRequestConfig() {
		return (this.requestConfig != null ? RequestConfig.copy(this.requestConfig) : RequestConfig.custom());
	}


	/**
	 * 通过HttpClient执行给定的请求.
	 * <p>此方法实现基本处理工作流程: 实际工作发生在这个类的模板方法中.
	 */
	@Override
	protected RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException, ClassNotFoundException {

		HttpPost postMethod = createHttpPost(config);
		setRequestBody(config, postMethod, baos);
		try {
			HttpResponse response = executeHttpPost(config, getHttpClient(), postMethod);
			validateResponse(config, response);
			InputStream responseBody = getResponseBody(config, response);
			return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
		}
		finally {
			postMethod.releaseConnection();
		}
	}

	/**
	 * 为给定配置创建HttpPost.
	 * <p>默认实现创建一个标准的HttpPost, 将"Content-Type" header设置为"application/x-java-serialized-object".
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * 
	 * @return HttpPost实例
	 * @throws java.io.IOException
	 */
	protected HttpPost createHttpPost(HttpInvokerClientConfiguration config) throws IOException {
		HttpPost httpPost = new HttpPost(config.getServiceUrl());

		RequestConfig requestConfig = createRequestConfig(config);
		if (requestConfig != null) {
			httpPost.setConfig(requestConfig);
		}

		LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null) {
				httpPost.addHeader(HTTP_HEADER_ACCEPT_LANGUAGE, StringUtils.toLanguageTag(locale));
			}
		}

		if (isAcceptGzipEncoding()) {
			httpPost.addHeader(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}

		return httpPost;
	}

	/**
	 * 为给定配置创建{@link RequestConfig}.
	 * 可以返回{@code null}以指示不应设置自定义请求配置, 并且应使用{@link HttpClient}的默认值.
	 * <p>默认实现尝试将客户端的默认值与实例的本地自定义项合并.
	 * 
	 * @param config 指定目标服务的 HTTP调用器配置
	 * 
	 * @return 要使用的RequestConfig
	 */
	protected RequestConfig createRequestConfig(HttpInvokerClientConfiguration config) {
		HttpClient client = getHttpClient();
		if (client instanceof Configurable) {
			RequestConfig clientRequestConfig = ((Configurable) client).getConfig();
			return mergeRequestConfig(clientRequestConfig);
		}
		return this.requestConfig;
	}

	private RequestConfig mergeRequestConfig(RequestConfig defaultRequestConfig) {
		if (this.requestConfig == null) {  // nothing to merge
			return defaultRequestConfig;
		}

		RequestConfig.Builder builder = RequestConfig.copy(defaultRequestConfig);
		int connectTimeout = this.requestConfig.getConnectTimeout();
		if (connectTimeout >= 0) {
			builder.setConnectTimeout(connectTimeout);
		}
		int connectionRequestTimeout = this.requestConfig.getConnectionRequestTimeout();
		if (connectionRequestTimeout >= 0) {
			builder.setConnectionRequestTimeout(connectionRequestTimeout);
		}
		int socketTimeout = this.requestConfig.getSocketTimeout();
		if (socketTimeout >= 0) {
			builder.setSocketTimeout(socketTimeout);
		}
		return builder.build();
	}

	/**
	 * 将给定的序列化远程调用设置为请求正文.
	 * <p>默认实现只是将序列化调用设置为HttpPost的请求主体.
	 * 例如, 可以覆盖此选项以写入特定编码, 或者可能设置适当的HTTP请求header.
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * @param httpPost 要设置请求主体的HttpPost
	 * @param baos 包含序列化RemoteInvocation对象的ByteArrayOutputStream
	 * 
	 * @throws java.io.IOException
	 */
	protected void setRequestBody(
			HttpInvokerClientConfiguration config, HttpPost httpPost, ByteArrayOutputStream baos)
			throws IOException {

		ByteArrayEntity entity = new ByteArrayEntity(baos.toByteArray());
		entity.setContentType(getContentType());
		httpPost.setEntity(entity);
	}

	/**
	 * 执行给定的HttpPost实例.
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * @param httpClient 要执行的HttpClient
	 * @param httpPost 要执行的HttpPost
	 * 
	 * @return 结果HttpResponse
	 * @throws java.io.IOException
	 */
	protected HttpResponse executeHttpPost(
			HttpInvokerClientConfiguration config, HttpClient httpClient, HttpPost httpPost)
			throws IOException {

		return httpClient.execute(httpPost);
	}

	/**
	 * 验证HttpPost对象中包含的给定响应, 如果它与成功的HTTP响应不对应, 则抛出异常.
	 * <p>默认实现拒绝任何超过2xx的HTTP状态代码, 以避免解析响应正文, 并尝试从损坏的流中反序列化.
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * @param response 要验证的结果HttpResponse
	 * 
	 * @throws java.io.IOException 如果验证失败
	 */
	protected void validateResponse(HttpInvokerClientConfiguration config, HttpResponse response)
			throws IOException {

		StatusLine status = response.getStatusLine();
		if (status.getStatusCode() >= 300) {
			throw new NoHttpResponseException(
					"Did not receive successful HTTP response: status code = " + status.getStatusCode() +
					", status message = [" + status.getReasonPhrase() + "]");
		}
	}

	/**
	 * 从给定的执行远程调用请求中提取响应主体.
	 * <p>默认实现只是获取HttpPost的响应主体流.
	 * 如果响应被识别为GZIP响应, 则InputStream将被包装在GZIPInputStream中.
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * @param httpResponse 从中读取响应主体的结果HttpResponse
	 * 
	 * @return 响应主体的InputStream
	 * @throws java.io.IOException
	 */
	protected InputStream getResponseBody(HttpInvokerClientConfiguration config, HttpResponse httpResponse)
			throws IOException {

		if (isGzipResponse(httpResponse)) {
			return new GZIPInputStream(httpResponse.getEntity().getContent());
		}
		else {
			return httpResponse.getEntity().getContent();
		}
	}

	/**
	 * 确定给定的响应是否表示GZIP响应.
	 * <p>默认实现检查HTTP "Content-Encoding" header是否包含"gzip".
	 * 
	 * @param httpResponse 要检查的结果HttpResponse
	 * 
	 * @return 给定的响应是否表示GZIP响应
	 */
	protected boolean isGzipResponse(HttpResponse httpResponse) {
		Header encodingHeader = httpResponse.getFirstHeader(HTTP_HEADER_CONTENT_ENCODING);
		return (encodingHeader != null && encodingHeader.getValue() != null &&
				encodingHeader.getValue().toLowerCase().contains(ENCODING_GZIP));
	}
}
