package org.springframework.beans.factory.config;

import java.io.IOException;
import java.io.Reader;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.reader.UnicodeReader;

import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * YAML工厂的基类.
 */
public abstract class YamlProcessor {

	private final Log logger = LogFactory.getLog(getClass());

	private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;

	private Resource[] resources = new Resource[0];

	private List<DocumentMatcher> documentMatchers = Collections.emptyList();

	private boolean matchDefault = true;


	/**
	 * 文档匹配器的映射, 允许调用者有选择地仅使用YAML资源中的某些文档.
	 * 在YAML中, 文档由<code>---<code>行分隔, 并且每个文档在匹配之前都转换为属性.
	 * E.g.
	 * <pre class="code">
	 * environment: dev
	 * url: http://dev.bar.com
	 * name: Developer Setup
	 * ---
	 * environment: prod
	 * url:http://foo.bar.com
	 * name: My Cool App
	 * </pre>
	 * when mapped with
	 * <pre class="code">
	 * setDocumentMatchers(properties ->
	 *     ("prod".equals(properties.getProperty("environment")) ? MatchStatus.FOUND : MatchStatus.NOT_FOUND));
	 * </pre>
	 * would end up as
	 * <pre class="code">
	 * environment=prod
	 * url=http://foo.bar.com
	 * name=My Cool App
	 * </pre>
	 */
	public void setDocumentMatchers(DocumentMatcher... matchers) {
		this.documentMatchers = Arrays.asList(matchers);
	}

	/**
	 * 表示匹配所有的文档, 包括{@link #setDocumentMatchers(DocumentMatcher...) 文档匹配器}丢弃的文档仍然匹配.
	 * 默认 {@code true}.
	 */
	public void setMatchDefault(boolean matchDefault) {
		this.matchDefault = matchDefault;
	}

	/**
	 * 用于解析资源的方法.
	 * 每个资源都将转换为Map, 所以这个属性用于决定在这个工厂的最终输出中保留哪些映射条目.
	 * 默认 {@link ResolutionMethod#OVERRIDE}.
	 */
	public void setResolutionMethod(ResolutionMethod resolutionMethod) {
		Assert.notNull(resolutionMethod, "ResolutionMethod must not be null");
		this.resolutionMethod = resolutionMethod;
	}

	/**
	 * 设置要加载的YAML {@link Resource resources}的位置.
	 */
	public void setResources(Resource... resources) {
		this.resources = resources;
	}


	/**
	 * 为子类提供从提供的资源解析Yaml的机会.
	 * 依次解析每个资源, 并根据 {@link #setDocumentMatchers(DocumentMatcher...) matchers}检查内部文档.
	 * 如果文档匹配, 则将其传递给回调, 并将其表示为 Properties.
	 * 根据 {@link #setResolutionMethod(ResolutionMethod)}, 不会解析所有文档.
	 * 
	 * @param callback 一旦找到匹配的文档, 要委托给的回调
	 */
	protected void process(MatchCallback callback) {
		Yaml yaml = createYaml();
		for (Resource resource : this.resources) {
			boolean found = process(callback, yaml, resource);
			if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
				return;
			}
		}
	}

	/**
	 * 创建要使用的{@link Yaml}实例.
	 */
	protected Yaml createYaml() {
		return new Yaml(new StrictMapAppenderConstructor());
	}

	private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
		int count = 0;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading from YAML: " + resource);
			}
			Reader reader = new UnicodeReader(resource.getInputStream());
			try {
				for (Object object : yaml.loadAll(reader)) {
					if (object != null && process(asMap(object), callback)) {
						count++;
						if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
							break;
						}
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Loaded " + count + " document" + (count > 1 ? "s" : "") +
							" from YAML resource: " + resource);
				}
			}
			finally {
				reader.close();
			}
		}
		catch (IOException ex) {
			handleProcessError(resource, ex);
		}
		return (count > 0);
	}

	private void handleProcessError(Resource resource, IOException ex) {
		if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND &&
				this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
			throw new IllegalStateException(ex);
		}
		if (logger.isWarnEnabled()) {
			logger.warn("Could not load map from " + resource + ": " + ex.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object object) {
		// YAML can have numbers as keys
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		if (!(object instanceof Map)) {
			// A document can be a text literal
			result.put("document", object);
			return result;
		}

		Map<Object, Object> map = (Map<Object, Object>) object;
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Map) {
				value = asMap(value);
			}
			Object key = entry.getKey();
			if (key instanceof CharSequence) {
				result.put(key.toString(), value);
			}
			else {
				// It has to be a map key in this case
				result.put("[" + key.toString() + "]", value);
			}
		}
		return result;
	}

	private boolean process(Map<String, Object> map, MatchCallback callback) {
		Properties properties = CollectionFactory.createStringAdaptingProperties();
		properties.putAll(getFlattenedMap(map));

		if (this.documentMatchers.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Merging document (no matchers set): " + map);
			}
			callback.process(properties, map);
			return true;
		}

		MatchStatus result = MatchStatus.ABSTAIN;
		for (DocumentMatcher matcher : this.documentMatchers) {
			MatchStatus match = matcher.matches(properties);
			result = MatchStatus.getMostSpecific(match, result);
			if (match == MatchStatus.FOUND) {
				if (logger.isDebugEnabled()) {
					logger.debug("Matched document with document matcher: " + properties);
				}
				callback.process(properties, map);
				return true;
			}
		}

		if (result == MatchStatus.ABSTAIN && this.matchDefault) {
			if (logger.isDebugEnabled()) {
				logger.debug("Matched document with default matcher: " + map);
			}
			callback.process(properties, map);
			return true;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Unmatched document: " + map);
		}
		return false;
	}

	/**
	 * 返回给定Map的展平版本, 递归地追加嵌套的Map或Collection值.
	 * 结果映射中的条目保留与源相同的顺序.
	 * 当使用来自{@link MatchCallback}的Map调用时, 结果将包含与{@link MatchCallback}属性相同的值.
	 * 
	 * @param source 源map
	 * 
	 * @return 展平的map
	 */
	protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		buildFlattenedMap(result, source, null);
		return result;
	}

	private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			String key = entry.getKey();
			if (StringUtils.hasText(path)) {
				if (key.startsWith("[")) {
					key = path + key;
				}
				else {
					key = path + '.' + key;
				}
			}
			Object value = entry.getValue();
			if (value instanceof String) {
				result.put(key, value);
			}
			else if (value instanceof Map) {
				// Need a compound key
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) value;
				buildFlattenedMap(result, map, key);
			}
			else if (value instanceof Collection) {
				// Need a compound key
				@SuppressWarnings("unchecked")
				Collection<Object> collection = (Collection<Object>) value;
				int count = 0;
				for (Object object : collection) {
					buildFlattenedMap(result,
							Collections.singletonMap("[" + (count++) + "]", object), key);
				}
			}
			else {
				result.put(key, (value != null ? value : ""));
			}
		}
	}


	/**
	 * 用于处理YAML解析结果的回调接口.
	 */
	public interface MatchCallback {

		/**
		 * 处理解析结果的给定代表.
		 * 
		 * @param properties 要处理的属性(作为Map或Collection的索引的Key的展平表示)
		 * @param map 结果map (保留YAML文档中的原始值结构)
		 */
		void process(Properties properties, Map<String, Object> map);
	}


	/**
	 * 用于测试属性是否匹配的策略接口.
	 */
	public interface DocumentMatcher {

		/**
		 * 测试给定属性是否匹配.
		 * 
		 * @param properties 要测试的属性
		 * 
		 * @return 匹配的状态
		 */
		MatchStatus matches(Properties properties);
	}


	/**
	 * {@link DocumentMatcher#matches(java.util.Properties)}返回的状态
	 */
	public enum MatchStatus {

		/**
		 * 匹配.
		 */
		FOUND,

		/**
		 * 不匹配.
		 */
		NOT_FOUND,

		/**
		 * 不应考虑匹配者.
		 */
		ABSTAIN;

		/**
		 * 比较两个 {@link MatchStatus}项, 返回最具体的状态.
		 */
		public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
			return (a.ordinal() < b.ordinal() ? a : b);
		}
	}


	/**
	 * 用于解析资源的方法.
	 */
	public enum ResolutionMethod {

		/**
		 * 替换列表中较早的值.
		 */
		OVERRIDE,

		/**
		 * 替换列表中较早的值, 忽略任何失败.
		 */
		OVERRIDE_AND_IGNORE,

		/**
		 * 获取列表中存在的第一个资源并使用它.
		 */
		FIRST_FOUND
	}


	/**
	 * 一个专门的{@link Constructor}, 用于检查重复的Key.
	 */
	protected static class StrictMapAppenderConstructor extends Constructor {

		// Declared as public for use in subclasses
		public StrictMapAppenderConstructor() {
			super();
		}

		@Override
		protected Map<Object, Object> constructMapping(MappingNode node) {
			try {
				return super.constructMapping(node);
			}
			catch (IllegalStateException ex) {
				throw new ParserException("while parsing MappingNode",
						node.getStartMark(), ex.getMessage(), node.getEndMark());
			}
		}

		@Override
		protected Map<Object, Object> createDefaultMap() {
			final Map<Object, Object> delegate = super.createDefaultMap();
			return new AbstractMap<Object, Object>() {
				@Override
				public Object put(Object key, Object value) {
					if (delegate.containsKey(key)) {
						throw new IllegalStateException("Duplicate key: " + key);
					}
					return delegate.put(key, value);
				}
				@Override
				public Set<Entry<Object, Object>> entrySet() {
					return delegate.entrySet();
				}
			};
		}
	}

}
