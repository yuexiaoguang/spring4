package org.springframework.mock.web.portlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.WindowState;

/**
 * {@link javax.portlet.PortalContext}接口的模拟实现.
 */
public class MockPortalContext implements PortalContext {

	private final Map<String, String> properties = new HashMap<String, String>();

	private final List<PortletMode> portletModes;

	private final List<WindowState> windowStates;


	/**
	 * 使用默认的PortletMode (VIEW, EDIT, HELP)和默认的WindowState (NORMAL, MAXIMIZED, MINIMIZED).
	 */
	public MockPortalContext() {
		this.portletModes = new ArrayList<PortletMode>(3);
		this.portletModes.add(PortletMode.VIEW);
		this.portletModes.add(PortletMode.EDIT);
		this.portletModes.add(PortletMode.HELP);

		this.windowStates = new ArrayList<WindowState>(3);
		this.windowStates.add(WindowState.NORMAL);
		this.windowStates.add(WindowState.MAXIMIZED);
		this.windowStates.add(WindowState.MINIMIZED);
	}

	/**
	 * @param supportedPortletModes 支持的PortletMode实例
	 * @param supportedWindowStates 支持的WindowState实例
	 */
	public MockPortalContext(List<PortletMode> supportedPortletModes, List<WindowState> supportedWindowStates) {
		this.portletModes = new ArrayList<PortletMode>(supportedPortletModes);
		this.windowStates = new ArrayList<WindowState>(supportedWindowStates);
	}


	@Override
	public String getPortalInfo() {
		return "MockPortal/1.0";
	}

	public void setProperty(String name, String value) {
		this.properties.put(name, value);
	}

	@Override
	public String getProperty(String name) {
		return this.properties.get(name);
	}

	@Override
	public Enumeration<String> getPropertyNames() {
		return Collections.enumeration(this.properties.keySet());
	}

	@Override
	public Enumeration<PortletMode> getSupportedPortletModes() {
		return Collections.enumeration(this.portletModes);
	}

	@Override
	public Enumeration<WindowState> getSupportedWindowStates() {
		return Collections.enumeration(this.windowStates);
	}

}
