package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * Interface for a resource descriptor that describes its version with a
 * version string that can be derived from its content and/or metadata.
 */
public interface VersionedResource extends Resource {

	String getVersion();

}
