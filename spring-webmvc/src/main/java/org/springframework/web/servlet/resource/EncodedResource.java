package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * Interface for a resource descriptor that describes the encoding
 * applied to the entire resource content.
 *
 * <p>This information is required if the client consuming that resource
 * needs additional decoding capabilities to retrieve the resource's content.
 */
public interface EncodedResource extends Resource {

	/**
	 * The content coding value, as defined in the IANA registry
	 * @return the content encoding
	 */
	String getContentEncoding();

}
