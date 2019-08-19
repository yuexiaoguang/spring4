package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link ResourceResolverChain}的默认实现, 用于调用{@link ResourceResolver}的列表.
 */
class DefaultResourceResolverChain implements ResourceResolverChain {

	private final List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();

	private int index = -1;


	public DefaultResourceResolverChain(List<? extends ResourceResolver> resolvers) {
		if (resolvers != null) {
			this.resolvers.addAll(resolvers);
		}
	}


	@Override
	public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations) {
		ResourceResolver resolver = getNext();
		if (resolver == null) {
			return null;
		}

		try {
			return resolver.resolveResource(request, requestPath, locations, this);
		}
		finally {
			this.index--;
		}
	}

	@Override
	public String resolveUrlPath(String resourcePath, List<? extends Resource> locations) {
		ResourceResolver resolver = getNext();
		if (resolver == null) {
			return null;
		}

		try {
			return resolver.resolveUrlPath(resourcePath, locations, this);
		}
		finally {
			this.index--;
		}
	}

	private ResourceResolver getNext() {
		Assert.state(this.index <= this.resolvers.size(),
				"Current index exceeds the number of configured ResourceResolvers");

		if (this.index == (this.resolvers.size() - 1)) {
			return null;
		}
		this.index++;
		return this.resolvers.get(this.index);
	}

}
