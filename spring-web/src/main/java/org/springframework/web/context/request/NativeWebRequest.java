package org.springframework.web.context.request;

/**
 * Extension of the {@link WebRequest} interface, exposing the
 * native request and response objects in a generic fashion.
 *
 * <p>Mainly intended for framework-internal usage,
 * in particular for generic argument resolution code.
 */
public interface NativeWebRequest extends WebRequest {

	/**
	 * Return the underlying native request object.
	 */
	Object getNativeRequest();

	/**
	 * Return the underlying native response object, if any.
	 */
	Object getNativeResponse();

	/**
	 * Return the underlying native request object, if available.
	 * @param requiredType the desired type of request object
	 * @return the matching request object, or {@code null} if none
	 * of that type is available
	 */
	<T> T getNativeRequest(Class<T> requiredType);

	/**
	 * Return the underlying native response object, if available.
	 * @param requiredType the desired type of response object
	 * @return the matching response object, or {@code null} if none
	 * of that type is available
	 */
	<T> T getNativeResponse(Class<T> requiredType);

}
