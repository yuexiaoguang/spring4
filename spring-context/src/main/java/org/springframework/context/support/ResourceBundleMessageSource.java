package org.springframework.context.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.context.MessageSource}实现, 使用指定的基本名称访问资源包的.
 * 这个类依赖于底层JDK的 {@link java.util.ResourceBundle}实现, 结合{@link java.text.MessageFormat}提供的JDK标准消息解析.
 *
 * <p>此MessageSource为每个消息缓存访问的ResourceBundle实例和生成的MessageFormats.
 * 它还实现了没有MessageFormat的无arg消息的呈现, 这是AbstractMessageSource基类所支持的.
 * 此MessageSource提供的缓存明显快于{@code java.util.ResourceBundle}类的内置缓存.
 *
 * <p>基本名称遵循 {@link java.util.ResourceBundle}约定:
 * 基本上, 一个完全限定的类路径位置.
 * 如果它不包含包限定符 (例如{@code org.mypackage}), 它将从类路径根解析.
 * 请注意, JDK的标准ResourceBundle将点视为包分隔符:
 * 这意味着 "test.theme" 实际上等同于 "test/theme".
 */
public class ResourceBundleMessageSource extends AbstractResourceBasedMessageSource implements BeanClassLoaderAware {

	private ClassLoader bundleClassLoader;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * 保存已加载的ResourceBundle的缓存.
	 * 此Map使用bundle basename作为Key.
	 * 这允许非常有效的哈希查找, 比ResourceBundle类自己的缓存快得多.
	 */
	private final Map<String, Map<Locale, ResourceBundle>> cachedResourceBundles =
			new HashMap<String, Map<Locale, ResourceBundle>>();

	/**
	 * 保存已生成的MessageFormats的缓存.
	 * 允许非常有效的散列查找, 而没有连接Key.
	 */
	private final Map<ResourceBundle, Map<String, Map<Locale, MessageFormat>>> cachedBundleMessageFormats =
			new HashMap<ResourceBundle, Map<String, Map<Locale, MessageFormat>>>();


	/**
	 * 设置用于加载资源包的ClassLoader.
	 * <p>默认是包含BeanFactory的 {@link org.springframework.beans.factory.BeanClassLoaderAware bean ClassLoader},
	 * 或者如果没有在BeanFactory中运行, 则由{@link org.springframework.util.ClassUtils#getDefaultClassLoader()}确定的默认ClassLoader.
	 */
	public void setBundleClassLoader(ClassLoader classLoader) {
		this.bundleClassLoader = classLoader;
	}

	/**
	 * 返回用于加载资源包的ClassLoader.
	 * <p>默认是包含BeanFactory的bean ClassLoader.
	 */
	protected ClassLoader getBundleClassLoader() {
		return (this.bundleClassLoader != null ? this.bundleClassLoader : this.beanClassLoader);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}


	/**
	 * 将给定的消息代码解析为已注册的资源包中的键, 并按原样返回包中找到的值 (不使用MessageFormat解析).
	 */
	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		Set<String> basenames = getBasenameSet();
		for (String basename : basenames) {
			ResourceBundle bundle = getResourceBundle(basename, locale);
			if (bundle != null) {
				String result = getStringOrNull(bundle, code);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * 使用每个消息代码的缓存的MessageFormat实例, 将给定的消息代码解析为已注册的资源包中的键.
	 */
	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
		Set<String> basenames = getBasenameSet();
		for (String basename : basenames) {
			ResourceBundle bundle = getResourceBundle(basename, locale);
			if (bundle != null) {
				MessageFormat messageFormat = getMessageFormat(bundle, code, locale);
				if (messageFormat != null) {
					return messageFormat;
				}
			}
		}
		return null;
	}


	/**
	 * 返回给定基本名称和代码的ResourceBundle, 从缓存中获取已生成的MessageFormats.
	 * 
	 * @param basename ResourceBundle的基础名称
	 * @param locale 用于查找ResourceBundle的Locale
	 * 
	 * @return 生成的ResourceBundle; 如果找不到给定的basename和Locale, 则为{@code null}
	 */
	protected ResourceBundle getResourceBundle(String basename, Locale locale) {
		if (getCacheMillis() >= 0) {
			// 新的ResourceBundle.getBundle调用, 以便让ResourceBundle执行其本机缓存, 但代价是更广泛的查找步骤.
			return doGetBundle(basename, locale);
		}
		else {
			// 永远缓存:更喜欢区域设置缓存, 而不是重复的getBundle调用.
			synchronized (this.cachedResourceBundles) {
				Map<Locale, ResourceBundle> localeMap = this.cachedResourceBundles.get(basename);
				if (localeMap != null) {
					ResourceBundle bundle = localeMap.get(locale);
					if (bundle != null) {
						return bundle;
					}
				}
				try {
					ResourceBundle bundle = doGetBundle(basename, locale);
					if (localeMap == null) {
						localeMap = new HashMap<Locale, ResourceBundle>();
						this.cachedResourceBundles.put(basename, localeMap);
					}
					localeMap.put(locale, bundle);
					return bundle;
				}
				catch (MissingResourceException ex) {
					if (logger.isWarnEnabled()) {
						logger.warn("ResourceBundle [" + basename + "] not found for MessageSource: " + ex.getMessage());
					}
					// 假设未找到包  -> 不要抛出异常以允许检查父消息源.
					return null;
				}
			}
		}
	}

	/**
	 * 获取给定basename和Locale的资源包.
	 * 
	 * @param basename 要查找的基本名称
	 * @param locale 要查找的区域设置
	 * 
	 * @return 相应的ResourceBundle
	 * @throws MissingResourceException 如果找不到匹配的包
	 */
	protected ResourceBundle doGetBundle(String basename, Locale locale) throws MissingResourceException {
		return ResourceBundle.getBundle(basename, locale, getBundleClassLoader(), new MessageSourceControl());
	}

	/**
	 * 从给定的读取器加载基于属性的资源包.
	 * <p>默认实现返回 {@link PropertyResourceBundle}.
	 * 
	 * @param reader 目标源的读取器
	 * 
	 * @return 完全加载的包
	 * @throws IOException 发送I/O 错误
	 */
	protected ResourceBundle loadBundle(Reader reader) throws IOException {
		return new PropertyResourceBundle(reader);
	}

	/**
	 * 返回给定包和代码的MessageFormat, 从缓存中获取已生成的MessageFormats.
	 * 
	 * @param bundle 要处理的ResourceBundle
	 * @param code 要检索的消息代码
	 * @param locale 用于构建MessageFormat的Locale
	 * 
	 * @return 生成的 MessageFormat; 如果没有为给定的代码定义消息, 则为{@code null}
	 * @throws MissingResourceException 如果被ResourceBundle抛出
	 */
	protected MessageFormat getMessageFormat(ResourceBundle bundle, String code, Locale locale)
			throws MissingResourceException {

		synchronized (this.cachedBundleMessageFormats) {
			Map<String, Map<Locale, MessageFormat>> codeMap = this.cachedBundleMessageFormats.get(bundle);
			Map<Locale, MessageFormat> localeMap = null;
			if (codeMap != null) {
				localeMap = codeMap.get(code);
				if (localeMap != null) {
					MessageFormat result = localeMap.get(locale);
					if (result != null) {
						return result;
					}
				}
			}

			String msg = getStringOrNull(bundle, code);
			if (msg != null) {
				if (codeMap == null) {
					codeMap = new HashMap<String, Map<Locale, MessageFormat>>();
					this.cachedBundleMessageFormats.put(bundle, codeMap);
				}
				if (localeMap == null) {
					localeMap = new HashMap<Locale, MessageFormat>();
					codeMap.put(code, localeMap);
				}
				MessageFormat result = createMessageFormat(msg, locale);
				localeMap.put(locale, result);
				return result;
			}

			return null;
		}
	}

	/**
	 * 有效地检索指定键的String值, 如果未找到则返回{@code null}.
	 * <p>从4.2开始, 默认实现在尝试调用{@code getString}之前检查{@code containsKey}
	 * (需要在找不到Key时, 捕获{@code MissingResourceException}).
	 * <p>可以在子类中重写.
	 * 
	 * @param bundle 用于执行查找的ResourceBundle
	 * @param key 要查找的key
	 * 
	 * @return 关联的值, 或{@code null}
	 */
	protected String getStringOrNull(ResourceBundle bundle, String key) {
		if (bundle.containsKey(key)) {
			try {
				return bundle.getString(key);
			}
			catch (MissingResourceException ex){
				// 假设由于某些其他原因未找到Key -> 不要抛出异常以允许检查父级消息源.
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": basenames=" + getBasenameSet();
	}


	/**
	 * 自定义实现Java 6的 {@code ResourceBundle.Control}, 添加对自定义文件编码的支持,
	 * 停用系统区域设置的回退并激活ResourceBundle的本机缓存.
	 */
	private class MessageSourceControl extends ResourceBundle.Control {

		@Override
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
				throws IllegalAccessException, InstantiationException, IOException {

			// 默认编码的特殊处理
			if (format.equals("java.properties")) {
				String bundleName = toBundleName(baseName, locale);
				final String resourceName = toResourceName(bundleName, "properties");
				final ClassLoader classLoader = loader;
				final boolean reloadFlag = reload;
				InputStream stream;
				try {
					stream = AccessController.doPrivileged(
							new PrivilegedExceptionAction<InputStream>() {
								@Override
								public InputStream run() throws IOException {
									InputStream is = null;
									if (reloadFlag) {
										URL url = classLoader.getResource(resourceName);
										if (url != null) {
											URLConnection connection = url.openConnection();
											if (connection != null) {
												connection.setUseCaches(false);
												is = connection.getInputStream();
											}
										}
									}
									else {
										is = classLoader.getResourceAsStream(resourceName);
									}
									return is;
								}
							});
				}
				catch (PrivilegedActionException ex) {
					throw (IOException) ex.getException();
				}
				if (stream != null) {
					String encoding = getDefaultEncoding();
					if (encoding == null) {
						encoding = "ISO-8859-1";
					}
					try {
						return loadBundle(new InputStreamReader(stream, encoding));
					}
					finally {
						stream.close();
					}
				}
				else {
					return null;
				}
			}
			else {
				// 将"java.class"格式的处理委托给标准Control
				return super.newBundle(baseName, locale, format, loader, reload);
			}
		}

		@Override
		public Locale getFallbackLocale(String baseName, Locale locale) {
			return (isFallbackToSystemLocale() ? super.getFallbackLocale(baseName, locale) : null);
		}

		@Override
		public long getTimeToLive(String baseName, Locale locale) {
			long cacheMillis = getCacheMillis();
			return (cacheMillis >= 0 ? cacheMillis : super.getTimeToLive(baseName, locale));
		}

		@Override
		public boolean needsReload(
				String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {

			if (super.needsReload(baseName, locale, format, loader, bundle, loadTime)) {
				synchronized (cachedBundleMessageFormats) {
					cachedBundleMessageFormats.remove(bundle);
				}
				return true;
			}
			else {
				return false;
			}
		}
	}
}
