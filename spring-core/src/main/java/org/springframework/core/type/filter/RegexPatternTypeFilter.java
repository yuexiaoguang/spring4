package org.springframework.core.type.filter;

import java.util.regex.Pattern;

import org.springframework.core.type.ClassMetadata;
import org.springframework.util.Assert;

/**
 * 一个简单的过滤器, 用于将完全限定的类名与正则表达式{@link Pattern}进行匹配.
 */
public class RegexPatternTypeFilter extends AbstractClassTestingTypeFilter {

	private final Pattern pattern;


	public RegexPatternTypeFilter(Pattern pattern) {
		Assert.notNull(pattern, "Pattern must not be null");
		this.pattern = pattern;
	}


	@Override
	protected boolean match(ClassMetadata metadata) {
		return this.pattern.matcher(metadata.getClassName()).matches();
	}

}
