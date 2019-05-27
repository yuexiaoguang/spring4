package org.springframework.test.web.servlet.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.util.Assert;

/**
 * {@code WebConnectionHtmlUnitDriver} enables configuration of the
 * {@link WebConnection} for an {@link HtmlUnitDriver} instance.
 *
 * <p>This is useful because it allows a
 * {@link org.springframework.test.web.servlet.htmlunit.MockMvcWebConnection
 * MockMvcWebConnection} to be injected.
 */
public class WebConnectionHtmlUnitDriver extends HtmlUnitDriver {

	private WebClient webClient;


	public WebConnectionHtmlUnitDriver() {
	}

	public WebConnectionHtmlUnitDriver(BrowserVersion browserVersion) {
		super(browserVersion);
	}

	public WebConnectionHtmlUnitDriver(boolean enableJavascript) {
		super(enableJavascript);
	}

	public WebConnectionHtmlUnitDriver(Capabilities capabilities) {
		super(capabilities);
	}


	/**
	 * Modify the supplied {@link WebClient} and retain a reference to it so that its
	 * {@link WebConnection} is {@linkplain #getWebConnection accessible} for later use.
	 * <p>Delegates to {@link HtmlUnitDriver#modifyWebClient} for default behavior
	 * and to {@link #modifyWebClientInternal} for further customization.
	 * @param webClient the client to modify
	 * @return the modified client
	 * @see HtmlUnitDriver#modifyWebClient(WebClient)
	 * @see #modifyWebClientInternal(WebClient)
	 */
	@Override
	protected final WebClient modifyWebClient(WebClient webClient) {
		this.webClient = super.modifyWebClient(webClient);
		this.webClient = modifyWebClientInternal(this.webClient);
		return this.webClient;
	}

	/**
	 * Modify the supplied {@link WebClient}.
	 * <p>The default implementation simply returns the supplied client unmodified.
	 * <p>Subclasses can override this method to customize the {@code WebClient}
	 * that the {@link HtmlUnitDriver} uses.
	 * @param webClient the client to modify
	 * @return the modified client
	 */
	protected WebClient modifyWebClientInternal(WebClient webClient) {
		return webClient;
	}

	/**
	 * Return the current {@link WebClient}.
	 * @since 4.3
	 */
	@Override
	public WebClient getWebClient() {
		return this.webClient;
	}

	/**
	 * Set the {@link WebConnection} to be used with the {@link WebClient}.
	 * @param webConnection the {@code WebConnection} to use
	 */
	public void setWebConnection(WebConnection webConnection) {
		Assert.notNull(webConnection, "WebConnection must not be null");
		this.webClient.setWebConnection(webConnection);
	}

	/**
	 * Access the current {@link WebConnection} for the {@link WebClient}.
	 * @return the current {@code WebConnection}
	 */
	public WebConnection getWebConnection() {
		return this.webClient.getWebConnection();
	}

}