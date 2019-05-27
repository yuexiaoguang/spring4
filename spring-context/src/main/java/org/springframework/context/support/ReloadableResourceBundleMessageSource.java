package org.springframework.context.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

/**
 * 特定于Spring的{@link org.springframework.context.MessageSource}实现,
 * 它使用指定的基本名称访问资源包, 参与Spring {@link org.springframework.context.ApplicationContext}的资源加载.
 *
 * <p>与基于JDK的{@link ResourceBundleMessageSource}相比, 此类使用 {@link java.util.Properties}实例作为其消息的自定义数据结构,
 * 通过Spring {@link Resource}句柄的 {@link org.springframework.util.PropertiesPersister}策略加载它们.
 * 此策略不仅能够根据时间戳更改重新加载文件, 还能够加载具有特定字符编码的属性文件.
 * 它还将检测XML属性文件.
 *
 * <p>请注意, 设置为 {@link #setBasenames "basenames"}属性的基本名称的处理方式
 * 与{@link ResourceBundleMessageSource}的 "basenames"属性略有不同.
 * 它遵循基本的ResourceBundle规则, 不指定文件扩展名或语言代码, 但可以引用任何Spring资源位置 (而不是限制为类路径资源).
 * 使用 "classpath:"前缀, 仍然可以从类路径加载资源, 但"cacheSeconds"除 "-1"之外的值 (永远缓存) 在这种情况下可能无法可靠地工作.
 *
 * <p>对于典型的Web应用程序, 可以将消息文件放在{@code WEB-INF}中:
 * e.g. "WEB-INF/messages" 基本名称将找到 "WEB-INF/messages.properties", "WEB-INF/messages_en.properties"等,
 * 以及"WEB-INF/messages.xml", "WEB-INF/messages_en.xml"等.
 * 请注意, 由于顺序查找, <i>上一个</i> 资源包中的消息定义将覆盖后一个包中的消息定义.

 * <p>此MessageSource可以在 {@link org.springframework.context.ApplicationContext}之外轻松使用:
 * 它将使用 {@link org.springframework.core.io.DefaultResourceLoader}作为默认值,
 * 如果在上下文中运行, 只需使用ApplicationContext的资源加载器进行覆盖.
 * 它没有任何其他特定的依赖项.
 *
 * <p>感谢Thomas Achleitner提供此消息源的初始实现!
 */
public class ReloadableResourceBundleMessageSource extends AbstractResourceBasedMessageSource
		implements ResourceLoaderAware {

	private static final String PROPERTIES_SUFFIX = ".properties";

	private static final String XML_SUFFIX = ".xml";


	private Properties fileEncodings;

	private boolean concurrentRefresh = true;

	private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	// 保存每个Locale的文件名列表的缓存
	private final ConcurrentMap<String, Map<Locale, List<String>>> cachedFilenames =
			new ConcurrentHashMap<String, Map<Locale, List<String>>>();

	// 保存每个文件名已加载的属性的缓存
	private final ConcurrentMap<String, PropertiesHolder> cachedProperties =
			new ConcurrentHashMap<String, PropertiesHolder>();

	// 保存每个区域设置的合并的已加载的属性的缓存
	private final ConcurrentMap<Locale, PropertiesHolder> cachedMergedProperties =
			new ConcurrentHashMap<Locale, PropertiesHolder>();


	/**
	 * 设置每个文件的字符集以用于解析属性文件.
	 * <p>仅适用于经典属性文件, 而不适用于XML文件.
	 * 
	 * @param fileEncodings 将文件名作为键, 将字符集名称作为值的属性.
	 * 文件名必须与basename语法匹配, 并具有可选的特定于区域配置的组件:
	 * e.g. "WEB-INF/messages" 或 "WEB-INF/messages_en".
	 */
	public void setFileEncodings(Properties fileEncodings) {
		this.fileEncodings = fileEncodings;
	}

	/**
	 * 指定是否允许并发刷新行为,
	 * i.e. 一个线程在特定的缓存的属性文件的刷新尝试中被锁定, 其他线程暂时返回旧属性, 直到刷新尝试完成.
	 * <p>默认 "true": 从Spring Framework 4.1开始, 这种行为是新的, 可以最大限度地减少线程之间的争用.
	 * 如果您更倾向于旧的行为, i.e. 在刷新时完全阻塞, 切换为 "false".
	 */
	public void setConcurrentRefresh(boolean concurrentRefresh) {
		this.concurrentRefresh = concurrentRefresh;
	}

	/**
	 * 设置用于解析属性文件的PropertiesPersister.
	 * <p>默认是 DefaultPropertiesPersister.
	 */
	public void setPropertiesPersister(PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : new DefaultPropertiesPersister());
	}

	/**
	 * 设置用于加载包属性文件的ResourceLoader.
	 * <p>默认是 DefaultResourceLoader.
	 * 如果在上下文中运行, 将被ApplicationContext覆盖, 因为它实现了ResourceLoaderAware接口.
	 * 在ApplicationContext的外部运行时可以手动覆盖.
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
	}


	/**
	 * 将给定的消息代码解析为检索到的包文件中的Key, 按原样返回包中找到的值 (不使用MessageFormat解析).
	 */
	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		if (getCacheMillis() < 0) {
			PropertiesHolder propHolder = getMergedProperties(locale);
			String result = propHolder.getProperty(code);
			if (result != null) {
				return result;
			}
		}
		else {
			for (String basename : getBasenameSet()) {
				List<String> filenames = calculateAllFilenames(basename, locale);
				for (String filename : filenames) {
					PropertiesHolder propHolder = getProperties(filename);
					String result = propHolder.getProperty(code);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 使用每个消息代码缓存的MessageFormat实例, 将给定的消息代码解析为检索到的包文件中的Key.
	 */
	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
		if (getCacheMillis() < 0) {
			PropertiesHolder propHolder = getMergedProperties(locale);
			MessageFormat result = propHolder.getMessageFormat(code, locale);
			if (result != null) {
				return result;
			}
		}
		else {
			for (String basename : getBasenameSet()) {
				List<String> filenames = calculateAllFilenames(basename, locale);
				for (String filename : filenames) {
					PropertiesHolder propHolder = getProperties(filename);
					MessageFormat result = propHolder.getMessageFormat(code, locale);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}


	/**
	 * 在合并所有指定的资源包之后, 获取包含Locale的实际可见属性的PropertiesHolder.
	 * 从缓存中提取持有者, 或新加载它.
	 * <p>仅在永久缓存资源包内容时使用, i.e. 使用 cacheSeconds < 0.
	 * 因此, 合并的属性始终永远缓存.
	 */
	protected PropertiesHolder getMergedProperties(Locale locale) {
		PropertiesHolder mergedHolder = this.cachedMergedProperties.get(locale);
		if (mergedHolder != null) {
			return mergedHolder;
		}
		Properties mergedProps = newProperties();
		long latestTimestamp = -1;
		String[] basenames = StringUtils.toStringArray(getBasenameSet());
		for (int i = basenames.length - 1; i >= 0; i--) {
			List<String> filenames = calculateAllFilenames(basenames[i], locale);
			for (int j = filenames.size() - 1; j >= 0; j--) {
				String filename = filenames.get(j);
				PropertiesHolder propHolder = getProperties(filename);
				if (propHolder.getProperties() != null) {
					mergedProps.putAll(propHolder.getProperties());
					if (propHolder.getFileTimestamp() > latestTimestamp) {
						latestTimestamp = propHolder.getFileTimestamp();
					}
				}
			}
		}
		mergedHolder = new PropertiesHolder(mergedProps, latestTimestamp);
		PropertiesHolder existing = this.cachedMergedProperties.putIfAbsent(locale, mergedHolder);
		if (existing != null) {
			mergedHolder = existing;
		}
		return mergedHolder;
	}

	/**
	 * 计算给定bundle basename和Locale的所有文件名.
	 * 将计算给定区域设置, 系统区域设置, 默认文件的文件名.
	 * 
	 * @param basename 包的基本名称
	 * @param locale 区域设置
	 * 
	 * @return 要检查的文件名列表
	 */
	protected List<String> calculateAllFilenames(String basename, Locale locale) {
		Map<Locale, List<String>> localeMap = this.cachedFilenames.get(basename);
		if (localeMap != null) {
			List<String> filenames = localeMap.get(locale);
			if (filenames != null) {
				return filenames;
			}
		}
		List<String> filenames = new ArrayList<String>(7);
		filenames.addAll(calculateFilenamesForLocale(basename, locale));
		if (isFallbackToSystemLocale() && !locale.equals(Locale.getDefault())) {
			List<String> fallbackFilenames = calculateFilenamesForLocale(basename, Locale.getDefault());
			for (String fallbackFilename : fallbackFilenames) {
				if (!filenames.contains(fallbackFilename)) {
					// 不在文件名列表中的回退区域设置的条目.
					filenames.add(fallbackFilename);
				}
			}
		}
		filenames.add(basename);
		if (localeMap == null) {
			localeMap = new ConcurrentHashMap<Locale, List<String>>();
			Map<Locale, List<String>> existing = this.cachedFilenames.putIfAbsent(basename, localeMap);
			if (existing != null) {
				localeMap = existing;
			}
		}
		localeMap.put(locale, filenames);
		return filenames;
	}

	/**
	 * 计算给定包基本名称和区域设置的文件名, 附加语言代码, 国家/地区代码和变体代码.
	 * E.g.: basename "messages", Locale "de_AT_oo" -> "messages_de_AT_OO", "messages_de_AT", "messages_de".
	 * <p>遵循 {@link java.util.Locale#toString()}定义的规则.
	 * 
	 * @param basename 包的基本名称
	 * @param locale 区域设置
	 * 
	 * @return 要检查的文件名列表
	 */
	protected List<String> calculateFilenamesForLocale(String basename, Locale locale) {
		List<String> result = new ArrayList<String>(3);
		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();
		StringBuilder temp = new StringBuilder(basename);

		temp.append('_');
		if (language.length() > 0) {
			temp.append(language);
			result.add(0, temp.toString());
		}

		temp.append('_');
		if (country.length() > 0) {
			temp.append(country);
			result.add(0, temp.toString());
		}

		if (variant.length() > 0 && (language.length() > 0 || country.length() > 0)) {
			temp.append('_').append(variant);
			result.add(0, temp.toString());
		}

		return result;
	}


	/**
	 * 从缓存或新加载的, 获取给定文件名的PropertiesHolder.
	 * 
	 * @param filename 包文件名 (basename + Locale)
	 * 
	 * @return 包的当前PropertiesHolder
	 */
	protected PropertiesHolder getProperties(String filename) {
		PropertiesHolder propHolder = this.cachedProperties.get(filename);
		long originalTimestamp = -2;

		if (propHolder != null) {
			originalTimestamp = propHolder.getRefreshTimestamp();
			if (originalTimestamp == -1 || originalTimestamp > System.currentTimeMillis() - getCacheMillis()) {
				// Up to date
				return propHolder;
			}
		}
		else {
			propHolder = new PropertiesHolder();
			PropertiesHolder existingHolder = this.cachedProperties.putIfAbsent(filename, propHolder);
			if (existingHolder != null) {
				propHolder = existingHolder;
			}
		}

		// 在这里需要刷新...
		if (this.concurrentRefresh && propHolder.getRefreshTimestamp() >= 0) {
			// 一个填充的但陈旧的持有者 -> 可以继续使用它.
			if (!propHolder.refreshLock.tryLock()) {
				// 已经被另一个线程刷新了 -> 暂时返回现有的属性.
				return propHolder;
			}
		}
		else {
			propHolder.refreshLock.lock();
		}
		try {
			PropertiesHolder existingHolder = this.cachedProperties.get(filename);
			if (existingHolder != null && existingHolder.getRefreshTimestamp() > originalTimestamp) {
				return existingHolder;
			}
			return refreshProperties(filename, propHolder);
		}
		finally {
			propHolder.refreshLock.unlock();
		}
	}

	/**
	 * 刷新给定包文件名的PropertiesHolder.
	 * 如果之前没有缓存, 则持有者可以是{@code null}, 或者是超时缓存条目
	 * (可能会针对当前上次修改的时间戳重新进行验证).
	 * 
	 * @param filename 包文件名 (basename + Locale)
	 * @param propHolder 包的当前PropertiesHolder
	 */
	protected PropertiesHolder refreshProperties(String filename, PropertiesHolder propHolder) {
		long refreshTimestamp = (getCacheMillis() < 0 ? -1 : System.currentTimeMillis());

		Resource resource = this.resourceLoader.getResource(filename + PROPERTIES_SUFFIX);
		if (!resource.exists()) {
			resource = this.resourceLoader.getResource(filename + XML_SUFFIX);
		}

		if (resource.exists()) {
			long fileTimestamp = -1;
			if (getCacheMillis() >= 0) {
				// 如果缓存超时, 则只读取文件的上次修改时间戳.
				try {
					fileTimestamp = resource.lastModified();
					if (propHolder != null && propHolder.getFileTimestamp() == fileTimestamp) {
						if (logger.isDebugEnabled()) {
							logger.debug("Re-caching properties for filename [" + filename + "] - file hasn't been modified");
						}
						propHolder.setRefreshTimestamp(refreshTimestamp);
						return propHolder;
					}
				}
				catch (IOException ex) {
					// 可能是类路径资源: 永远缓存它.
					if (logger.isDebugEnabled()) {
						logger.debug(resource + " could not be resolved in the file system - assuming that it hasn't changed", ex);
					}
					fileTimestamp = -1;
				}
			}
			try {
				Properties props = loadProperties(resource, filename);
				propHolder = new PropertiesHolder(props, fileTimestamp);
			}
			catch (IOException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not parse properties file [" + resource.getFilename() + "]", ex);
				}
				// 空持有者表示 "没有有效的".
				propHolder = new PropertiesHolder();
			}
		}

		else {
			// 资源不存在.
			if (logger.isDebugEnabled()) {
				logger.debug("No properties file found for [" + filename + "] - neither plain properties nor XML");
			}
			// 空持有者表示 "未找到".
			propHolder = new PropertiesHolder();
		}

		propHolder.setRefreshTimestamp(refreshTimestamp);
		this.cachedProperties.put(filename, propHolder);
		return propHolder;
	}

	/**
	 * 从给定资源加载属性.
	 * 
	 * @param resource 要加载的资源
	 * @param filename原始包文件名 (basename + Locale)
	 * 
	 * @return 已填充的Properties实例
	 * @throws IOException 如果属性加载失败
	 */
	protected Properties loadProperties(Resource resource, String filename) throws IOException {
		InputStream is = resource.getInputStream();
		Properties props = newProperties();
		try {
			if (resource.getFilename().endsWith(XML_SUFFIX)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Loading properties [" + resource.getFilename() + "]");
				}
				this.propertiesPersister.loadFromXml(props, is);
			}
			else {
				String encoding = null;
				if (this.fileEncodings != null) {
					encoding = this.fileEncodings.getProperty(filename);
				}
				if (encoding == null) {
					encoding = getDefaultEncoding();
				}
				if (encoding != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Loading properties [" + resource.getFilename() + "] with encoding '" + encoding + "'");
					}
					this.propertiesPersister.load(props, new InputStreamReader(is, encoding));
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Loading properties [" + resource.getFilename() + "]");
					}
					this.propertiesPersister.load(props, is);
				}
			}
			return props;
		}
		finally {
			is.close();
		}
	}

	/**
	 * 用于创建普通的新的{@link Properties}实例的模板方法.
	 * 默认实现简单调用 {@link Properties#Properties()}.
	 * <p>允许在子类中返回自定义 {@link Properties}扩展.
	 * 重写方法应该只是实例化一个自定义的 {@link Properties}子类, 不需要进一步初始化或在那里执行填充.
	 * 
	 * @return 一个普通的Properties实例
	 */
	protected Properties newProperties() {
		return new Properties();
	}


	/**
	 * 清除资源包缓存.
	 * 后续的解析调用将导致重新加载属性文件.
	 */
	public void clearCache() {
		logger.debug("Clearing entire resource bundle cache");
		this.cachedProperties.clear();
		this.cachedMergedProperties.clear();
	}

	/**
	 * 清除此MessageSource及其所有祖先的资源包缓存.
	 */
	public void clearCacheIncludingAncestors() {
		clearCache();
		if (getParentMessageSource() instanceof ReloadableResourceBundleMessageSource) {
			((ReloadableResourceBundleMessageSource) getParentMessageSource()).clearCacheIncludingAncestors();
		}
	}


	@Override
	public String toString() {
		return getClass().getName() + ": basenames=" + getBasenameSet();
	}


	/**
	 * 用于缓存的PropertiesHolder.
	 * 存储源文件的上次修改时间戳, 以进行有效的更改检测, 以及上次刷新尝试的时间戳
	 * (每次重新验证缓存条目时都会更新).
	 */
	protected class PropertiesHolder {

		private final Properties properties;

		private final long fileTimestamp;

		private volatile long refreshTimestamp = -2;

		private final ReentrantLock refreshLock = new ReentrantLock();

		/** 保存每个消息代码已生成的MessageFormats的缓存 */
		private final ConcurrentMap<String, Map<Locale, MessageFormat>> cachedMessageFormats =
				new ConcurrentHashMap<String, Map<Locale, MessageFormat>>();

		public PropertiesHolder() {
			this.properties = null;
			this.fileTimestamp = -1;
		}

		public PropertiesHolder(Properties properties, long fileTimestamp) {
			this.properties = properties;
			this.fileTimestamp = fileTimestamp;
		}

		public Properties getProperties() {
			return this.properties;
		}

		public long getFileTimestamp() {
			return this.fileTimestamp;
		}

		public void setRefreshTimestamp(long refreshTimestamp) {
			this.refreshTimestamp = refreshTimestamp;
		}

		public long getRefreshTimestamp() {
			return this.refreshTimestamp;
		}

		public String getProperty(String code) {
			if (this.properties == null) {
				return null;
			}
			return this.properties.getProperty(code);
		}

		public MessageFormat getMessageFormat(String code, Locale locale) {
			if (this.properties == null) {
				return null;
			}
			Map<Locale, MessageFormat> localeMap = this.cachedMessageFormats.get(code);
			if (localeMap != null) {
				MessageFormat result = localeMap.get(locale);
				if (result != null) {
					return result;
				}
			}
			String msg = this.properties.getProperty(code);
			if (msg != null) {
				if (localeMap == null) {
					localeMap = new ConcurrentHashMap<Locale, MessageFormat>();
					Map<Locale, MessageFormat> existing = this.cachedMessageFormats.putIfAbsent(code, localeMap);
					if (existing != null) {
						localeMap = existing;
					}
				}
				MessageFormat result = createMessageFormat(msg, locale);
				localeMap.put(locale, result);
				return result;
			}
			return null;
		}
	}

}
