package org.springframework.mock.web.portlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.portlet.PortletConfig} interface.
 */
public class MockPortletConfig implements PortletConfig {

	private final PortletContext portletContext;

	private final String portletName;

	private final Map<Locale, ResourceBundle> resourceBundles = new HashMap<Locale, ResourceBundle>();

	private final Map<String, String> initParameters = new LinkedHashMap<String, String>();

	private final Set<String> publicRenderParameterNames = new LinkedHashSet<String>();

	private String defaultNamespace = XMLConstants.NULL_NS_URI;

	private final Set<QName> publishingEventQNames = new LinkedHashSet<QName>();

	private final Set<QName> processingEventQNames = new LinkedHashSet<QName>();

	private final Set<Locale> supportedLocales = new LinkedHashSet<Locale>();

	private final Map<String, String[]> containerRuntimeOptions = new LinkedHashMap<String, String[]>();


	/**
	 * Create a new MockPortletConfig with a default {@link MockPortletContext}.
	 */
	public MockPortletConfig() {
		this(null, "");
	}

	/**
	 * Create a new MockPortletConfig with a default {@link MockPortletContext}.
	 * @param portletName the name of the portlet
	 */
	public MockPortletConfig(String portletName) {
		this(null, portletName);
	}

	/**
	 * Create a new MockPortletConfig.
	 * @param portletContext the PortletContext that the portlet runs in
	 */
	public MockPortletConfig(PortletContext portletContext) {
		this(portletContext, "");
	}

	/**
	 * Create a new MockPortletConfig.
	 * @param portletContext the PortletContext that the portlet runs in
	 * @param portletName the name of the portlet
	 */
	public MockPortletConfig(PortletContext portletContext, String portletName) {
		this.portletContext = (portletContext != null ? portletContext : new MockPortletContext());
		this.portletName = portletName;
	}


	@Override
	public String getPortletName() {
		return this.portletName;
	}

	@Override
	public PortletContext getPortletContext() {
		return this.portletContext;
	}

	public void setResourceBundle(Locale locale, ResourceBundle resourceBundle) {
		Assert.notNull(locale, "Locale must not be null");
		this.resourceBundles.put(locale, resourceBundle);
	}

	@Override
	public ResourceBundle getResourceBundle(Locale locale) {
		Assert.notNull(locale, "Locale must not be null");
		return this.resourceBundles.get(locale);
	}

	public void addInitParameter(String name, String value) {
		Assert.notNull(name, "Parameter name must not be null");
		this.initParameters.put(name, value);
	}

	@Override
	public String getInitParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		return this.initParameters.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(this.initParameters.keySet());
	}

	public void addPublicRenderParameterName(String name) {
		this.publicRenderParameterNames.add(name);
	}

	@Override
	public Enumeration<String> getPublicRenderParameterNames() {
		return Collections.enumeration(this.publicRenderParameterNames);
	}

	public void setDefaultNamespace(String defaultNamespace) {
		this.defaultNamespace = defaultNamespace;
	}

	@Override
	public String getDefaultNamespace() {
		return this.defaultNamespace;
	}

	public void addPublishingEventQName(QName name) {
		this.publishingEventQNames.add(name);
	}

	@Override
	public Enumeration<QName> getPublishingEventQNames() {
		return Collections.enumeration(this.publishingEventQNames);
	}

	public void addProcessingEventQName(QName name) {
		this.processingEventQNames.add(name);
	}

	@Override
	public Enumeration<QName> getProcessingEventQNames() {
		return Collections.enumeration(this.processingEventQNames);
	}

	public void addSupportedLocale(Locale locale) {
		this.supportedLocales.add(locale);
	}

	@Override
	public Enumeration<Locale> getSupportedLocales() {
		return Collections.enumeration(this.supportedLocales);
	}

	public void addContainerRuntimeOption(String key, String value) {
		this.containerRuntimeOptions.put(key, new String[] {value});
	}

	public void addContainerRuntimeOption(String key, String[] values) {
		this.containerRuntimeOptions.put(key, values);
	}

	@Override
	public Map<String, String[]> getContainerRuntimeOptions() {
		return Collections.unmodifiableMap(this.containerRuntimeOptions);
	}

}
