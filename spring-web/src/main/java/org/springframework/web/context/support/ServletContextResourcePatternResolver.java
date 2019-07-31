package org.springframework.web.context.support;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link PathMatchingResourcePatternResolver}的了解ServletContext子类,
 * 能够通过{@link ServletContext#getResourcePaths}在Web应用程序根目录下查找匹配的资源.
 * 回退到超类的文件系统, 检查其他资源.
 */
public class ServletContextResourcePatternResolver extends PathMatchingResourcePatternResolver {

	private static final Log logger = LogFactory.getLog(ServletContextResourcePatternResolver.class);


	/**
	 * @param servletContext 用于加载资源的ServletContext
	 */
	public ServletContextResourcePatternResolver(ServletContext servletContext) {
		super(new ServletContextResourceLoader(servletContext));
	}

	/**
	 * @param resourceLoader 用于加载根目录和实际资源的ResourceLoader
	 */
	public ServletContextResourcePatternResolver(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}


	/**
	 * 重写版本, 检查ServletContextResource并使用{@code ServletContext.getResourcePaths}
	 * 查找Web应用程序根目录下的匹配资源.
	 * 在其他资源的情况下, 委托给超类版本.
	 */
	@Override
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		if (rootDirResource instanceof ServletContextResource) {
			ServletContextResource scResource = (ServletContextResource) rootDirResource;
			ServletContext sc = scResource.getServletContext();
			String fullPattern = scResource.getPath() + subPattern;
			Set<Resource> result = new LinkedHashSet<Resource>(8);
			doRetrieveMatchingServletContextResources(sc, fullPattern, scResource.getPath(), result);
			return result;
		}
		else {
			return super.doFindPathMatchingFileResources(rootDirResource, subPattern);
		}
	}

	/**
	 * 递归检索与给定模式匹配的ServletContextResources, 将它们添加到给定的结果集中.
	 * 
	 * @param servletContext 要处理的ServletContext
	 * @param fullPattern 与预先渲染的根目录路径匹配的模式
	 * @param dir 当前目录
	 * @param result 要添加的匹配资源集
	 * 
	 * @throws IOException 如果无法检索目录内容
	 */
	protected void doRetrieveMatchingServletContextResources(
			ServletContext servletContext, String fullPattern, String dir, Set<Resource> result)
			throws IOException {

		Set<String> candidates = servletContext.getResourcePaths(dir);
		if (candidates != null) {
			boolean dirDepthNotFixed = fullPattern.contains("**");
			int jarFileSep = fullPattern.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
			String jarFilePath = null;
			String pathInJarFile = null;
			if (jarFileSep > 0 && jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length() < fullPattern.length()) {
				jarFilePath = fullPattern.substring(0, jarFileSep);
				pathInJarFile = fullPattern.substring(jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length());
			}
			for (String currPath : candidates) {
				if (!currPath.startsWith(dir)) {
					// 返回的资源路径不以相对目录开头: 假设返回绝对路径  -> 剥离绝对路径.
					int dirIndex = currPath.indexOf(dir);
					if (dirIndex != -1) {
						currPath = currPath.substring(dirIndex);
					}
				}
				if (currPath.endsWith("/") && (dirDepthNotFixed || StringUtils.countOccurrencesOf(currPath, "/") <=
						StringUtils.countOccurrencesOf(fullPattern, "/"))) {
					// 递归搜索子目录: ServletContext.getResourcePaths 仅返回一个目录级别的条目.
					doRetrieveMatchingServletContextResources(servletContext, fullPattern, currPath, result);
				}
				if (jarFilePath != null && getPathMatcher().match(jarFilePath, currPath)) {
					// 基本模式匹配jar文件 - 搜索匹配的条目.
					String absoluteJarPath = servletContext.getRealPath(currPath);
					if (absoluteJarPath != null) {
						doRetrieveMatchingJarEntries(absoluteJarPath, pathInJarFile, result);
					}
				}
				if (getPathMatcher().match(fullPattern, currPath)) {
					result.add(new ServletContextResource(servletContext, currPath));
				}
			}
		}
	}

	/**
	 * 按模式从给定jar中提取条目.
	 * 
	 * @param jarFilePath jar文件的路径
	 * @param entryPattern 匹配jar条目的模式
	 * @param result 要添加的匹配资源集
	 */
	private void doRetrieveMatchingJarEntries(String jarFilePath, String entryPattern, Set<Resource> result) {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching jar file [" + jarFilePath + "] for entries matching [" + entryPattern + "]");
		}
		try {
			JarFile jarFile = new JarFile(jarFilePath);
			try {
				for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
					JarEntry entry = entries.nextElement();
					String entryPath = entry.getName();
					if (getPathMatcher().match(entryPattern, entryPath)) {
						result.add(new UrlResource(
								ResourceUtils.URL_PROTOCOL_JAR,
								ResourceUtils.FILE_URL_PREFIX + jarFilePath + ResourceUtils.JAR_URL_SEPARATOR + entryPath));
					}
				}
			}
			finally {
				jarFile.close();
			}
		}
		catch (IOException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Cannot search for matching resources in jar file [" + jarFilePath +
						"] because the jar cannot be opened through the file system", ex);
			}
		}
	}

}
