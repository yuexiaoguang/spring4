package org.springframework.context.support;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 基于资源包约定的{@code MessageSource}实现的抽象基类,
 * 例如{@link ResourceBundleMessageSource} 和 {@link ReloadableResourceBundleMessageSource}.
 * 提供通用的配置方法和相应的语义定义.
 */
public abstract class AbstractResourceBasedMessageSource extends AbstractMessageSource {

	private final Set<String> basenameSet = new LinkedHashSet<String>(4);

	private String defaultEncoding;

	private boolean fallbackToSystemLocale = true;

	private long cacheMillis = -1;


	/**
	 * 设置单个基本名称, 遵循未指定文件扩展名或语言代码的基本ResourceBundle约定.
	 * 资源位置格式取决于特定的 {@code MessageSource}实现.
	 * <p>支持Regular和XMl属性文件: 
	 * e.g. "messages" 会找到 "messages.properties", "messages_en.properties"等, 以及"messages.xml", "messages_en.xml"等.
	 * 
	 * @param basename 单个basename
	 */
	public void setBasename(String basename) {
		setBasenames(basename);
	}

	/**
	 * 设置一个基本名称数组, 每个基本名称遵循未指定文件扩展名或语言代码的基本ResourceBundle约定.
	 * 资源位置格式取决于特定的{@code MessageSource}实现.
	 * <p>支持Regular和XMl属性文件:
	 * e.g. "messages"会找到 "messages.properties", "messages_en.properties"等, 以及"messages.xml", "messages_en.xml"等.
	 * <p>在解析消息代码时, 将依次检查关联的资源包.
	 * 请注意, 由于顺序查找, <i>上一个</i>资源包中的消息定义将覆盖后一个包中的消息定义.
	 * <p>Note: 与{@link #addBasenames}相反, 这将替换具有给定名称的现有条目, 因此也可用于重置配置.
	 * 
	 * @param basenames 基本名称数组
	 */
	public void setBasenames(String... basenames) {
		this.basenameSet.clear();
		addBasenames(basenames);
	}

	/**
	 * 将指定的基本名称添加到现有的basename配置中.
	 * <p>Note: 如果给定的基本名称已存在, 则其条目的位置将保留原始集合中的位置.
	 * 将在列表末尾添加新条目, 以便在现有基本名称之后进行搜索.
	 */
	public void addBasenames(String... basenames) {
		if (!ObjectUtils.isEmpty(basenames)) {
			for (String basename : basenames) {
				Assert.hasText(basename, "Basename must not be empty");
				this.basenameSet.add(basename.trim());
			}
		}
	}

	/**
	 * 返回此{@code MessageSource}的基本名称集合, 包含按注册顺序的条目.
	 * <p>调用代码可以内省此集合, 并添加或删除条目.
	 */
	public Set<String> getBasenameSet() {
		return this.basenameSet;
	}

	/**
	 * 设置用于解析属性文件的默认字符集.
	 * 如果没有为文件指定特定于文件的字符集, 则使用此选项.
	 * <p>默认无, 使用{@code java.util.Properties}默认编码: ISO-8859-1.
	 * <p>仅适用于经典属性文件, 而不适用于XML文件.
	 * 
	 * @param defaultEncoding 默认字符集
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 返回用于解析属性文件的默认字符集.
	 */
	protected String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 在找不到特定区域设置的文件时, 设置是否回退到系统区域设置.
	 * 默认 "true"; 如果关闭此选项, 则唯一的回退将是默认文件 (e.g. "messages.properties" 用于基本名称 "messages").
	 * <p>回退到系统Locale是{@code java.util.ResourceBundle}的默认行为.
	 * 但是, 在应用程序服务器环境中, 这通常是不可取的, 因为系统区域设置根本与应用程序无关: 在这种情况下将此标志设置为 "false".
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	/**
	 * 如果找不到特定区域设置的文件, 是否回退到系统区域设置.
	 */
	protected boolean isFallbackToSystemLocale() {
		return this.fallbackToSystemLocale;
	}

	/**
	 * 设置缓存已加载的属性文件的秒数.
	 * <ul>
	 * <li>默认 "-1", 表示永远缓存 (就像 {@code java.util.ResourceBundle}).
	 * <li>正数, 将缓存加载的属性文件达给定的秒数.
	 * 这实际上是刷新检查之间的间隔.
	 * 请注意, 刷新尝试将在实际重新加载之前首先检查文件的上次修改时间戳;
	 * 因此, 如果文件没有更改, 则可以将此间隔设置得相当低, 因为刷新尝试实际上不会重新加载.
	 * <li>值“0”将检查每次消息访问时, 文件的最后修改时间戳. <b>不要在生产环境中使用它!</b>
	 * </ul>
	 * <p><b>请注意, 根据您的ClassLoader, 过期可能无法可靠地工作, 因为ClassLoader可能会保留捆绑文件的缓存版本.</b>
	 * 在这种情况下, 首选{@link ReloadableResourceBundleMessageSource}优先于{@link ResourceBundleMessageSource}并结合非类路径位置.
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheMillis = (cacheSeconds * 1000);
	}

	/**
	 * 设置缓存已加载的属性文件的毫秒数.
	 * 请注意, 通常设置秒数: {@link #setCacheSeconds}.
	 * <ul>
	 * <li>默认 "-1", 表示永远缓存 (就像 {@code java.util.ResourceBundle}).
	 * <li>正数, 将缓存加载的属性文件达给定的毫秒数.
	 * 这实际上是刷新检查之间的间隔.
	 * 请注意, 刷新尝试将在实际重新加载之前首先检查文件的上次修改时间戳;
	 * 因此, 如果文件没有更改, 则可以将此间隔设置得相当低, 因为刷新尝试实际上不会重新加载.
	 * <li>值“0”将检查每次消息访问时, 文件的最后修改时间戳. <b>不要在生产环境中使用它!</b>
	 * </ul>
	 */
	public void setCacheMillis(long cacheMillis) {
		this.cacheMillis = cacheMillis;
	}

	/**
	 * 返回缓存已加载的属性文件的毫秒数.
	 */
	protected long getCacheMillis() {
		return this.cacheMillis;
	}

}
