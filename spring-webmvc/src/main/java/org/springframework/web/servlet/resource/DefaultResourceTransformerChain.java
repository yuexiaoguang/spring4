package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link ResourceTransformerChain}的默认实现, 用于调用{@link ResourceTransformer}的列表.
 */
class DefaultResourceTransformerChain implements ResourceTransformerChain {

	private final ResourceResolverChain resolverChain;

	private final List<ResourceTransformer> transformers = new ArrayList<ResourceTransformer>();

	private int index = -1;


	public DefaultResourceTransformerChain(ResourceResolverChain resolverChain,
			List<ResourceTransformer> transformers) {

		Assert.notNull(resolverChain, "ResourceResolverChain is required");
		this.resolverChain = resolverChain;
		if (transformers != null) {
			this.transformers.addAll(transformers);
		}
	}


	public ResourceResolverChain getResolverChain() {
		return this.resolverChain;
	}


	@Override
	public Resource transform(HttpServletRequest request, Resource resource) throws IOException {
		ResourceTransformer transformer = getNext();
		if (transformer == null) {
			return resource;
		}

		try {
			return transformer.transform(request, resource, this);
		}
		finally {
			this.index--;
		}
	}

	private ResourceTransformer getNext() {
		Assert.state(this.index <= this.transformers.size(),
				"Current index exceeds the number of configured ResourceTransformer's");

		if (this.index == (this.transformers.size() - 1)) {
			return null;
		}

		this.index++;
		return this.transformers.get(this.index);
	}

}
