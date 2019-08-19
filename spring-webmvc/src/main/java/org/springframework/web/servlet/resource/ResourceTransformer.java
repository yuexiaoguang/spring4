package org.springframework.web.servlet.resource;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * 用于转换资源内容的抽象.
 */
public interface ResourceTransformer {

	/**
	 * 转换给定的资源.
	 * 
	 * @param request 当前的请求
	 * @param resource 要转换的资源
	 * @param transformerChain 要委托给的剩余的转换器链
	 * 
	 * @return 转换后的资源 (never {@code null})
	 * @throws IOException 如果转换失败
	 */
	Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException;

}
