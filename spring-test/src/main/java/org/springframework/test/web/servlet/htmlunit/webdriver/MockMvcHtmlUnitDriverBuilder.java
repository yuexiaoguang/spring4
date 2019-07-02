package org.springframework.test.web.servlet.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebConnectionBuilderSupport;
import org.springframework.test.web.servlet.htmlunit.WebRequestMatcher;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@code MockMvcHtmlUnitDriverBuilder}简化了{@link HtmlUnitDriver}的构建,
 * 委托给{@link MockMvc}, 并可选择委托给特定请求的实际连接.
 *
 * <p>默认情况下, 驱动程序将委托给{@code MockMvc}处理对{@code localhost}和{@link WebClient}的请求,
 * 以处理任何其他URL (i.e. 执行实际的HTTP请求).
 */
public class MockMvcHtmlUnitDriverBuilder extends MockMvcWebConnectionBuilderSupport<MockMvcHtmlUnitDriverBuilder> {

	private HtmlUnitDriver driver;

	private boolean javascriptEnabled = true;


	protected MockMvcHtmlUnitDriverBuilder(MockMvc mockMvc) {
		super(mockMvc);
	}

	protected MockMvcHtmlUnitDriverBuilder(WebApplicationContext context) {
		super(context);
	}

	protected MockMvcHtmlUnitDriverBuilder(WebApplicationContext context, MockMvcConfigurer configurer) {
		super(context, configurer);
	}


	/**
	 * 根据提供的{@link MockMvc}实例创建一个新的{@code MockMvcHtmlUnitDriverBuilder}.
	 * 
	 * @param mockMvc 要使用的{@code MockMvc}实例 (never {@code null})
	 * 
	 * @return 要自定义的MockMvcHtmlUnitDriverBuilder
	 */
	public static MockMvcHtmlUnitDriverBuilder mockMvcSetup(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		return new MockMvcHtmlUnitDriverBuilder(mockMvc);
	}

	/**
	 * 根据提供的{@link WebApplicationContext}创建一个新的{@code MockMvcHtmlUnitDriverBuilder}.
	 * 
	 * @param context 从中创建{@link MockMvc}实例的{@code WebApplicationContext} (never {@code null})
	 * 
	 * @return 要自定义的MockMvcHtmlUnitDriverBuilder
	 */
	public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		return new MockMvcHtmlUnitDriverBuilder(context);
	}

	/**
	 * 根据提供的{@link WebApplicationContext}和{@link MockMvcConfigurer}创建一个新的{@code MockMvcHtmlUnitDriverBuilder}.
	 * 
	 * @param context 从中创建{@link MockMvc}实例的{@code WebApplicationContext} (never {@code null})
	 * @param configurer 要应用的{@code MockMvcConfigurer} (never {@code null})
	 * 
	 * @return 要自定义的MockMvcHtmlUnitDriverBuilder
	 */
	public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context,
			MockMvcConfigurer configurer) {

		Assert.notNull(context, "WebApplicationContext must not be null");
		Assert.notNull(configurer, "MockMvcConfigurer must not be null");
		return new MockMvcHtmlUnitDriverBuilder(context, configurer);
	}

	/**
	 * 指定是否应启用JavaScript.
	 * <p>默认{@code true}.
	 * 
	 * @param javascriptEnabled {@code true} 如果应启用JavaScript
	 * 
	 * @return 用于进一步自定义的构建器
	 */
	public MockMvcHtmlUnitDriverBuilder javascriptEnabled(boolean javascriptEnabled) {
		this.javascriptEnabled = javascriptEnabled;
		return this;
	}

	/**
	 * 提供此编译器在处理非{@linkplain WebRequestMatcher 匹配}请求时,
	 * 应委托的驱动程序通过此构建器{@linkplain #build 构建}的{@code WebConnectionHtmlUnitDriver}.
	 * 
	 * @param driver 用于不匹配的请求的{@code WebConnectionHtmlUnitDriver} (never {@code null})
	 * 
	 * @return 用于进一步自定义的构建器
	 */
	public MockMvcHtmlUnitDriverBuilder withDelegate(WebConnectionHtmlUnitDriver driver) {
		Assert.notNull(driver, "HtmlUnitDriver must not be null");
		driver.setJavascriptEnabled(this.javascriptEnabled);
		driver.setWebConnection(createConnection(driver.getWebClient()));
		this.driver = driver;
		return this;
	}

	/**
	 * 构建通过此构建器配置的{@link HtmlUnitDriver}.
	 * <p>返回的驱动程序将使用配置的{@link MockMvc}实例处理{@linkplain WebRequestMatcher 匹配}请求,
	 * 并使用委托{@code HtmlUnitDriver}处理所有其他请求.
	 * <p>如果已明确配置{@linkplain #withDelegate 委托}, 则将使用它;
	 * 否则, 将{@link BrowserVersion}设置为{@link BrowserVersion#CHROME CHROME}的默认{@code WebConnectionHtmlUnitDriver}将被配置为委托.
	 * 
	 * @return 要使用的{@code HtmlUnitDriver}
	 */
	public HtmlUnitDriver build() {
		return (this.driver != null ? this.driver :
				withDelegate(new WebConnectionHtmlUnitDriver(BrowserVersion.CHROME)).build());
	}

}
