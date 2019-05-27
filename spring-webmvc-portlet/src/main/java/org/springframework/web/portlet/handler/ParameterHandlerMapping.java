package org.springframework.web.portlet.handler;

import java.util.Map;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link org.springframework.web.portlet.HandlerMapping}
 * to map from a request parameter to request handler beans.
 *
 * <p>The default name of the parameter is "action", but can be changed using
 * {@link #setParameterName setParameterName()}.
 *
 * <p>The bean configuration for this mapping will look somthing like this:
 *
 * <pre class="code">
 * &lt;bean id="parameterHandlerMapping" class="org.springframework.web.portlet.handler.ParameterHandlerMapping"&gt;
 *   &lt;property name="parameterMap"&gt;
 *     &lt;map&gt;
 * 	     &lt;entry key="add"&gt;&lt;ref bean="addItemHandler"/&gt;&lt;/entry&gt;
 *       &lt;entry key="edit"&gt;&lt;ref bean="editItemHandler"/&gt;&lt;/entry&gt;
 *       &lt;entry key="delete"&gt;&lt;ref bean="deleteItemHandler"/&gt;&lt;/entry&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Thanks to Rainer Schmitz for suggesting this mapping strategy!
 */
public class ParameterHandlerMapping extends AbstractMapBasedHandlerMapping<String> {

	/**
	 * Default request parameter name to use for mapping to handlers: "action".
	 */
	public final static String DEFAULT_PARAMETER_NAME = "action";


	private String parameterName = DEFAULT_PARAMETER_NAME;

	private Map<String, ?> parameterMap;


	/**
	 * Set the name of the parameter used for mapping to handlers.
	 * <p>Default is "action".
	 */
	public void setParameterName(String parameterName) {
		Assert.hasText(parameterName, "'parameterName' must not be empty");
		this.parameterName = parameterName;
	}

	/**
	 * Set a Map with parameters as keys and handler beans or bean names as values.
	 * Convenient for population with bean references.
	 * @param parameterMap map with parameters as keys and beans as values
	 */
	public void setParameterMap(Map<String, ?> parameterMap) {
		this.parameterMap = parameterMap;
	}


	/**
	 * Calls the {@code registerHandlers} method in addition
	 * to the superclass's initialization.
	 * @see #registerHandlers
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.parameterMap);
	}

	/**
	 * Uses the value of the specified parameter as lookup key.
	 * @see #setParameterName
	 */
	@Override
	protected String getLookupKey(PortletRequest request) throws Exception {
		return request.getParameter(this.parameterName);
	}

}
