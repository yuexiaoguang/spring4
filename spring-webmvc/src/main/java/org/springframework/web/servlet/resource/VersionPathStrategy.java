package org.springframework.web.servlet.resource;

/**
 * A strategy for extracting and embedding a resource version in its URL path.
*/
public interface VersionPathStrategy {

	/**
	 * Extract the resource version from the request path.
	 * @param requestPath the request path to check
	 * @return the version string or {@code null} if none was found
	 */
	String extractVersion(String requestPath);

	/**
	 * Remove the version from the request path. It is assumed that the given
	 * version was extracted via {@link #extractVersion(String)}.
	 * @param requestPath the request path of the resource being resolved
	 * @param version the version obtained from {@link #extractVersion(String)}
	 * @return the request path with the version removed
	 */
	String removeVersion(String requestPath, String version);

	/**
	 * Add a version to the given request path.
	 * @param requestPath the requestPath
	 * @param version the version
	 * @return the requestPath updated with a version string
	 */
	String addVersion(String requestPath, String version);

}
