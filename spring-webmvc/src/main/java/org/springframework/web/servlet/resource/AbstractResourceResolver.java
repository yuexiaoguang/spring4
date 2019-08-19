package org.springframework.web.servlet.resource;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;

/**
 * {@link org.springframework.web.servlet.resource.ResourceResolver}实现的基类. 提供一致的记录.
 */
public abstract class AbstractResourceResolver implements ResourceResolver {

	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public Resource resolveResource(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		if (logger.isTraceEnabled()) {
			logger.trace("Resolving resource for request path \"" + requestPath + "\"");
		}
		return resolveResourceInternal(request, requestPath, locations, chain);
	}

	@Override
	public String resolveUrlPath(String resourceUrlPath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		if (logger.isTraceEnabled()) {
			logger.trace("Resolving public URL for resource path \"" + resourceUrlPath + "\"");
		}

		return resolveUrlPathInternal(resourceUrlPath, locations, chain);
	}


	protected abstract Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain);

	protected abstract String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain);

}
