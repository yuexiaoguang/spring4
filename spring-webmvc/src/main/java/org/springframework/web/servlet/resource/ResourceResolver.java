package org.springframework.web.servlet.resource;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * 用于解析对服务器端资源的请求的策略.
 *
 * <p>提供用于解析对实际{@link org.springframework.core.io.Resource}的传入请求以及获取客户端在请求资源时应使用的公共URL路径的机制.
 */
public interface ResourceResolver {

	/**
	 * 将提供的请求和请求路径解析为存在于某个给定资源位置下的{@link Resource}.
	 * 
	 * @param request 当前的请求
	 * @param requestPath 要使用的请求路径的一部分
	 * @param locations 查找资源时要搜索的位置
	 * @param chain 要委托给的剩余解析器链
	 * 
	 * @return 已解析的资源或{@code null}
	 */
	Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations,
			ResourceResolverChain chain);

	/**
	 * 解析面向外部的<em>public</em> URL路径, 以供客户端用于访问位于给定<em>内部</em>资源路径的资源.
	 * <p>这在向客户端呈现URL链接时很有用.
	 * 
	 * @param resourcePath 内部资源路径
	 * @param locations 查找资源时要搜索的位置
	 * @param chain 要委托给的解析器链
	 * 
	 * @return 已解析的公用URL路径或{@code null}
	 */
	String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain);

}
