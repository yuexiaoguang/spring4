package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.WebRequest;

/**
 * Callback interface for initializing a {@link org.springframework.web.bind.WebDataBinder}
 * for performing data binding in the context of a specific web request.
 */
public interface WebBindingInitializer {

	/**
	 * Initialize the given DataBinder for the given request.
	 * @param binder the DataBinder to initialize
	 * @param request the web request that the data binding happens within
	 */
	void initBinder(WebDataBinder binder, WebRequest request);

}