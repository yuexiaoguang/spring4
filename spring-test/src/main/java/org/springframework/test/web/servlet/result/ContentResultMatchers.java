package org.springframework.test.web.servlet.result;

import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.MediaType;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 响应内容断言的工厂.
 *
 * <p>通常通过{@link MockMvcResultMatchers#content}访问此类的实例.
 */
public class ContentResultMatchers {

	private final XmlExpectationsHelper xmlHelper;

	private final JsonExpectationsHelper jsonHelper;


	/**
	 * Use {@link MockMvcResultMatchers#content()}.
	 */
	protected ContentResultMatchers() {
		this.xmlHelper = new XmlExpectationsHelper();
		this.jsonHelper = new JsonExpectationsHelper();
	}


	/**
	 * 断言ServletResponse内容类型.
	 * 给定的内容类型必须完全匹配, 包括类型, 子类型和参数.
	 * 仅检查类型和子类型, 请参阅{@link #contentTypeCompatibleWith(String)}.
	 */
	public ResultMatcher contentType(String contentType) {
		return contentType(MediaType.parseMediaType(contentType));
	}

	/**
	 * 将ServletResponse内容类型解析为MediaType后, 断言.
	 * 给定的内容类型必须完全匹配, 包括类型, 子类型和参数.
	 * 仅检查类型和子类型, 请参阅{@link #contentTypeCompatibleWith(MediaType)}.
	 */
	public ResultMatcher contentType(final MediaType contentType) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String actual = result.getResponse().getContentType();
				assertTrue("Content type not set", actual != null);
				assertEquals("Content type", contentType, MediaType.parseMediaType(actual));
			}
		};
	}

	/**
	 * 断言ServletResponse内容类型与{@link MediaType#isCompatibleWith(MediaType)}定义的给定内容类型兼容.
	 */
	public ResultMatcher contentTypeCompatibleWith(String contentType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(contentType));
	}

	/**
	 * 断言ServletResponse内容类型与{@link MediaType#isCompatibleWith(MediaType)}定义的给定内容类型兼容.
	 */
	public ResultMatcher contentTypeCompatibleWith(final MediaType contentType) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String actual = result.getResponse().getContentType();
				assertTrue("Content type not set", actual != null);
				MediaType actualContentType = MediaType.parseMediaType(actual);
				assertTrue("Content type [" + actual + "] is not compatible with [" + contentType + "]",
						actualContentType.isCompatibleWith(contentType));
			}
		};
	}

	/**
	 * 在ServletResponse中断言字符编码.
	 */
	public ResultMatcher encoding(final String characterEncoding) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				String actual = result.getResponse().getCharacterEncoding();
				assertEquals("Character encoding", characterEncoding, actual);
			}
		};
	}

	/**
	 * 使用Hamcrest {@link Matcher}断言响应正文内容.
	 * <pre class="code">
	 * mockMvc.perform(get("/path"))
	 *   .andExpect(content().string(containsString("text")));
	 * </pre>
	 */
	public ResultMatcher string(final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				assertThat("Response content", result.getResponse().getContentAsString(), matcher);
			}
		};
	}

	/**
	 * 断言响应正文内容为字符串.
	 */
	public ResultMatcher string(final String expectedContent) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				assertEquals("Response content", expectedContent, result.getResponse().getContentAsString());
			}
		};
	}

	/**
	 * 断言响应正文内容为字节数组.
	 */
	public ResultMatcher bytes(final byte[] expectedContent) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				assertEquals("Response content", expectedContent, result.getResponse().getContentAsByteArray());
			}
		};
	}

	/**
	 * 将响应内容和给定字符串解析为XML, 并断言两者是"相似的" - i.e. 它们包含相同的元素和属性, 而不管顺序如何.
	 * <p>使用此匹配器需要<a href="http://xmlunit.sourceforge.net/">XMLUnit<a/>库.
	 * 
	 * @param xmlContent 预期的XML内容
	 */
	public ResultMatcher xml(final String xmlContent) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xmlHelper.assertXmlEqual(xmlContent, content);
			}
		};
	}

	/**
	 * 将响应内容解析为{@link Node}, 并应用给定的Hamcrest {@link Matcher}.
	 */
	public ResultMatcher node(final Matcher<? super Node> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xmlHelper.assertNode(content, matcher);
			}
		};
	}

	/**
	 * 将响应内容解析为{@link DOMSource}并应用给定的Hamcrest {@link Matcher}.
	 */
	public ResultMatcher source(final Matcher<? super Source> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xmlHelper.assertSource(content, matcher);
			}
		};
	}

	/**
	 * 将预期字符串和实际字符串解析为JSON, 并断言两者是"相似的" 
	 * - i.e. 它们包含相同的属性-值对, 而不管格式的宽松检查 (可扩展和非严格的数组排序).
	 * 
	 * @param jsonContent 预期的JSON内容
	 */
	public ResultMatcher json(final String jsonContent) {
		return json(jsonContent, false);
	}

	/**
	 * 将响应内容和给定字符串解析为JSON, 并断言两者是 "相似的" - i.e. 它们包含相同的属性-值对, 而不管格式如何.
	 * <p>可以在两种模式下进行比较, 具体取决于{@code strict}参数值:
	 * <ul>
	 * <li>{@code true}: 严格检查. 不可扩展, 严格的数组排序.</li>
	 * <li>{@code false}: 宽松的检查. 可扩展和非严格的数组排序.</li>
	 * </ul>
	 * <p>使用此匹配器需要<a href="http://jsonassert.skyscreamer.org/">JSONassert<a/>库.
	 * 
	 * @param jsonContent 预期的JSON内容
	 * @param strict 是否严格检查
	 */
	public ResultMatcher json(final String jsonContent, final boolean strict) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonHelper.assertJsonEqual(jsonContent, content, strict);
			}
		};
	}

}
