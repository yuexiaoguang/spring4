package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * An extension of {@link VersionPathStrategy} that adds a method
 * to determine the actual version of a {@link Resource}.
*/
public interface VersionStrategy extends VersionPathStrategy {

	/**
	 * Determine the version for the given resource.
	 * @param resource the resource to check
	 * @return the version (never {@code null})
	 */
	String getResourceVersion(Resource resource);

}
