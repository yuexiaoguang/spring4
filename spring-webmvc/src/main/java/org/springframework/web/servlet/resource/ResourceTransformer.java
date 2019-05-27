package org.springframework.web.servlet.resource;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * An abstraction for transforming the content of a resource.
 */
public interface ResourceTransformer {

	/**
	 * Transform the given resource.
	 * @param request the current request
	 * @param resource the resource to transform
	 * @param transformerChain the chain of remaining transformers to delegate to
	 * @return the transformed resource (never {@code null})
	 * @throws IOException if the transformation fails
	 */
	Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException;

}
