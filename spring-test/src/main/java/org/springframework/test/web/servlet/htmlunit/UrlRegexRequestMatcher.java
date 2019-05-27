package org.springframework.test.web.servlet.htmlunit;

import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * A {@link WebRequestMatcher} that allows matching on
 * {@code WebRequest#getUrl().toExternalForm()} using a regular expression.
 *
 * <p>For example, if you would like to match on the domain {@code code.jquery.com},
 * you might want to use the following.
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
