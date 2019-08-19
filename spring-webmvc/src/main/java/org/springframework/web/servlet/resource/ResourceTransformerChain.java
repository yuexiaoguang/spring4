package org.springframework.web.servlet.resource;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * 用于调用{@link ResourceTransformer}链的约定, 其中每个解析器都被赋予对链的引用, 允许它在必要时委托.
 */
public interface ResourceTransformerChain {

	/**
	 * 返回用于解析正在转换的{@code Resource}的{@code ResourceResolverChain}.
	 * 这可能是解析相关资源所必需的, e.g. 链接到其他资源.
	 */
	ResourceResolverChain getResolverChain();

	/**
	 * 转换给定的资源.
	 * 
	 * @param request 当前的请求
	 * @param resource 要转换的候选资源
	 * 
	 * @return 转换后的或相同的资源, never {@code null}
	 * @throws IOException 如果转换失败
	 */
	Resource transform(HttpServletRequest request, Resource resource) throws IOException;

}
