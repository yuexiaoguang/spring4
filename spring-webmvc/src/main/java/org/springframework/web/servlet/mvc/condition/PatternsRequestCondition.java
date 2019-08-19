package org.springframework.web.servlet.mvc.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 逻辑或 (' || ') 请求条件, 它根据一组URL路径模式匹配请求.
 */
public final class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

	private final Set<String> patterns;

	private final UrlPathHelper pathHelper;

	private final PathMatcher pathMatcher;

	private final boolean useSuffixPatternMatch;

	private final boolean useTrailingSlashMatch;

	private final List<String> fileExtensions = new ArrayList<String>();


	/**
	 * 每个非空且不以"/"开头的模式加上前缀"/".
	 * 
	 * @param patterns 0个或更多URL模式; 如果为0, 则条件将匹配每个请求.
	 */
	public PatternsRequestCondition(String... patterns) {
		this(asList(patterns), null, null, true, true, null);
	}

	/**
	 * 用于使用后缀模式 (.*) 和尾部斜杠匹配.
	 * 
	 * @param patterns 要使用的URL模式; 如果为0, 则条件将匹配每个请求.
	 * @param urlPathHelper 用于确定请求的查找路径
	 * @param pathMatcher 用于与模式匹配的路径
	 * @param useSuffixPatternMatch 是否启用按后缀 (".*")匹配
	 * @param useTrailingSlashMatch 是否匹配, 不管尾部斜杠
	 */
	public PatternsRequestCondition(String[] patterns, UrlPathHelper urlPathHelper, PathMatcher pathMatcher,
			boolean useSuffixPatternMatch, boolean useTrailingSlashMatch) {

		this(asList(patterns), urlPathHelper, pathMatcher, useSuffixPatternMatch, useTrailingSlashMatch, null);
	}

	/**
	 * 每个非空且不以"/"开头的模式都加上前缀"/".
	 * 
	 * @param patterns 要使用的URL模式; 如果为0, 则条件将匹配每个请求.
	 * @param urlPathHelper 用于确定请求的查找路径的{@link UrlPathHelper}
	 * @param pathMatcher 用于模式路径匹配的{@link PathMatcher}
	 * @param useSuffixPatternMatch 是否启用按后缀 (".*")匹配
	 * @param useTrailingSlashMatch 是否匹配, 不管尾部斜杠
	 * @param fileExtensions 要考虑路径匹配的文件扩展名列表
	 */
	public PatternsRequestCondition(String[] patterns, UrlPathHelper urlPathHelper,
			PathMatcher pathMatcher, boolean useSuffixPatternMatch, boolean useTrailingSlashMatch,
			List<String> fileExtensions) {

		this(asList(patterns), urlPathHelper, pathMatcher, useSuffixPatternMatch, useTrailingSlashMatch, fileExtensions);
	}

	/**
	 * 接受模式集合.
	 */
	private PatternsRequestCondition(Collection<String> patterns, UrlPathHelper urlPathHelper,
			PathMatcher pathMatcher, boolean useSuffixPatternMatch, boolean useTrailingSlashMatch,
			List<String> fileExtensions) {

		this.patterns = Collections.unmodifiableSet(prependLeadingSlash(patterns));
		this.pathHelper = (urlPathHelper != null ? urlPathHelper : new UrlPathHelper());
		this.pathMatcher = (pathMatcher != null ? pathMatcher : new AntPathMatcher());
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		this.useTrailingSlashMatch = useTrailingSlashMatch;
		if (fileExtensions != null) {
			for (String fileExtension : fileExtensions) {
				if (fileExtension.charAt(0) != '.') {
					fileExtension = "." + fileExtension;
				}
				this.fileExtensions.add(fileExtension);
			}
		}
	}


	private static List<String> asList(String... patterns) {
		return (patterns != null ? Arrays.asList(patterns) : Collections.<String>emptyList());
	}

	private static Set<String> prependLeadingSlash(Collection<String> patterns) {
		if (patterns == null) {
			return Collections.emptySet();
		}
		Set<String> result = new LinkedHashSet<String>(patterns.size());
		for (String pattern : patterns) {
			if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			result.add(pattern);
		}
		return result;
	}

	public Set<String> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<String> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 返回具有来自当前实例 ("this")和"other"实例的URL模式的新实例, 如下所示:
	 * <ul>
	 * <li>如果两个实例中都有模式, 使用{@link PathMatcher#combine(String, String)}将"this"中的模式与"other"中的模式相结合.
	 * <li>如果只有一个实例具有模式, 使用它们.
	 * <li>如果两个实例都没有模式, 使用空字符串 (i.e. "").
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		Set<String> result = new LinkedHashSet<String>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (String pattern1 : this.patterns) {
				for (String pattern2 : other.patterns) {
					result.add(this.pathMatcher.combine(pattern1, pattern2));
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			result.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			result.addAll(other.patterns);
		}
		else {
			result.add("");
		}
		return new PatternsRequestCondition(result, this.pathHelper, this.pathMatcher, this.useSuffixPatternMatch,
				this.useTrailingSlashMatch, this.fileExtensions);
	}

	/**
	 * 检查是否有任何模式与给定请求匹配, 并返回一个保证包含匹配模式的实例,
	 * 通过{@link PathMatcher#getPatternComparator(String)}排序.
	 * <p>通过按以下顺序进行检查来获得匹配模式:
	 * <ul>
	 * <li>直接匹配
	 * <li>如果模式尚未包含".", 则使用附加的".*"模式匹配
	 * <li>模式匹配
	 * <li>如果模式尚未以"/"结尾, 则使用附加的"/"模式匹配
	 * </ul>
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 如果条件不包含模式, 则为同一实例;
	 * 或具有排序匹配模式的新条件; 如果没有模式匹配, 则为{@code null}.
	 */
	@Override
	public PatternsRequestCondition getMatchingCondition(HttpServletRequest request) {

		if (this.patterns.isEmpty()) {
			return this;
		}

		String lookupPath = this.pathHelper.getLookupPathForRequest(request);
		List<String> matches = getMatchingPatterns(lookupPath);

		return matches.isEmpty() ? null :
			new PatternsRequestCondition(matches, this.pathHelper, this.pathMatcher, this.useSuffixPatternMatch,
					this.useTrailingSlashMatch, this.fileExtensions);
	}

	/**
	 * 查找与给定查找路径匹配的模式.
	 * 调用此方法应该产生与调用
	 * {@link #getMatchingCondition(javax.servlet.http.HttpServletRequest)}相同的结果.
	 * 如果没有可用的请求 (e.g. 内省, 工具等), 可以使用此方法作为替代方法.
	 * 
	 * @param lookupPath 用于匹配现有模式的查找路径
	 * 
	 * @return 匹配模式的集合, 最接近的匹配排在最上面
	 */
	public List<String> getMatchingPatterns(String lookupPath) {
		List<String> matches = new ArrayList<String>();
		for (String pattern : this.patterns) {
			String match = getMatchingPattern(pattern, lookupPath);
			if (match != null) {
				matches.add(match);
			}
		}
		Collections.sort(matches, this.pathMatcher.getPatternComparator(lookupPath));
		return matches;
	}

	private String getMatchingPattern(String pattern, String lookupPath) {
		if (pattern.equals(lookupPath)) {
			return pattern;
		}
		if (this.useSuffixPatternMatch) {
			if (!this.fileExtensions.isEmpty() && lookupPath.indexOf('.') != -1) {
				for (String extension : this.fileExtensions) {
					if (this.pathMatcher.match(pattern + extension, lookupPath)) {
						return pattern + extension;
					}
				}
			}
			else {
				boolean hasSuffix = pattern.indexOf('.') != -1;
				if (!hasSuffix && this.pathMatcher.match(pattern + ".*", lookupPath)) {
					return pattern + ".*";
				}
			}
		}
		if (this.pathMatcher.match(pattern, lookupPath)) {
			return pattern;
		}
		if (this.useTrailingSlashMatch) {
			if (!pattern.endsWith("/") && this.pathMatcher.match(pattern + "/", lookupPath)) {
				return pattern +"/";
			}
		}
		return null;
	}

	/**
	 * 根据它们包含的URL模式比较这两个条件.
	 * 模式通过{@link PathMatcher#getPatternComparator(String)}从上到下逐个进行比较.
	 * 如果所有比较的模式都匹配, 但是一个实例具有更多模式, 则认为它更接近匹配.
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的,
	 * 以确保它们只包含与请求匹配的模式, 而且最佳匹配排在最上面.
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, HttpServletRequest request) {
		String lookupPath = this.pathHelper.getLookupPathForRequest(request);
		Comparator<String> patternComparator = this.pathMatcher.getPatternComparator(lookupPath);
		Iterator<String> iterator = this.patterns.iterator();
		Iterator<String> iteratorOther = other.patterns.iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
			if (result != 0) {
				return result;
			}
		}
		if (iterator.hasNext()) {
			return -1;
		}
		else if (iteratorOther.hasNext()) {
			return 1;
		}
		else {
			return 0;
		}
	}

}
