package org.springframework.web.servlet.resource;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * 用于调用{@link ResourceResolver}链的约定, 其中每个解析器都被赋予对链的引用, 允许它在必要时委托.
 */
public interface ResourceResolverChain {

	/**
	 * 将提供的请求和请求路径解析为存在于某个给定资源位置下的{@link Resource}.
	 * 
	 * @param request 当前的请求
	 * @param requestPath 要使用的请求路径的一部分
	 * @param locations 查找资源时要搜索的位置
	 * 
	 * @return 已解析的资源或{@code null}
	 */
	Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations);

	/**
	 * 解析面向外部的<em>public</em> URL路径, 以供客户端用于访问位于给定<em>内部</em>资源路径的资源.
	 * <p>这在向客户端呈现URL链接时很有用.
	 * 
	 * @param resourcePath 内部资源路径
	 * @param locations 查找资源时要搜索的位置
	 * 
	 * @return 已解析的公用URL路径或{@code null}
	 */
	String resolveUrlPath(String resourcePath, List<? extends Resource> locations);

}
