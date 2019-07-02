package org.springframework.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * 匹配{@link WebRequest}的策略.
 */
public interface WebRequestMatcher {

	/**
	 * 此匹配器是否与提供的Web请求匹配.
	 * 
	 * @param request 尝试匹配的{@link WebRequest}
	 * 
	 * @return {@code true} 如果此匹配器匹配{@code WebRequest}
	 */
	boolean matches(WebRequest request);

}
