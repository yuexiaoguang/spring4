package org.springframework.web.util;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * 表示URI组件的不可变集合, 将组件类型映射到String值.
 * 包含所有组件的 getter.
 * 与{@link java.net.URI}类似, 但具有更强大的编码选项和对URI模板变量的支持.
 */
@SuppressWarnings("serial")
public abstract class UriComponents implements Serializable {

	private static final String DEFAULT_ENCODING = "UTF-8";

	/** 捕获URI模板变量名称 */
	private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");


	private final String scheme;

	private final String fragment;


	protected UriComponents(String scheme, String fragment) {
		this.scheme = scheme;
		this.fragment = fragment;
	}


	// Component getters

	/**
	 * 返回scheme. 可以是{@code null}.
	 */
	public final String getScheme() {
		return this.scheme;
	}

	/**
	 * 返回分段. 可以是{@code null}.
	 */
	public final String getFragment() {
		return this.fragment;
	}

	/**
	 * 返回scheme特定部分. 可以是{@code null}.
	 */
	public abstract String getSchemeSpecificPart();

	/**
	 * 返回用户信息. 可以是{@code null}.
	 */
	public abstract String getUserInfo();

	/**
	 * 返回主机. 可以是{@code null}.
	 */
	public abstract String getHost();

	/**
	 * 返回端口. {@code -1} 如果没有设置端口.
	 */
	public abstract int getPort();

	/**
	 * 返回路径. 可以是{@code null}.
	 */
	public abstract String getPath();

	/**
	 * 返回路径分段的列表. 如果未设置路径, 则为空.
	 */
	public abstract List<String> getPathSegments();

	/**
	 * 返回查询. 可以是{@code null}.
	 */
	public abstract String getQuery();

	/**
	 * 返回查询参数的Map. 如果未设置任何查询, 则为空.
	 */
	public abstract MultiValueMap<String, String> getQueryParams();


	/**
	 * 使用特定的编码规则对所有URI组件进行编码, 并将结果作为新的{{@code UriComponents}实例返回.
	 * 此方法使用UTF-8进行编码.
	 * 
	 * @return 编码的URI组件
	 */
	public final UriComponents encode() {
		try {
			return encode(DEFAULT_ENCODING);
		}
		catch (UnsupportedEncodingException ex) {
			// should not occur
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * 使用特定的编码规则对所有URI组件进行编码, 并将结果作为新的{@code UriComponents}实例返回.
	 * 
	 * @param encoding 此Map中包含的值的编码
	 * 
	 * @return 编码的URI组件
	 * @throws UnsupportedEncodingException 如果不支持给定的编码
	 */
	public abstract UriComponents encode(String encoding) throws UnsupportedEncodingException;

	/**
	 * 使用给定Map中的值替换所有URI模板变量.
	 * <p>键表示变量名称; 相应的值表示变量值.
	 * 变量的顺序并不重要.
	 * 
	 * @param uriVariables URI变量的Map
	 * 
	 * @return 扩展的URI组件
	 */
	public final UriComponents expand(Map<String, ?> uriVariables) {
		Assert.notNull(uriVariables, "'uriVariables' must not be null");
		return expandInternal(new MapTemplateVariables(uriVariables));
	}

	/**
	 * 使用给定数组中的值替换所有URI模板变量.
	 * <p>给定的数组表示变量值. 变量的顺序很重要.
	 * 
	 * @param uriVariableValues URI变量值
	 * 
	 * @return 扩展的URI组件
	 */
	public final UriComponents expand(Object... uriVariableValues) {
		Assert.notNull(uriVariableValues, "'uriVariableValues' must not be null");
		return expandInternal(new VarArgsTemplateVariables(uriVariableValues));
	}

	/**
	 * 使用给定{@link UriTemplateVariables}中的值替换所有URI模板变量.
	 * 
	 * @param uriVariables URI模板值
	 * 
	 * @return 扩展的URI组件
	 */
	public final UriComponents expand(UriTemplateVariables uriVariables) {
		Assert.notNull(uriVariables, "'uriVariables' must not be null");
		return expandInternal(uriVariables);
	}

	/**
	 * 使用给定{@link UriTemplateVariables}中的值替换所有URI模板变量
	 * 
	 * @param uriVariables URI模板值
	 * 
	 * @return 扩展的URI组件
	 */
	abstract UriComponents expandInternal(UriTemplateVariables uriVariables);

	/**
	 * 规范化路径删除序列, 如"path/..".
	 */
	public abstract UriComponents normalize();

	/**
	 * 从此{@code UriComponents}实例返回URI字符串.
	 */
	public abstract String toUriString();

	/**
	 * 从此{@code UriComponents}实例返回{@code URI}.
	 */
	public abstract URI toUri();

	@Override
	public final String toString() {
		return toUriString();
	}

	/**
	 * 设置给定UriComponentsBuilder的所有组件.
	 */
	protected abstract void copyToUriComponentsBuilder(UriComponentsBuilder builder);


	// Static expansion helpers

	static String expandUriComponent(String source, UriTemplateVariables uriVariables) {
		if (source == null) {
			return null;
		}
		if (source.indexOf('{') == -1) {
			return source;
		}
		if (source.indexOf(':') != -1) {
			source = sanitizeSource(source);
		}
		Matcher matcher = NAMES_PATTERN.matcher(source);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String match = matcher.group(1);
			String variableName = getVariableName(match);
			Object variableValue = uriVariables.getValue(variableName);
			if (UriTemplateVariables.SKIP_VALUE.equals(variableValue)) {
				continue;
			}
			String variableValueString = getVariableValueAsString(variableValue);
			String replacement = Matcher.quoteReplacement(variableValueString);
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * 删除嵌套的 "{}", 例如在带有正则表达式的URI变量中.
	 */
	private static String sanitizeSource(String source) {
		int level = 0;
		StringBuilder sb = new StringBuilder();
		for (char c : source.toCharArray()) {
			if (c == '{') {
				level++;
			}
			if (c == '}') {
				level--;
			}
			if (level > 1 || (level == 1 && c == '}')) {
				continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	private static String getVariableName(String match) {
		int colonIdx = match.indexOf(':');
		return (colonIdx != -1 ? match.substring(0, colonIdx) : match);
	}

	private static String getVariableValueAsString(Object variableValue) {
		return (variableValue != null ? variableValue.toString() : "");
	}


	/**
	 * 定义URI模板变量的约定
	 */
	public interface UriTemplateVariables {

		Object SKIP_VALUE = UriTemplateVariables.class;

		/**
		 * 获取给定URI变量名称的值.
		 * 如果值为{@code null}, 则展开空String.
		 * 如果值为{@link #SKIP_VALUE}, 则不扩展URI变量.
		 * 
		 * @param name 变量名称
		 * 
		 * @return 变量值, 可能为{@code null} 或 {@link #SKIP_VALUE}
		 */
		Object getValue(String name);
	}


	/**
	 * 由Map支持的URI模板变量.
	 */
	private static class MapTemplateVariables implements UriTemplateVariables {

		private final Map<String, ?> uriVariables;

		public MapTemplateVariables(Map<String, ?> uriVariables) {
			this.uriVariables = uriVariables;
		}

		@Override
		public Object getValue(String name) {
			if (!this.uriVariables.containsKey(name)) {
				throw new IllegalArgumentException("Map has no value for '" + name + "'");
			}
			return this.uriVariables.get(name);
		}
	}


	/**
	 * 由变量参数数组支持的URI模板变量.
	 */
	private static class VarArgsTemplateVariables implements UriTemplateVariables {

		private final Iterator<Object> valueIterator;

		public VarArgsTemplateVariables(Object... uriVariableValues) {
			this.valueIterator = Arrays.asList(uriVariableValues).iterator();
		}

		@Override
		public Object getValue(String name) {
			if (!this.valueIterator.hasNext()) {
				throw new IllegalArgumentException("Not enough variable values available to expand '" + name + "'");
			}
			return this.valueIterator.next();
		}
	}
}
