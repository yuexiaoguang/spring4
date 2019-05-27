package org.springframework.web.portlet.bind;

import javax.portlet.PortletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * PropertyValues implementation created from parameters in a PortletRequest.
 * Can look for all property values beginning with a certain prefix and
 * prefix separator (default is "_").
 *
 * <p>For example, with a prefix of "spring", "spring_param1" and
 * "spring_param2" result in a Map with "param1" and "param2" as keys.
 *
 * <p>This class is not immutable to be able to efficiently remove property
 * values that should be ignored for binding.
 */
@SuppressWarnings("serial")
public class PortletRequestParameterPropertyValues extends MutablePropertyValues {

	/** Default prefix separator */
	public static final String DEFAULT_PREFIX_SEPARATOR = "_";


	/**
	 * Create new PortletRequestPropertyValues using no prefix
	 * (and hence, no prefix separator).
	 * @param request portlet request
	 */
	public PortletRequestParameterPropertyValues(PortletRequest request) {
		this(request, null, null);
	}

	/**
	 * Create new PortletRequestPropertyValues using the given prefix and
	 * the default prefix separator (the underscore character "_").
	 * @param request portlet request
	 * @param prefix the prefix for parameters (the full prefix will
	 * consist of this plus the separator)
	 */
	public PortletRequestParameterPropertyValues(PortletRequest request, String prefix) {
		this(request, prefix, DEFAULT_PREFIX_SEPARATOR);
	}

	/**
	 * Create new PortletRequestPropertyValues supplying both prefix and
	 * prefix separator.
	 * @param request portlet request
	 * @param prefix the prefix for parameters (the full prefix will
	 * consist of this plus the separator)
	 * @param prefixSeparator separator delimiting prefix (e.g. "spring")
	 * and the rest of the parameter name ("param1", "param2")
	 */
	public PortletRequestParameterPropertyValues(PortletRequest request, String prefix, String prefixSeparator) {
		super(PortletUtils.getParametersStartingWith(
				request, (prefix != null ? prefix + prefixSeparator : null)));
	}

}
