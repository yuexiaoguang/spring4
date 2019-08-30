package org.springframework.web.portlet.context;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.portlet.PortletContext;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * {@link PathMatchingResourcePatternResolver}的感知PortletContext的子类,
 * 能够通过Portlet API的{@code PortletContext.getResourcePaths}在Web应用程序根目录下查找匹配的资源.
 * 回退到超类的文件系统, 检查其他资源.
 *
 * <p>使用{@code PortletContext.getResourcePaths}查找匹配文件的优点是它可以在一个尚未展开的WAR文件中工作.
 */
public class PortletContextResourcePatternResolver extends PathMatchingResourcePatternResolver {

	/**
	 * @param portletContext 用于加载资源的PortletContext
	 */
	public PortletContextResourcePatternResolver(PortletContext portletContext) {
		super(new PortletContextResourceLoader(portletContext));
	}

	/**
	 * @param resourceLoader 用于加载根目录和实际资源的ResourceLoader
	 */
	public PortletContextResourcePatternResolver(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}


	/**
	 * 重写版本, 检查PortletContextResource,
	 * 并使用{@code PortletContext.getResourcePaths}查找Web应用程序根目录下的匹配资源.
	 * 在其他资源的情况下, 委托给超类版本.
	 */
	@Override
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern) throws IOException {
		if (rootDirResource instanceof PortletContextResource) {
			PortletContextResource pcResource = (PortletContextResource) rootDirResource;
			PortletContext pc = pcResource.getPortletContext();
			String fullPattern = pcResource.getPath() + subPattern;
			Set<Resource> result = new HashSet<Resource>();
			doRetrieveMatchingPortletContextResources(pc, fullPattern, pcResource.getPath(), result);
			return result;
		}
		return super.doFindPathMatchingFileResources(rootDirResource, subPattern);
	}

	/**
	 * 递归检索与给定模式匹配的PortletContextResources, 将它们添加到给定的结果集.
	 * 
	 * @param portletContext 要处理的PortletContext
	 * @param fullPattern 与预先渲染的根目录路径匹配的模式
	 * @param dir 当前目录
	 * @param result 要添加到的匹配的资源集
	 * 
	 * @throws IOException 如果无法检索目录内容
	 */
	protected void doRetrieveMatchingPortletContextResources(
			PortletContext portletContext, String fullPattern, String dir, Set<Resource> result) throws IOException {

		Set<String> candidates = portletContext.getResourcePaths(dir);
		if (candidates != null) {
			boolean dirDepthNotFixed = fullPattern.contains("**");
			for (Iterator<String> it = candidates.iterator(); it.hasNext();) {
				String currPath = it.next();
				if (currPath.endsWith("/") &&
						(dirDepthNotFixed ||
						StringUtils.countOccurrencesOf(currPath, "/") <= StringUtils.countOccurrencesOf(fullPattern, "/"))) {
					doRetrieveMatchingPortletContextResources(portletContext, fullPattern, currPath, result);
				}
				if (getPathMatcher().match(fullPattern, currPath)) {
					result.add(new PortletContextResource(portletContext, currPath));
				}
			}
		}
	}

}
