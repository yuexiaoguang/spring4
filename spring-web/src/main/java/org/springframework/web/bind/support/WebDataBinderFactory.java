package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A factory for creating a {@link WebDataBinder} instance for a named target object.
 */
public interface WebDataBinderFactory {

	/**
	 * Create a {@link WebDataBinder} for the given object.
	 * @param webRequest the current request
	 * @param target the object to create a data binder for, or {@code null} if creating a binder for a simple type
	 * @param objectName the name of the target object
	 * @return the created {@link WebDataBinder} instance, never null
	 * @throws Exception raised if the creation and initialization of the data binder fails
	 */
	WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception;

}
