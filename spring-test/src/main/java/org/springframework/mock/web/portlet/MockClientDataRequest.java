package org.springframework.mock.web.portlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import javax.portlet.ClientDataRequest;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;

/**
 * {@link javax.portlet.ClientDataRequest}接口的模拟实现.
 */
public class MockClientDataRequest extends MockPortletRequest implements ClientDataRequest {

	private String characterEncoding;

	private byte[] content;

	private String contentType;

	private String method;


	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 */
	public MockClientDataRequest() {
		super();
	}

	/**
	 * 使用默认的{@link MockPortalContext}.
	 * 
	 * @param portletContext 运行请求的PortletContext
	 */
	public MockClientDataRequest(PortletContext portletContext) {
		super(portletContext);
	}

	/**
	 * @param portalContext 运行请求的PortalContext
	 * @param portletContext 运行请求的PortletContext
	 */
	public MockClientDataRequest(PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
	}


	public void setContent(byte[] content) {
		this.content = content;
	}

	@Override
	public InputStream getPortletInputStream() throws IOException {
		if (this.content != null) {
			return new ByteArrayInputStream(this.content);
		}
		else {
			return null;
		}
	}

	@Override
	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	@Override
	public BufferedReader getReader() throws UnsupportedEncodingException {
		if (this.content != null) {
			InputStream sourceStream = new ByteArrayInputStream(this.content);
			Reader sourceReader = (this.characterEncoding != null) ?
				new InputStreamReader(sourceStream, this.characterEncoding) : new InputStreamReader(sourceStream);
			return new BufferedReader(sourceReader);
		}
		else {
			return null;
		}
	}

	@Override
	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public int getContentLength() {
		return (this.content != null ? content.length : -1);
	}

	public void setMethod(String method) {
		this.method = method;
	}

	@Override
	public String getMethod() {
		return this.method;
	}

}
