package org.springframework.web.servlet.mvc.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsUtils;

/**
 * 一个逻辑或 (' || ') 请求条件, 它匹配一组{@link RequestMethod}的请求.
 */
public final class RequestMethodsRequestCondition extends AbstractRequestCondition<RequestMethodsRequestCondition> {

	private static final RequestMethodsRequestCondition GET_CONDITION =
			new RequestMethodsRequestCondition(RequestMethod.GET);


	private final Set<RequestMethod> methods;


	/**
	 * @param requestMethods 0个或更多HTTP请求方法; 0条件将匹配每个请求
	 */
	public RequestMethodsRequestCondition(RequestMethod... requestMethods) {
		this(asList(requestMethods));
	}

	private RequestMethodsRequestCondition(Collection<RequestMethod> requestMethods) {
		this.methods = Collections.unmodifiableSet(new LinkedHashSet<RequestMethod>(requestMethods));
	}


	private static List<RequestMethod> asList(RequestMethod... requestMethods) {
		return (requestMethods != null ? Arrays.asList(requestMethods) : Collections.<RequestMethod>emptyList());
	}


	/**
	 * 返回此条件中包含的所有{@link RequestMethod}.
	 */
	public Set<RequestMethod> getMethods() {
		return this.methods;
	}

	@Override
	protected Collection<RequestMethod> getContent() {
		return this.methods;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 返回一个新实例, 其中包含来自"this"和"other"实例的HTTP请求方法的并集.
	 */
	@Override
	public RequestMethodsRequestCondition combine(RequestMethodsRequestCondition other) {
		Set<RequestMethod> set = new LinkedHashSet<RequestMethod>(this.methods);
		set.addAll(other.methods);
		return new RequestMethodsRequestCondition(set);
	}

	/**
	 * 检查是否有任何HTTP请求方法与给定请求匹配, 并返回仅包含匹配的HTTP请求方法的实例.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 如果条件为空(除非请求方法是 HTTP OPTIONS), 则使用匹配的请求方法的新条件,
	 * 如果没有匹配或条件为空且请求方法为OPTIONS, 则为{@code null}.
	 */
	@Override
	public RequestMethodsRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return matchPreFlight(request);
		}

		if (getMethods().isEmpty()) {
			if (RequestMethod.OPTIONS.name().equals(request.getMethod()) &&
					!DispatcherType.ERROR.equals(request.getDispatcherType())) {

				return null; // No implicit match for OPTIONS (we handle it)
			}
			return this;
		}

		return matchRequestMethod(request.getMethod());
	}

	/**
	 * 匹配pre-flight请求.
	 * 因此, 空条件是匹配的, 否则尝试匹配"Access-Control-Request-Method" header中的HTTP方法.
	 */
	private RequestMethodsRequestCondition matchPreFlight(HttpServletRequest request) {
		if (getMethods().isEmpty()) {
			return this;
		}
		String expectedMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
		return matchRequestMethod(expectedMethod);
	}

	private RequestMethodsRequestCondition matchRequestMethod(String httpMethodValue) {
		HttpMethod httpMethod = HttpMethod.resolve(httpMethodValue);
		if (httpMethod != null) {
			for (RequestMethod method : getMethods()) {
				if (httpMethod.matches(method.name())) {
					return new RequestMethodsRequestCondition(method);
				}
			}
			if (httpMethod == HttpMethod.HEAD && getMethods().contains(RequestMethod.GET)) {
				return GET_CONDITION;
			}
		}
		return null;
	}

	/**
	 * 返回:
	 * <ul>
	 * <li>如果两个条件包含相同数量的HTTP请求方法, 则为0
	 * <li>如果"this"实例具有的HTTP请求方法"other"不具有, 则小于0
	 * <li>"other"具有的HTTP请求方法"this"不具有, 则大于0
	 * </ul>
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的,
	 * 因此每个实例仅包含匹配的HTTP请求方法, 否则为空.
	 */
	@Override
	public int compareTo(RequestMethodsRequestCondition other, HttpServletRequest request) {
		if (other.methods.size() != this.methods.size()) {
			return other.methods.size() - this.methods.size();
		}
		else if (this.methods.size() == 1) {
			if (this.methods.contains(RequestMethod.HEAD) && other.methods.contains(RequestMethod.GET)) {
				return -1;
			}
			else if (this.methods.contains(RequestMethod.GET) && other.methods.contains(RequestMethod.HEAD)) {
				return 1;
			}
		}
		return 0;
	}

}
