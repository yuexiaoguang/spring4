package org.springframework.test.web.servlet.htmlunit;

import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * {@link WebRequestMatcher}, 允许使用正则表达式在{@code WebRequest#getUrl().toExternalForm()}上进行匹配.
 *
 * <p>例如, 如果想在域{@code code.jquery.com}上匹配, 可能想要使用以下内容.
 *
 * <pre class="code">
 * WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
 * </pre>
 */
public final class UrlRegexRequestMatcher implements WebRequestMatcher {

	private final Pattern pattern;


	public UrlRegexRequestMatcher(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	public UrlRegexRequestMatcher(Pattern pattern) {
		this.pattern = pattern;
	}


	@Override
	public boolean matches(WebRequest request) {
		String url = request.getUrl().toExternalForm();
		return this.pattern.matcher(url).matches();
	}

}
