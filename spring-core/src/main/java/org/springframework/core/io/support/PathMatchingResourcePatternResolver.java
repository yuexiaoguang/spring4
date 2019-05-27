package org.springframework.core.io.support;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.VfsResource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ResourcePatternResolver}实现, 它能够将指定的资源位置路径解析为一个或多个匹配的资源.
 * 源路径可以是一个简单的路径, 它与目标{@link org.springframework.core.io.Resource}具有一对一的映射关系,
 * 或者可能包含特殊的"{@code classpath*:}"前缀, 和/或内部Ant风格的正则表达式
 * (使用Spring的{@link org.springframework.util.AntPathMatcher}工具进行匹配).
 * 后者都是有效的通配符.
 *
 * <p><b>没有通配符:</b>
 *
 * <p>在简单的情况下, 如果指定的位置路径不以{@code "classpath*:}"前缀开头, 并且不包含PathMatcher模式,
 * 则此解析器只需通过底层{@code ResourceLoader}上的{@code getResource()}调用返回单个资源.
 * 示例是真实的URL, 例如"{@code file:C:/context.xml}", 伪URL, 例如"{@code classpath:/context.xml}",
 * 以及简单的无前缀路径, 例如"{@code /WEB-INF/context.xml}".
 * 后者将以特定于底层{@code ResourceLoader}的方式解析 (e.g. {@code ServletContextResource}用于{@code WebApplicationContext}).
 *
 * <p><b>Ant风格的模式:</b>
 *
 * <p>当路径位置包含Ant样式模式时, e.g.:
 * <pre class="code">
 * /WEB-INF/*-context.xml
 * com/mycompany/**&#47;applicationContext.xml
 * file:C:/some/path/*-context.xml
 * classpath:com/mycompany/**&#47;applicationContext.xml</pre>
 * 解析器遵循一个更复杂但已定义的过程来尝试解析通配符.
 * 它为直到最后一个非通配符段的路径生成{@code Resource}, 并从中获取{@code URL}.
 * 如果此URL不是"{@code jar:}" URL 或特定于容器的变体 (e.g. WebLogic中的"{@code zip:}", WebSphere中的"{@code wsjar}", etc.),
 * 然后从中获取{@code java.io.File}, 并通过遍历文件系统来解析通配符.
 * 对于jar URL, 解析器从它获取{@code java.net.JarURLConnection}, 或者手动解析jar URL, 然后遍历jar文件的内容, 以解析通配符.
 *
 * <p><b>对可移植性的影响:</b>
 *
 * <p>如果指定的路径已经是文件URL(显式或隐式), 因为基础{@code ResourceLoader}是文件系统的,
 * 那么通配符保证以完全可移植的方式工作.
 *
 * <p>如果指定的路径是类路径位置, 则解析器必须通过{@code Classloader.getResource()}调用获取最后一个非通配符路径段URL.
 * 由于这只是路径的一个节点 (不是最后的文件), 因此实际上未定义(在ClassLoader Javadocs中), 在这种情况下返回的URL究竟是什么类型.
 * 在实践中, 通常是{@code java.io.File}表示目录, 其中类路径资源解析为文件系统位置,
 * 或者某种类型的jar URL, 其中类路径资源解析为jar位置.
 * 尽管如此, 这个操作仍存在可移植性问题.
 *
 * <p>如果获取最后一个非通配符段的jar URL, 则解析器必须能够从中获取{@code java.net.JarURLConnection},
 * 或者手动解析jar URL, 以便能够遍历jar的内容, 并解析通配符.
 * 这将适用于大多数环境, 但在其他环境中会失败, 强烈建议在依赖它之前,
 * 在特定环境中对来自jar的资源的通配符解析进行全面测试.
 *
 * <p><b>{@code classpath*:}前缀:</b>
 *
 * <p>通过"{@code classpath*:}"前缀, 可以检索具有相同名称的多个类路径资源.
 * 例如, "{@code classpath*:META-INF/beans.xml}"将在类路径中找到所有"beans.xml"文件, 无论是在"classes"目录中还是在JAR文件中.
 * 这对于在每个jar文件中的相同位置自动检测同名的配置文件特别有用.
 * 在内部, 这通过{@code ClassLoader.getResources()}调用发生, 并且是完全可移植的.
 *
 * <p>"classpath*:"前缀也可以与位置路径的其余部分中的PathMatcher模式组合, 例如"classpath*:META-INF/*-beans.xml".
 * 在这种情况下, 解析策略非常简单:
 * 在最后一个非通配符路径段上使用{@code ClassLoader.getResources()}调用来获取类加载器层次结构中的所有匹配资源,
 * 然后关闭每个资源, 将上述相同的PathMatcher解析策略用于通配符子路径.
 *
 * <p><b>其他说明:</b>
 *
 * <p><b>WARNING:</b> 请注意, "{@code classpath*:}"与Ant样式模式结合使用时, 在模式启动前只能与至少一个根目录可靠地工作,
 * 除非实际目标文件驻留在文件系统中.
 * 这意味着像"{@code classpath*:*.xml}"这样的模式将<i>不</i>从jar文件的根目录中检索文件,
 * 而只是从扩展目录的根目录中检索文件.
 * 这源于JDK的{@code ClassLoader.getResources()}方法的限制, 该方法仅返回传入的空字符串的文件系统位置 (指示要搜索的潜在根目录).
 * 这个{@code ResourcePatternResolver}实现试图通过{@link URLClassLoader}内省和"java.class.path" 清单评估来缓解jar root查找限制;
 * 但是, 没有可移植性保证.
 *
 * <p><b>WARNING:</b> 如果要搜索的根包在多个类路径位置中可用, 则不保证具有"classpath:"资源的Ant样式模式可以找到匹配的资源.
 * 这是因为资源, 如
 * <pre class="code">
 *     com/mycompany/package1/service-context.xml
 * </pre>
 * 可能只在一个位置, 但是当一个路径, 如
 * <pre class="code">
 *     classpath:com/mycompany/**&#47;service-context.xml
 * </pre>
 * 用于尝试解决它, 解析器将处理{@code getResource("com/mycompany");}返回的(第一个)URL.
 * 如果此基本包节点存在于多个类加载器位置中, 则实际的最终资源可能不在下面.
 * 因此, 最好在这种情况下使用具有相同Ant样式模式的"{@code classpath*:}", 这将搜索包含根包的<i>所有</i>类路径位置.
 */
public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {

	private static final Log logger = LogFactory.getLog(PathMatchingResourcePatternResolver.class);

	private static Method equinoxResolveMethod;

	static {
		try {
			// Detect Equinox OSGi (e.g. on WebSphere 6.1)
			Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator",
					PathMatchingResourcePatternResolver.class.getClassLoader());
			equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
			logger.debug("Found Equinox FileLocator for OSGi bundle URL resolution");
		}
		catch (Throwable ex) {
			equinoxResolveMethod = null;
		}
	}


	private final ResourceLoader resourceLoader;

	private PathMatcher pathMatcher = new AntPathMatcher();


	/**
	 * 使用DefaultResourceLoader创建一个新的PathMatchingResourcePatternResolver.
	 * <p>ClassLoader访问将通过线程上下文类加载器进行.
	 */
	public PathMatchingResourcePatternResolver() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * <p>ClassLoader访问将通过线程上下文类加载器进行.
	 * 
	 * @param resourceLoader 用于加载根目录和实际资源的ResourceLoader
	 */
	public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 使用DefaultResourceLoader创建一个新的PathMatchingResourcePatternResolver.
	 * 
	 * @param classLoader 用于加载类路径资源的ClassLoader, 或{@code null} 用于在实际资源访问时使用线程上下文类加载器
	 */
	public PathMatchingResourcePatternResolver(ClassLoader classLoader) {
		this.resourceLoader = new DefaultResourceLoader(classLoader);
	}


	/**
	 * 返回此模式解析器使用的ResourceLoader.
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	@Override
	public ClassLoader getClassLoader() {
		return getResourceLoader().getClassLoader();
	}

	/**
	 * 设置用于此资源模式解析器的PathMatcher实现.
	 * 默认是 AntPathMatcher.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 返回此资源模式解析器使用的PathMatcher.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	@Override
	public Resource getResource(String location) {
		return getResourceLoader().getResource(location);
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		Assert.notNull(locationPattern, "Location pattern must not be null");
		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
			// 类路径资源 (可能有多个同名资源)
			if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
				// 类路径资源模式
				return findPathMatchingResources(locationPattern);
			}
			else {
				// 具有给定名称的所有类路径资源
				return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
			}
		}
		else {
			// 一般只在前缀后面查找一个模式, 而在Tomcat上只查找"war:"协议的 "*/"分隔符后面的模式.
			int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
					locationPattern.indexOf(':') + 1);
			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
				// 文件模式
				return findPathMatchingResources(locationPattern);
			}
			else {
				// 具有给定名称的单个资源
				return new Resource[] {getResourceLoader().getResource(locationPattern)};
			}
		}
	}

	/**
	 * 通过ClassLoader查找具有给定位置的所有类位置资源.
	 * 委托给{@link #doFindAllClassPathResources(String)}.
	 * 
	 * @param location 类路径中的绝对路径
	 * 
	 * @return 结果
	 * @throws IOException 在I/O错误的情况下
	 */
	protected Resource[] findAllClassPathResources(String location) throws IOException {
		String path = location;
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		Set<Resource> result = doFindAllClassPathResources(path);
		if (logger.isDebugEnabled()) {
			logger.debug("Resolved classpath location [" + location + "] to resources " + result);
		}
		return result.toArray(new Resource[result.size()]);
	}

	/**
	 * 通过ClassLoader查找具有给定路径的所有类位置资源.
	 * 由{@link #findAllClassPathResources(String)}调用.
	 * 
	 * @param path 类路径中的绝对路径 (不是前导斜杠)
	 * 
	 * @return 一组可匹配的资源实例
	 */
	protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
		Set<Resource> result = new LinkedHashSet<Resource>(16);
		ClassLoader cl = getClassLoader();
		Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
		while (resourceUrls.hasMoreElements()) {
			URL url = resourceUrls.nextElement();
			result.add(convertClassLoaderURL(url));
		}
		if ("".equals(path)) {
			// 上述结果可能不完整, i.e. 仅包含文件系统引用.
			// 还需要指向类路径中每个jar文件的指针...
			addAllClassLoaderJarRoots(cl, result);
		}
		return result;
	}

	/**
	 * 将从ClassLoader返回的给定URL转换为{@link Resource}.
	 * <p>默认实现只是创建一个{@link UrlResource}实例.
	 * 
	 * @param url 从ClassLoader返回的URL
	 * 
	 * @return 相应的Resource对象
	 */
	protected Resource convertClassLoaderURL(URL url) {
		return new UrlResource(url);
	}

	/**
	 * 搜索所有{@link URLClassLoader} URL以获取jar文件引用, 并以指向jar文件内容根目录的指针的形式将它们添加到给定的资源集中.
	 * 
	 * @param classLoader 要搜索的ClassLoader (包括它的祖先)
	 * @param result 要添加jar根目录的资源集
	 */
	protected void addAllClassLoaderJarRoots(ClassLoader classLoader, Set<Resource> result) {
		if (classLoader instanceof URLClassLoader) {
			try {
				for (URL url : ((URLClassLoader) classLoader).getURLs()) {
					try {
						UrlResource jarResource = new UrlResource(
								ResourceUtils.JAR_URL_PREFIX + url + ResourceUtils.JAR_URL_SEPARATOR);
						if (jarResource.exists()) {
							result.add(jarResource);
						}
					}
					catch (MalformedURLException ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Cannot search for matching files underneath [" + url +
									"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
						}
					}
				}
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files since ClassLoader [" + classLoader +
							"] does not support 'getURLs()': " + ex);
				}
			}
		}

		if (classLoader == ClassLoader.getSystemClassLoader()) {
			// "java.class.path" manifest evaluation...
			addClassPathManifestEntries(result);
		}

		if (classLoader != null) {
			try {
				// Hierarchy traversal...
				addAllClassLoaderJarRoots(classLoader.getParent(), result);
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files in parent ClassLoader since [" + classLoader +
							"] does not support 'getParent()': " + ex);
				}
			}
		}
	}

	/**
	 * 从"java.class.path."清单属性中确定JAR文件引用, 并以指向jar文件内容根目录的指针的形式将它们添加到给定的资源集.
	 * 
	 * @param result 要添加jar根目录的资源集
	 */
	protected void addClassPathManifestEntries(Set<Resource> result) {
		try {
			String javaClassPathProperty = System.getProperty("java.class.path");
			for (String path : StringUtils.delimitedListToStringArray(
					javaClassPathProperty, System.getProperty("path.separator"))) {
				try {
					String filePath = new File(path).getAbsolutePath();
					int prefixIndex = filePath.indexOf(':');
					if (prefixIndex == 1) {
						// 在Windows上可能是 "c:"驱动器前缀, 以便进行适当的重复检测
						filePath = StringUtils.capitalize(filePath);
					}
					UrlResource jarResource = new UrlResource(ResourceUtils.JAR_URL_PREFIX +
							ResourceUtils.FILE_URL_PREFIX + filePath + ResourceUtils.JAR_URL_SEPARATOR);
					// 可能与上面的 URLClassLoader.getURLs() 结果重叠!
					if (!result.contains(jarResource) && !hasDuplicate(filePath, result) && jarResource.exists()) {
						result.add(jarResource);
					}
				}
				catch (MalformedURLException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Cannot search for matching files underneath [" + path +
								"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
					}
				}
			}
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
			}
		}
	}

	/**
	 * 检查给定文件路径在现有结果中是否具有重复但结构不同的条目, i.e. 是否带有前导斜杠.
	 * 
	 * @param filePath 文件路径 (带或不带前导斜杠)
	 * @param result 当前结果
	 * 
	 * @return {@code true}如果有重复 (i.e. 忽略给定的文件路径),
	 * {@code false} 继续向当前结果添加相应的资源
	 */
	private boolean hasDuplicate(String filePath, Set<Resource> result) {
		if (result.isEmpty()) {
			return false;
		}
		String duplicatePath = (filePath.startsWith("/") ? filePath.substring(1) : "/" + filePath);
		try {
			return result.contains(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX +
					duplicatePath + ResourceUtils.JAR_URL_SEPARATOR));
		}
		catch (MalformedURLException ex) {
			// Ignore: just for testing against duplicate.
			return false;
		}
	}

	/**
	 * 通过Ant样式PathMatcher查找与给定位置模式匹配的所有资源.
	 * 支持jar文件和zip文件以及文件系统中的资源.
	 * 
	 * @param locationPattern 要匹配的位置模式
	 * 
	 * @return 结果
	 * @throws IOException 发生I/O错误
	 */
	protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
		String rootDirPath = determineRootDir(locationPattern);
		String subPattern = locationPattern.substring(rootDirPath.length());
		Resource[] rootDirResources = getResources(rootDirPath);
		Set<Resource> result = new LinkedHashSet<Resource>(16);
		for (Resource rootDirResource : rootDirResources) {
			rootDirResource = resolveRootDirResource(rootDirResource);
			URL rootDirUrl = rootDirResource.getURL();
			if (equinoxResolveMethod != null) {
				if (rootDirUrl.getProtocol().startsWith("bundle")) {
					rootDirUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
					rootDirResource = new UrlResource(rootDirUrl);
				}
			}
			if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
			}
			else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
				result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
			}
			else {
				result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Resolved location pattern [" + locationPattern + "] to resources " + result);
		}
		return result.toArray(new Resource[result.size()]);
	}

	/**
	 * 确定给定位置的根目录.
	 * <p>用于确定文件匹配的起始点, 将根目录位置解析为{@code java.io.File},
	 * 并将其传递到{@code retrieveMatchingFiles}, 其余位置为模式.
	 * <p>例如, 将为"/WEB-INF/*.xml"模式返回"/WEB-INF/".
	 * 
	 * @param location 要检查的位置
	 * 
	 * @return 表示根目录的位置部分
	 */
	protected String determineRootDir(String location) {
		int prefixEnd = location.indexOf(':') + 1;
		int rootDirEnd = location.length();
		while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
			rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
		}
		if (rootDirEnd == 0) {
			rootDirEnd = prefixEnd;
		}
		return location.substring(0, rootDirEnd);
	}

	/**
	 * 解析指定的资源以进行路径匹配.
	 * <p>默认情况下, Equinox OSGi "bundleresource:" / "bundleentry:" URL
	 * 将被解析为使用Spring的标准jar文件遍历算法遍历的标准jar文件URL.
	 * 对于任何先前的自定义解析, 请覆盖此方法并相应地替换资源句柄.
	 * 
	 * @param original 要解析的资源
	 * 
	 * @return 已解析的资源 (可能与传入的资源相同)
	 * @throws IOException 在解析失败的情况下
	 */
	protected Resource resolveRootDirResource(Resource original) throws IOException {
		return original;
	}

	/**
	 * 返回给定资源句柄是否指示{@code doFindPathMatchingJarResources}方法可以处理的jar资源.
	 * <p>默认情况下, URL协议"jar", "zip", "vfszip" 和 "wsjar"将被视为jar资源.
	 * 该模板方法允许检测其他类型的jar资源, e.g. 通过{@code instanceof}检查资源句柄类型.
	 * 
	 * @param resource 要检查的资源句柄 (通常从根目录开始路径匹配)
	 */
	protected boolean isJarResource(Resource resource) throws IOException {
		return false;
	}

	/**
	 * 通过Ant样式的PathMatcher查找jar文件中与给定位置模式匹配的所有资源.
	 * 
	 * @param rootDirResource 根目录
	 * @param rootDirURL 预解析的根目录URL
	 * @param subPattern 要匹配的子模式 (在根目录下)
	 * 
	 * @return 可匹配的资源实例
	 * @throws IOException 发生I/O错误
	 */
	@SuppressWarnings("deprecation")
	protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, URL rootDirURL, String subPattern)
			throws IOException {

		// 首先检查已弃用的变体以获取潜在的覆盖...
		Set<Resource> result = doFindPathMatchingJarResources(rootDirResource, subPattern);
		if (result != null) {
			return result;
		}

		URLConnection con = rootDirURL.openConnection();
		JarFile jarFile;
		String jarFileUrl;
		String rootEntryPath;
		boolean closeJarFile;

		if (con instanceof JarURLConnection) {
			// 通常应该是传统JAR文件的情况.
			JarURLConnection jarCon = (JarURLConnection) con;
			ResourceUtils.useCachesIfNecessary(jarCon);
			jarFile = jarCon.getJarFile();
			jarFileUrl = jarCon.getJarFileURL().toExternalForm();
			JarEntry jarEntry = jarCon.getJarEntry();
			rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
			closeJarFile = !jarCon.getUseCaches();
		}
		else {
			// 没有JarURLConnection -> 需要求助于URL文件解析.
			// 假设格式为"jar:path!/entry"的URL, 只要遵循条目格式, 协议就是任意的.
			// 还将处理带有和不带有"file:"前缀的路径.
			String urlFile = rootDirURL.getFile();
			try {
				int separatorIndex = urlFile.indexOf(ResourceUtils.WAR_URL_SEPARATOR);
				if (separatorIndex == -1) {
					separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
				}
				if (separatorIndex != -1) {
					jarFileUrl = urlFile.substring(0, separatorIndex);
					rootEntryPath = urlFile.substring(separatorIndex + 2);  // both separators are 2 chars
					jarFile = getJarFile(jarFileUrl);
				}
				else {
					jarFile = new JarFile(urlFile);
					jarFileUrl = urlFile;
					rootEntryPath = "";
				}
				closeJarFile = true;
			}
			catch (ZipException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping invalid jar classpath entry [" + urlFile + "]");
				}
				return Collections.emptySet();
			}
		}

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for matching resources in jar file [" + jarFileUrl + "]");
			}
			if (!"".equals(rootEntryPath) && !rootEntryPath.endsWith("/")) {
				// 根条目路径必须以斜杠结尾才能进行正确匹配.
				// Sun JRE在这里没有返回斜线, 但BEA JRockit确实如此.
				rootEntryPath = rootEntryPath + "/";
			}
			result = new LinkedHashSet<Resource>(8);
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (entryPath.startsWith(rootEntryPath)) {
					String relativePath = entryPath.substring(rootEntryPath.length());
					if (getPathMatcher().match(subPattern, relativePath)) {
						result.add(rootDirResource.createRelative(relativePath));
					}
				}
			}
			return result;
		}
		finally {
			if (closeJarFile) {
				jarFile.close();
			}
		}
	}

	/**
	 * 通过Ant样式的PathMatcher查找jar文件中与给定位置模式匹配的所有资源.
	 * 
	 * @param rootDirResource 根目录
	 * @param subPattern 要匹配的子模式 (在根目录下)
	 * 
	 * @return 可匹配的资源实例
	 * @throws IOException 发生I/O错误
	 * 
	 * @deprecated as of Spring 4.3, in favor of
	 * {@link #doFindPathMatchingJarResources(Resource, URL, String)}
	 */
	@Deprecated
	protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, String subPattern)
			throws IOException {

		return null;
	}

	/**
	 * 将给定的jar文件URL解析为JarFile对象.
	 */
	protected JarFile getJarFile(String jarFileUrl) throws IOException {
		if (jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
			try {
				return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
			}
			catch (URISyntaxException ex) {
				// 无效URI的URL的后备 (几乎不会发生).
				return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
			}
		}
		else {
			return new JarFile(jarFileUrl);
		}
	}

	/**
	 * 通过Ant样式的PathMatcher查找文件系统中与给定位置模式匹配的所有资源.
	 * 
	 * @param rootDirResource 根目录
	 * @param subPattern 要匹配的子模式 (在根目录下)
	 * 
	 * @return 可匹配的资源实例
	 * @throws IOException 发生I/O错误
	 */
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		File rootDir;
		try {
			rootDir = rootDirResource.getFile().getAbsoluteFile();
		}
		catch (IOException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Cannot search for matching files underneath " + rootDirResource +
						" because it does not correspond to a directory in the file system", ex);
			}
			return Collections.emptySet();
		}
		return doFindMatchingFileSystemResources(rootDir, subPattern);
	}

	/**
	 * 通过Ant样式的PathMatcher查找文件系统中与给定位置模式匹配的所有资源.
	 * 
	 * @param rootDir 文件系统中的根目录
	 * @param subPattern 要匹配的子模式 (在根目录下)
	 * 
	 * @return 可匹配的资源实例
	 * @throws IOException 发生I/O错误
	 */
	protected Set<Resource> doFindMatchingFileSystemResources(File rootDir, String subPattern) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for matching resources in directory tree [" + rootDir.getPath() + "]");
		}
		Set<File> matchingFiles = retrieveMatchingFiles(rootDir, subPattern);
		Set<Resource> result = new LinkedHashSet<Resource>(matchingFiles.size());
		for (File file : matchingFiles) {
			result.add(new FileSystemResource(file));
		}
		return result;
	}

	/**
	 * 检索与给定路径模式匹配的文件, 检查给定目录及其子目录.
	 * 
	 * @param rootDir 要从中开始的目录
	 * @param pattern 要匹配的模式, 相对于根目录
	 * 
	 * @return 可匹配的资源实例
	 * @throws IOException 如果无法检索目录内容
	 */
	protected Set<File> retrieveMatchingFiles(File rootDir, String pattern) throws IOException {
		if (!rootDir.exists()) {
			// 静默跳过不存在的目录.
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping [" + rootDir.getAbsolutePath() + "] because it does not exist");
			}
			return Collections.emptySet();
		}
		if (!rootDir.isDirectory()) {
			// 如果它存在但是没有目录.
			if (logger.isWarnEnabled()) {
				logger.warn("Skipping [" + rootDir.getAbsolutePath() + "] because it does not denote a directory");
			}
			return Collections.emptySet();
		}
		if (!rootDir.canRead()) {
			if (logger.isWarnEnabled()) {
				logger.warn("Cannot search for matching files underneath directory [" + rootDir.getAbsolutePath() +
						"] because the application is not allowed to read the directory");
			}
			return Collections.emptySet();
		}
		String fullPattern = StringUtils.replace(rootDir.getAbsolutePath(), File.separator, "/");
		if (!pattern.startsWith("/")) {
			fullPattern += "/";
		}
		fullPattern = fullPattern + StringUtils.replace(pattern, File.separator, "/");
		Set<File> result = new LinkedHashSet<File>(8);
		doRetrieveMatchingFiles(fullPattern, rootDir, result);
		return result;
	}

	/**
	 * 递归检索与给定模式匹配的文件, 将它们添加到给定的结果列表中.
	 * 
	 * @param fullPattern 与之前根目录路径匹配的模式
	 * @param dir 当前目录
	 * @param result 要添加的匹配文件实例的集合
	 * 
	 * @throws IOException 如果无法检索目录内容
	 */
	protected void doRetrieveMatchingFiles(String fullPattern, File dir, Set<File> result) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching directory [" + dir.getAbsolutePath() +
					"] for files matching pattern [" + fullPattern + "]");
		}
		File[] dirContents = dir.listFiles();
		if (dirContents == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Could not retrieve contents of directory [" + dir.getAbsolutePath() + "]");
			}
			return;
		}
		Arrays.sort(dirContents);
		for (File content : dirContents) {
			String currPath = StringUtils.replace(content.getAbsolutePath(), File.separator, "/");
			if (content.isDirectory() && getPathMatcher().matchStart(fullPattern, currPath + "/")) {
				if (!content.canRead()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping subdirectory [" + dir.getAbsolutePath() +
								"] because the application is not allowed to read the directory");
					}
				}
				else {
					doRetrieveMatchingFiles(fullPattern, content, result);
				}
			}
			if (getPathMatcher().match(fullPattern, currPath)) {
				result.add(content);
			}
		}
	}


	/**
	 * 内部委托类, 在运行时避免硬JBoss VFS API依赖.
	 */
	private static class VfsResourceMatchingDelegate {

		public static Set<Resource> findMatchingResources(
				URL rootDirURL, String locationPattern, PathMatcher pathMatcher) throws IOException {

			Object root = VfsPatternUtils.findRoot(rootDirURL);
			PatternVirtualFileVisitor visitor =
					new PatternVirtualFileVisitor(VfsPatternUtils.getPath(root), locationPattern, pathMatcher);
			VfsPatternUtils.visit(root, visitor);
			return visitor.getResources();
		}
	}


	/**
	 * 用于路径匹配目的的VFS访问器.
	 */
	@SuppressWarnings("unused")
	private static class PatternVirtualFileVisitor implements InvocationHandler {

		private final String subPattern;

		private final PathMatcher pathMatcher;

		private final String rootPath;

		private final Set<Resource> resources = new LinkedHashSet<Resource>();

		public PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher) {
			this.subPattern = subPattern;
			this.pathMatcher = pathMatcher;
			this.rootPath = (rootPath.isEmpty() || rootPath.endsWith("/") ? rootPath : rootPath + "/");
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (Object.class == method.getDeclaringClass()) {
				if (methodName.equals("equals")) {
					// 只有当代理相同时才考虑相等.
					return (proxy == args[0]);
				}
				else if (methodName.equals("hashCode")) {
					return System.identityHashCode(proxy);
				}
			}
			else if ("getAttributes".equals(methodName)) {
				return getAttributes();
			}
			else if ("visit".equals(methodName)) {
				visit(args[0]);
				return null;
			}
			else if ("toString".equals(methodName)) {
				return toString();
			}

			throw new IllegalStateException("Unexpected method invocation: " + method);
		}

		public void visit(Object vfsResource) {
			if (this.pathMatcher.match(this.subPattern,
					VfsPatternUtils.getPath(vfsResource).substring(this.rootPath.length()))) {
				this.resources.add(new VfsResource(vfsResource));
			}
		}

		public Object getAttributes() {
			return VfsPatternUtils.getVisitorAttribute();
		}

		public Set<Resource> getResources() {
			return this.resources;
		}

		public int size() {
			return this.resources.size();
		}

		@Override
		public String toString() {
			return "sub-pattern: " + this.subPattern + ", resources: " + this.resources;
		}
	}
}
