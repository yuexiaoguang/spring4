package org.springframework.web.util;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * 表示URI模板.
 * URI模板是一个类似URI的字符串, 它包含由大括号 ({@code {}})括起来的变量, 可以展开这些变量以生成实际的URI.
 *
 * <p>请参阅{@link #expand(Map)}, {@link #expand(Object[])}, 和{@link #match(String)}示例用法.
 *
 * <p>此类设计为线程安全且可重用, 允许任意数量的扩展或匹配调用.
 */
@SuppressWarnings("serial")
public class UriTemplate implements Serializable {

	private final String uriTemplate;

	private final UriComponents uriComponents;

	private final List<String> variableNames;

	private final Pattern matchPattern;


	/**
	 * @param uriTemplate URI模板字符串
	 */
	public UriTemplate(String uriTemplate) {
		Assert.hasText(uriTemplate, "'uriTemplate' must not be null");
		this.uriTemplate = uriTemplate;
		this.uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).build();

		TemplateInfo info = TemplateInfo.parse(uriTemplate);
		this.variableNames = Collections.unmodifiableList(info.getVariableNames());
		this.matchPattern = info.getMatchPattern();
	}


	/**
	 * 按顺序返回模板中变量的名称.
	 * 
	 * @return 模板变量名称
	 */
	public List<String> getVariableNames() {
		return this.variableNames;
	}

	/**
	 * 给定变量Map, 将此模板扩展为URI.
	 * Map键表示变量名称, Map值表示变量值. 变量的顺序并不重要.
	 * <p>Example:
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
	 * Map&lt;String, String&gt; uriVariables = new HashMap&lt;String, String&gt;();
	 * uriVariables.put("booking", "42");
	 * uriVariables.put("hotel", "Rest & Relax");
	 * System.out.println(template.expand(uriVariables));
	 * </pre>
	 * 将打印: <blockquote>{@code http://example.com/hotels/Rest%20%26%20Relax/bookings/42}</blockquote>
	 * 
	 * @param uriVariables URI变量的映射
	 * 
	 * @return 扩展的URI
	 * @throws IllegalArgumentException 如果{@code uriVariables}是{@code null};
	 * 或者如果它不包含所有变量名的值
	 */
	public URI expand(Map<String, ?> uriVariables) {
		UriComponents expandedComponents = this.uriComponents.expand(uriVariables);
		UriComponents encodedComponents = expandedComponents.encode();
		return encodedComponents.toUri();
	}

    /**
     * 给定一组变量, 将此模板扩展为完整URI. 数组表示变量值.
     * 变量的顺序很重要.
     * <p>Example:
     * <pre class="code">
     * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
     * System.out.println(template.expand("Rest & Relax", 42));
     * </pre>
     * 将打印: <blockquote>{@code http://example.com/hotels/Rest%20%26%20Relax/bookings/42}</blockquote>
     * 
     * @param uriVariableValues URI变量数组
     * 
     * @return 扩展的URI
     * @throws IllegalArgumentException 如果{@code uriVariables}是{@code null}或者它不包含足够的变量
     */
	public URI expand(Object... uriVariableValues) {
		UriComponents expandedComponents = this.uriComponents.expand(uriVariableValues);
		UriComponents encodedComponents = expandedComponents.encode();
		return encodedComponents.toUri();
	}

	/**
	 * 指示给定的URI是否与此模板匹配.
	 * 
	 * @param uri 要匹配的URI
	 * 
	 * @return {@code true} 如果匹配; 否则{@code false}
	 */
	public boolean matches(String uri) {
		if (uri == null) {
			return false;
		}
		Matcher matcher = this.matchPattern.matcher(uri);
		return matcher.matches();
	}

	/**
	 * 将给定的URI与变量值的Map匹配.
	 * 返回的Map中的键是变量名, 值是变量值, 如给定URI中所示.
	 * <p>Example:
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
	 * System.out.println(template.match("http://example.com/hotels/1/bookings/42"));
	 * </pre>
	 * 将打印: <blockquote>{@code {hotel=1, booking=42}}</blockquote>
	 * 
	 * @param uri 要匹配的URI
	 * 
	 * @return 变量值的Map
	 */
	public Map<String, String> match(String uri) {
		Assert.notNull(uri, "'uri' must not be null");
		Map<String, String> result = new LinkedHashMap<String, String>(this.variableNames.size());
		Matcher matcher = this.matchPattern.matcher(uri);
		if (matcher.find()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				String name = this.variableNames.get(i - 1);
				String value = matcher.group(i);
				result.put(name, value);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return this.uriTemplate;
	}


	/**
	 * 提取变量名称和正则表达式以匹配实际的URL的工具类.
	 */
	private static class TemplateInfo {

		private final List<String> variableNames;

		private final Pattern pattern;

		private TemplateInfo(List<String> vars, Pattern pattern) {
			this.variableNames = vars;
			this.pattern = pattern;
		}

		public List<String> getVariableNames() {
			return this.variableNames;
		}

		public Pattern getMatchPattern() {
			return this.pattern;
		}

		public static TemplateInfo parse(String uriTemplate) {
			int level = 0;
			List<String> variableNames = new ArrayList<String>();
			StringBuilder pattern = new StringBuilder();
			StringBuilder builder = new StringBuilder();
			for (int i = 0 ; i < uriTemplate.length(); i++) {
				char c = uriTemplate.charAt(i);
				if (c == '{') {
					level++;
					if (level == 1) {
						// start of URI variable
						pattern.append(quote(builder));
						builder = new StringBuilder();
						continue;
					}
				}
				else if (c == '}') {
					level--;
					if (level == 0) {
						// end of URI variable
						String variable = builder.toString();
						int idx = variable.indexOf(':');
						if (idx == -1) {
							pattern.append("(.*)");
							variableNames.add(variable);
						}
						else {
							if (idx + 1 == variable.length()) {
								throw new IllegalArgumentException(
										"No custom regular expression specified after ':' in \"" + variable + "\"");
							}
							String regex = variable.substring(idx + 1, variable.length());
							pattern.append('(');
							pattern.append(regex);
							pattern.append(')');
							variableNames.add(variable.substring(0, idx));
						}
						builder = new StringBuilder();
						continue;
					}
				}
				builder.append(c);
			}
			if (builder.length() > 0) {
				pattern.append(quote(builder));
			}
			return new TemplateInfo(variableNames, Pattern.compile(pattern.toString()));
		}

		private static String quote(StringBuilder builder) {
			return (builder.length() > 0 ? Pattern.quote(builder.toString()) : "");
		}
	}

}
