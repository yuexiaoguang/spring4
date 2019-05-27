package org.springframework.messaging.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * {@link MessageCondition}, 用于使用{@link PathMatcher}将Message的目标与一个或多个目标模式进行匹配.
 */
public class DestinationPatternsMessageCondition extends AbstractMessageCondition<DestinationPatternsMessageCondition> {

	public static final String LOOKUP_DESTINATION_HEADER = "lookupDestination";


	private final Set<String> patterns;

	private final PathMatcher pathMatcher;


	/**
	 * 使用给定的目标模式创建新实例.
	 * 每个非空且不以"/"开头的模式前缀为 "/".
	 * 
	 * @param patterns 0个或更多URL模式; 如果为0, 则条件将匹配每个请求.
	 */
	public DestinationPatternsMessageCondition(String... patterns) {
		this(patterns, null);
	}

	/**
	 * 接受自定义PathMatcher.
	 * 
	 * @param patterns 要使用的URL模式; 如果为0, 则条件将匹配每个请求.
	 * @param pathMatcher 要使用的PathMatcher
	 */
	public DestinationPatternsMessageCondition(String[] patterns, PathMatcher pathMatcher) {
		this(asList(patterns), pathMatcher);
	}

	private DestinationPatternsMessageCondition(Collection<String> patterns, PathMatcher pathMatcher) {
		this.pathMatcher = (pathMatcher != null ? pathMatcher : new AntPathMatcher());
		this.patterns = Collections.unmodifiableSet(prependLeadingSlash(patterns, this.pathMatcher));
	}


	private static List<String> asList(String... patterns) {
		return (patterns != null ? Arrays.asList(patterns) : Collections.<String>emptyList());
	}

	private static Set<String> prependLeadingSlash(Collection<String> patterns, PathMatcher pathMatcher) {
		if (patterns == null) {
			return Collections.emptySet();
		}
		boolean slashSeparator = pathMatcher.combine("a", "a").equals("a/a");
		Set<String> result = new LinkedHashSet<String>(patterns.size());
		for (String pattern : patterns) {
			if (slashSeparator) {
				if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
					pattern = "/" + pattern;
				}
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
	 * 返回具有来自当前实例 ("this")和"其它"实例的URL模式的新实例, 如下所示:
	 * <ul>
	 * <li>如果两个实例中都有模式, 使用{@link org.springframework.util.PathMatcher#combine(String, String)}
	 * 将"this"中的模式与"other"中的模式相结合.
	 * <li>如果只有一个实例具有模式, 使用它们.
	 * <li>如果两个实例都没有模式, 使用空字符串 (i.e. "").
	 * </ul>
	 */
	@Override
	public DestinationPatternsMessageCondition combine(DestinationPatternsMessageCondition other) {
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
		return new DestinationPatternsMessageCondition(result, this.pathMatcher);
	}

	/**
	 * 检查是否有任何模式与给定的Message目标匹配, 并返回一个保证包含匹配模式的实例,
	 * 通过{@link org.springframework.util.PathMatcher#getPatternComparator(String)}排序.
	 * 
	 * @param message 要匹配的消息
	 * 
	 * @return 如果条件不包含模式, 则为同一实例;
	 * 或具有已排序匹配模式的新条件;
	 * 或{@code null}, 如果无法提取目标或没有匹配项
	 */
	@Override
	public DestinationPatternsMessageCondition getMatchingCondition(Message<?> message) {
		String destination = (String) message.getHeaders().get(LOOKUP_DESTINATION_HEADER);
		if (destination == null) {
			return null;
		}

		if (this.patterns.isEmpty()) {
			return this;
		}

		List<String> matches = new ArrayList<String>();
		for (String pattern : this.patterns) {
			if (pattern.equals(destination) || this.pathMatcher.match(pattern, destination)) {
				matches.add(pattern);
			}
		}

		if (matches.isEmpty()) {
			return null;
		}

		Collections.sort(matches, this.pathMatcher.getPatternComparator(destination));
		return new DestinationPatternsMessageCondition(matches, this.pathMatcher);
	}

	/**
	 * 根据它们包含的目标模式比较这两个条件.
	 * 模式通过{@link org.springframework.util.PathMatcher#getPatternComparator(String)}从上到下逐个进行比较.
	 * 如果所有比较的模式都匹配, 但是一个实例具有更多模式, 则认为它更接近匹配.
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(Message)}获得的,
	 * 以确保它们只包含与请求匹配的模式, 并在顶部排序最佳匹配.
	 */
	@Override
	public int compareTo(DestinationPatternsMessageCondition other, Message<?> message) {
		String destination = (String) message.getHeaders().get(LOOKUP_DESTINATION_HEADER);
		if (destination == null) {
			return 0;
		}
		Comparator<String> patternComparator = this.pathMatcher.getPatternComparator(destination);

		Iterator<String> iterator = patterns.iterator();
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
