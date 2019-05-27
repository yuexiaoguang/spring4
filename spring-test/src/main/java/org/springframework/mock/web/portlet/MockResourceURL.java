package org.springframework.mock.web.portlet;

import java.util.Map;
import javax.portlet.ResourceURL;

/**
 * Mock implementation of the {@link javax.portlet.ResourceURL} interface.
 */
public class MockResourceURL extends MockBaseURL implements ResourceURL {

	private String resourceID;

	private String cacheability;


	//---------------------------------------------------------------------
	// ResourceURL methods
	//---------------------------------------------------------------------

	@Override
	public void setResourceID(String resourceID) {
		this.resourceID = resourceID;
	}

	public String getResourceID() {
		return this.resourceID;
	}

	@Override
	public void setCacheability(String cacheLevel) {
		this.cacheability = cacheLevel;
	}

	@Override
	public String getCacheability() {
		return this.cacheability;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(encodeParameter("resourceID", this.resourceID));
		if (this.cacheability != null) {
			sb.append(";").append(encodeParameter("cacheability", this.cacheability));
		}
		for (Map.Entry<String, String[]> entry : this.parameters.entrySet()) {
			sb.append(";").append(encodeParameter("param_" + entry.getKey(), entry.getValue()));
		}
		return (isSecure() ? "https:" : "http:") +
				"//localhost/mockportlet?" + sb.toString();
	}

}
