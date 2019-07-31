package org.springframework.web.client;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 指定一组基本的异步RESTful操作的接口.
 * 由{@link AsyncRestTemplate}实现. 不经常直接使用, 但是增强可测试性的有用选项, 因为它很容易被模拟或存根.
 */
public interface AsyncRestOperations {

	/**
	 * 公开同步Spring RestTemplate以允许同步调用.
	 */
	RestOperations getRestOperations();


	// GET

	/**
	 * 通过在指定的URL上执行GET来异步检索实体.
	 * 响应被转换并存储在{@link ResponseEntity}中.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Object... uriVariables) throws RestClientException;

	/**
	 * 通过在URI模板上执行GET来异步检索表示.
	 * 响应被转换并存储在{@link ResponseEntity}中.
	 * <p>使用给定的map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 包含URI模板变量的Map
	 * 
	 * @return 包装在{@link Future}中的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过在URL上执行GET异步检索表示.
	 * 响应被转换并存储在{@link ResponseEntity}中.
	 * 
	 * @param url the URL
	 * @param responseType 返回值的类型
	 * 
	 * @return 包装在{@link Future}中的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(URI url, Class<T> responseType)
			throws RestClientException;


	// HEAD

	/**
	 * 异步检索URI模板指定的资源的所有header.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的该资源的所有HTTP header
	 */
	ListenableFuture<HttpHeaders> headForHeaders(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步检索URI模板指定的资源的所有header.
	 * <p>使用给定的Map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param uriVariables 包含URI模板变量的Map
	 * 
	 * @return 包装在{@link Future}中的该资源的所有HTTP header
	 */
	ListenableFuture<HttpHeaders> headForHeaders(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 异步检索URL指定的资源的所有header.
	 * 
	 * @param url the URL
	 * 
	 * @return 包装在{@link Future}中的该资源的所有HTTP header
	 */
	ListenableFuture<HttpHeaders> headForHeaders(URI url) throws RestClientException;


	// POST

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并异步返回{@code Location} header的值.
	 * 此header通常指示新资源的存储位置.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的{@code Location} header的值
	 */
	ListenableFuture<URI> postForLocation(String url, HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并异步返回{@code Location} header的值.
	 * 此header通常指示新资源的存储位置.
	 * <p>使用给定的Map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的{@code Location} header的值
	 */
	ListenableFuture<URI> postForLocation(String url, HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象POST到URL来创建新资源, 并异步返回{@code Location} header的值.
	 * 此header通常指示新资源的存储位置.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * 
	 * @return 包装在{@link Future}中的{@code Location} header的值
	 */
	ListenableFuture<URI> postForLocation(URI url, HttpEntity<?> request) throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并以{@link ResponseEntity}的方式异步返回响应.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, HttpEntity<?> request,
			Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源, 并以{@link ResponseEntity}的方式异步返回响应.
	 * <p>使用给定的Map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, HttpEntity<?> request,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象POST到URL来创建新资源, 并以{@link ResponseEntity}的方式异步返回响应.
	 * 
	 * @param url the URL
	 * @param request 要POST的对象 (may be {@code null})
	 * 
	 * @return 包装在{@link Future}中的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(URI url, HttpEntity<?> request,
			Class<T> responseType) throws RestClientException;


	// PUT

	/**
	 * 通过将给定对象PUT到URI来创建或更新资源.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * <p>Future将在完成后返回{@code null}结果.
	 * 
	 * @param url the URL
	 * @param request 要PUT的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 */
	ListenableFuture<?> put(String url, HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象PUT到URI模板来创建新资源.
	 * <p>使用给定的Map扩展URI模板变量.
	 * <p>Future将在完成后返回{@code null}结果.
	 * 
	 * @param url the URL
	 * @param request 要PUT的对象 (may be {@code null})
	 * @param uriVariables 用于扩展模板的变量
	 */
	ListenableFuture<?> put(String url, HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象PUT到URL来创建新资源.
	 * <p>Future将在完成后返回{@code null}结果.
	 * 
	 * @param url the URL
	 * @param request 要PUT的对象 (may be {@code null})
	 */
	ListenableFuture<?> put(URI url, HttpEntity<?> request) throws RestClientException;


	// DELETE

	/**
	 * 异步删除指定URI处的资源.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * <p>Future将在完成后返回{@code null}结果.
	 * 
	 * @param url the URL
	 * @param uriVariables 用于扩展模板的变量
	 */
	ListenableFuture<?> delete(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 异步删除指定URI处的资源.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * <p>Future将在完成后返回{@code null}结果.
	 * 
	 * @param url the URL
	 * @param uriVariables 用于扩展模板的变量
	 */
	ListenableFuture<?> delete(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步删除指定URI处的资源.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * <p>Future将在完成后返回{@code null}结果.
	 * 
	 * @param url the URL
	 */
	ListenableFuture<?> delete(URI url) throws RestClientException;


	// OPTIONS

	/**
	 * 异步返回给定URI的Allow header的值.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的allow header的值
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步返回给定URI的Allow header的值.
	 * <p>使用给定的Map扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的allow header的值
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 异步返回给定URL的Allow header的值.
	 * 
	 * @param url the URL
	 * 
	 * @return 包装在{@link Future}中的allow header的值
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(URI url) throws RestClientException;


	// exchange

	/**
	 * 异步执行给定URI模板的HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 要写入请求的实体 (header和/或正文) (may be {@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步执行给定URI模板的HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 要写入请求的实体 (header和/或正文) (may be {@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步执行给定URI模板的HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 要写入请求的实体 (header和/或正文) (may be {@code null})
	 * @param responseType 返回值的类型
	 * 
	 * @return 包装在{@link Future}中的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType)
			throws RestClientException;

	/**
	 * 异步执行给定URI模板的HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * 给定的{@link ParameterizedTypeReference}用于传递泛型类型信息:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 要写入请求的实体 (header和/或正文) (may be {@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Object... uriVariables) throws RestClientException;

	/**
	 * 异步执行给定URI模板的HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * 给定的{@link ParameterizedTypeReference}用于传递泛型类型信息:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 要写入请求的实体 (header和/或正文) (may be {@code null})
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 包装在{@link Future}中的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步执行给定URI模板的HTTP方法, 将给定的请求实体写入请求, 并将响应返回为{@link ResponseEntity}.
	 * 给定的{@link ParameterizedTypeReference}用于传递泛型类型信息:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestEntity 要写入请求的实体 (header和/或正文) (may be {@code null})
	 * @param responseType 返回值的类型
	 * 
	 * @return 包装在{@link Future}中的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException;


	// general execution

	/**
	 * 异步执行给定URI模板的HTTP方法, 使用{@link AsyncRequestCallback}准备请求, 并使用{@link ResponseExtractor}读取响应.
	 * <p>使用给定的URI变量扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 任意的对象, 由{@link ResponseExtractor}返回
	 */
	<T> ListenableFuture<T> execute(String url, HttpMethod method,
			AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor,
			Object... uriVariables) throws RestClientException;

	/**
	 * 异步执行给定URI模板的HTTP方法, 使用{@link AsyncRequestCallback}准备请求, 并使用{@link ResponseExtractor}读取响应.
	 * <p>使用给定的URI变量映射扩展URI模板变量.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables 用于扩展模板的变量
	 * 
	 * @return 任意的对象, 由{@link ResponseExtractor}返回
	 */
	<T> ListenableFuture<T> execute(String url, HttpMethod method,
			AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步执行给定URL的HTTP方法, 使用{@link AsyncRequestCallback}准备请求, 并使用{@link ResponseExtractor}读取响应.
	 * 
	 * @param url the URL
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * 
	 * @return 任意的对象, 由{@link ResponseExtractor}返回
	 */
	<T> ListenableFuture<T> execute(URI url, HttpMethod method,
			AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor)
			throws RestClientException;

}
