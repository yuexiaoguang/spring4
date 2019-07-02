package org.springframework.test.web.servlet.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.util.Assert;

/**
 * {@code WebConnectionHtmlUnitDriver}可以为{@link HtmlUnitDriver}实例配置{@link WebConnection}.
 *
 * <p>这很有用, 因为它允许注入{@link org.springframework.test.web.servlet.htmlunit.MockMvcWebConnection MockMvcWebConnection}.
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
	 * 修改提供的{@link WebClient}并保留对它的引用,
	 * 以使其{@link WebConnection}的{@linkplain #getWebConnection 可到达}, 供以后使用.
	 * <p>委托给{@link HtmlUnitDriver#modifyWebClient}获取默认行为, 以及{@link #modifyWebClientInternal}进行进一步自定义.
	 * 
	 * @param webClient 要修改的客户端
	 * 
	 * @return 已修改的客户端
	 */
	@Override
	protected final WebClient modifyWebClient(WebClient webClient) {
		this.webClient = super.modifyWebClient(webClient);
		this.webClient = modifyWebClientInternal(this.webClient);
		return this.webClient;
	}

	/**
	 * 修改提供的{@link WebClient}.
	 * <p>默认实现只是返回提供的未修改的客户端.
	 * <p>子类可以覆盖此方法以自定义{@link HtmlUnitDriver}使用的{@code WebClient}.
	 * 
	 * @param webClient 要修改的客户端
	 * 
	 * @return 已修改的客户端
	 */
	protected WebClient modifyWebClientInternal(WebClient webClient) {
		return webClient;
	}

	/**
	 * 返回当前{@link WebClient}.
	 */
	@Override
	public WebClient getWebClient() {
		return this.webClient;
	}

	/**
	 * 设置要与{@link WebClient}一起使用的{@link WebConnection}.
	 * 
	 * @param webConnection 要使用的{@code WebConnection}
	 */
	public void setWebConnection(WebConnection webConnection) {
		Assert.notNull(webConnection, "WebConnection must not be null");
		this.webClient.setWebConnection(webConnection);
	}

	/**
	 * 访问{@link WebClient}的当前{@link WebConnection}.
	 * 
	 * @return 当前{@code WebConnection}
	 */
	public WebConnection getWebConnection() {
		return this.webClient.getWebConnection();
	}

}
