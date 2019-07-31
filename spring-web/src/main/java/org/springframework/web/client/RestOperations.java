package org.springframework.web.client;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

/**
 * 指定一组基本的RESTful操作的接口.
 * 由{@link RestTemplate}实现. 不经常直接使用, 但是增强可测试性的有用选项, 因为它很容易被模拟或存根.
 */
public interface RestOperations {

	// GET

	/**
	 * 通过对指定的URL执行GET来检索表示.
	 * 转换并返回响应.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 转换后的对象
	 */
	<T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 通过在URI模板上执行GET来检索表示.
	 * 转换并返回响应.
	 * <p>使用给定的Map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 包含URI模板变量的Map
	 * 
	 * @return 转换后的对象
	 */
	<T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过对URL执行GET来检索表示.
	 * 转换并返回响应.
	 * 
	 * @param url the URL
	 * @param responseType 返回值的类型
	 * 
	 * @return 转换后的对象
	 */
	<T> T getForObject(URI url, Class<T> responseType) throws RestClientException;

	/**
	 * 通过对指定的URL执行GET来检索实体.
	 * 响应被转换并存储在{@link ResponseEntity}中.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return the entity
	 */
	<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 通过在URI模板上执行GET来检索表示.
	 * 响应被转换并存储在{@link ResponseEntity}中.
	 * <p>使用给定的Map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 包含URI模板变量的Map
	 * 
	 * @return 转换后的对象
	 */
	<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过对URL执行GET来检索表示.
	 * 响应被转换并存储在{@link ResponseEntity}中.
	 * 
	 * @param url the URL
	 * @param responseType 返回值的类型
	 * 
	 * @return 转换后的对象
	 */
	<T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException;


	// HEAD

	/**
	 * 检索URI模板指定的资源的所有header.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 该资源的所有HTTP header
	 */
	HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 检索URI模板指定的资源的所有header.
	 * <p>使用给定的Map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param uriVariables 包含URI模板变量的Map
	 * 
	 * @return 该资源的所有HTTP header
	 */
	HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 检索URL指定的资源的所有header.
	 * 
	 * @param url the URL
	 * 
	 * @return 该资源的所有HTTP header
	 */
	HttpHeaders headForHeaders(URI url) throws RestClientException;


	// POST

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并返回{@code Location} header的值.
	 * 此header通常指示新资源的存储位置.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * <p>The {@code request} parameter can be a {@link HttpEntity} in order to add additional HTTP headers to the request.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return the value for the {@code Location} header
	 */
	URI postForLocation(String url, Object request, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并返回{@code Location} header的值.
	 * 此header通常指示新资源的存储位置.
	 * <p>使用给定的Map扩展URI模板变量.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return {@code Location} header的值
	 */
	URI postForLocation(String url, Object request, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并返回{@code Location} header的值.
	 * 此header通常指示新资源的存储位置.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * 
	 * @return {@code Location} header的值
	 */
	URI postForLocation(URI url, Object request) throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并返回在响应中找到的表示.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 转换后的对象
	 */
	<T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并返回在响应中找到的表示.
	 * <p>使用给定的Map扩展URI模板变量.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 转换后的对象
	 */
	<T> T postForObject(String url, Object request, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象POST到URL来创建新资源, 并返回在响应中找到的表示.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param responseType 返回值的类型
	 * 
	 * @return 转换后的对象
	 */
	<T> T postForObject(URI url, Object request, Class<T> responseType) throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并将响应返回为{@link ResponseEntity}.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 转换后的对象
	 */
	<T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并将响应返回为{@link HttpEntity}.
	 * <p>使用给定的Map扩展URI模板变量.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 转换后的对象
	 */
	<T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象POST到URL来创建新资源, 并将响应返回为{@link ResponseEntity}.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * 
	 * @return 转换后的对象
	 */
	<T> ResponseEntity<T> postForEntity(URI url, Object request, Class<T> responseType) throws RestClientException;


	// PUT

	/**
	 * 通过将给定对象PUT到URI来创建或更新资源.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要PUT的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 */
	void put(String url, Object request, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象PUT到URI模板来创建新资源.
	 * <p>使用给定的Map扩展URI模板变量.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要PUT的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 */
	void put(String url, Object request, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象PUT到URL来创建新资源.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * 
	 * @param url the URL
	 * @param request 要PUT的对象 (may be {@code null})
	 */
	void put(URI url, Object request) throws RestClientException;


	// PATCH

	/**
	 * 通过将给定对象PATCH到URI模板来更新资源, 并返回在响应中找到的表示.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * <p><b>NOTE: 标准JDK HTTP库不支持HTTP PATCH. 需要使用Apache HttpComponents或OkHttp请求工厂.</b>
	 * 
	 * @param url the URL
	 * @param request 要PATCH的对象 (may be {@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 转换后的对象
	 */
	<T> T patchForObject(String url, Object request, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象PATCH到URI模板来更新资源, 并返回在响应中找到的表示.
	 * <p>使用给定的Map扩展URI模板变量.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * <p><b>NOTE: 标准JDK HTTP库不支持HTTP PATCH. 需要使用Apache HttpComponents或OkHttp请求工厂.</b>
	 * 
	 * @param url the URL
	 * @param request 要PATCH的对象 (may be {@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 转换后的对象
	 */
	<T> T patchForObject(String url, Object request, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象PATCH到URL来更新资源, 并返回在响应中找到的表示.
	 * <p>{@code request}参数可以是{@link HttpEntity}, 以便为请求添加额外的HTTP header.
	 * <p><b>NOTE: 标准JDK HTTP库不支持HTTP PATCH. 需要使用Apache HttpComponents或OkHttp请求工厂.</b>
	 * 
	 * @param url the URL
	 * @param request 要PATCH的对象 (may be {@code null})
	 * @param responseType 返回值的类型
	 * 
	 * @return 转换后的对象
	 */
	<T> T patchForObject(URI url, Object request, Class<T> responseType) throws RestClientException;



	// DELETE

	/**
	 * 删除指定URI处的资源.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param uriVariables 要在模板中展开的变量
	 */
	void delete(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 删除指定URI处的资源.
	 * <p>使用给定的Map扩展URI模板变量.
	 *
	 * @param url the URL
	 * @param uriVariables 用于扩展模板的变量
	 */
	void delete(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 删除指定URL处的资源.
	 * 
	 * @param url the URL
	 */
	void delete(URI url) throws RestClientException;


	// OPTIONS

	/**
	 * 返回给定URI的Allow header的值.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param uriVariables 要在模板中展开的变量
	 * 
	 * @return the value of the allow header
	 */
	Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 返回给定URI的Allow header的值.
	 * <p>使用给定的Map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param uriVariables 要在模板中展开的变量
	 * 
	 * @return allow header的值
	 */
	Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 返回给定URI的Allow header的值.
	 * 
	 * @param url the URL
	 * 
	 * @return allow header的值
	 */
	Set<HttpMethod> optionsForAllow(URI url) throws RestClientException;


	// exchange

	/**
	 * 对给定的URI模板执行HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 写入请求的实体 (header和/或正文)(可能是{@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 要在模板中展开的变量
	 * 
	 * @return 响应实体
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
			Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 对给定的URI模板执行HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 写入请求的实体 (header和/或正文)(可能是{@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 要在模板中展开的变量
	 * 
	 * @return 响应实体
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 对给定的URI模板执行HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 写入请求的实体 (header和/或正文)(可能是{@code null})
	 * @param responseType 返回值的类型
	 * 
	 * @return 响应实体
	 */
	<T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity,
			Class<T> responseType) throws RestClientException;

	/**
	 * 对给定的URI模板执行HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * 给定的{@link ParameterizedTypeReference}用于传递泛型类型信息:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 写入请求的实体 (header和/或正文)(可能是{@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 要在模板中展开的变量
	 * 
	 * @return 响应实体
	 */
	<T> ResponseEntity<T> exchange(String url,HttpMethod method, HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 对给定的URI模板执行HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * 给定的{@link ParameterizedTypeReference}用于传递泛型类型信息:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 写入请求的实体 (header和/或正文)(可能是{@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 要在模板中展开的变量
	 * 
	 * @return 响应实体
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 对给定的URI模板执行HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * 给定的{@link ParameterizedTypeReference}用于传递泛型类型信息:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 写入请求的实体 (header和/或正文)(可能是{@code null})
	 * @param responseType 返回值的类型
	 * 
	 * @return 响应实体
	 */
	<T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType) throws RestClientException;

	/**
	 * 执行给定{@link RequestEntity}中指定的请求, 并将响应返回为{@link ResponseEntity}.
	 * 通常与{@code RequestEntity}上的静态构建器方法结合使用, 例如:
	 * <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity.post(new URI(&quot;http://example.com/foo&quot;)).accept(MediaType.APPLICATION_JSON).body(body);
	 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
	 * </pre>
	 * 
	 * @param requestEntity 要写入请求的实体
	 * @param responseType 返回值的类型
	 * 
	 * @return 响应实体
	 */
	<T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType) throws RestClientException;

	/**
	 * 执行给定{@link RequestEntity}中指定的请求, 并将响应返回为{@link ResponseEntity}.
	 * 给定的{@link ParameterizedTypeReference}用于传递泛型类型信息:
	 * <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity.post(new URI(&quot;http://example.com/foo&quot;)).accept(MediaType.APPLICATION_JSON).body(body);
	 * ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyResponse&gt;&gt; response = template.exchange(request, myBean);
	 * </pre>
	 * 
	 * @param requestEntity 要写入请求的实体
	 * @param responseType 返回值的类型
	 * 
	 * @return 响应实体
	 */
	<T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException;


	// general execution

	/**
	 * 对给定的URI模板执行HTTP方法, 使用{@link RequestCallback}准备请求, 并使用{@link ResponseExtractor}读取响应.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables 要在模板中展开的变量
	 * 
	 * @return 任意的对象, 由{@link ResponseExtractor}返回
	 */
	<T> T execute(String url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException;

	/**
	 * 对给定的URI模板执行HTTP方法, 使用{@link RequestCallback}准备请求, 并使用{@link ResponseExtractor}读取响应.
	 * <p>使用给定的URI变量Map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables 要在模板中展开的变量
	 * 
	 * @return 任意的对象, 由{@link ResponseExtractor}返回
	 */
	<T> T execute(String url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 对给定的URL执行HTTP方法, 使用{@link RequestCallback}准备请求, 并使用{@link ResponseExtractor}读取响应.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * 
	 * @return 任意的对象, 由{@link ResponseExtractor}返回
	 */
	<T> T execute(URI url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor) throws RestClientException;

}
