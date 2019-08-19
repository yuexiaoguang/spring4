package org.springframework.web.servlet.handler;

import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

/**
 * 通过{@link MatchableHandlerMapping}匹配请求模式的结果的容器,
 * 以及从模式中进一步提取URI模板变量的方法.
 */
public class RequestMatchResult {

	private final String matchingPattern;

	private final String lookupPath;

	private final PathMatcher pathMatcher;


	/**
	 * @param matchingPattern 匹配模式, 可能与输入模式不同, e.g. inputPattern="/foo" 和 matchingPattern="/foo/".
	 * @param lookupPath 从请求中提取的查找路径
	 * @param pathMatcher 使用的PathMatcher
	 */
	public RequestMatchResult(String matchingPattern, String lookupPath, PathMatcher pathMatcher) {
		Assert.hasText(matchingPattern, "'matchingPattern' is required");
		Assert.hasText(lookupPath, "'lookupPath' is required");
		Assert.notNull(pathMatcher, "'pathMatcher' is required");
		this.matchingPattern = matchingPattern;
		this.lookupPath = lookupPath;
		this.pathMatcher = pathMatcher;
	}


	/**
	 * 从{@link PathMatcher#extractUriTemplateVariables}中定义的匹配模式中提取URI模板变量.
	 * 
	 * @return 带有URI模板变量的Map
	 */
	public Map<String, String> extractUriTemplateVariables() {
		return this.pathMatcher.extractUriTemplateVariables(this.matchingPattern, this.lookupPath);
	}
}
