package org.springframework.test.web.client.match;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 请求内容{@code RequestMatcher}的工厂.
 * 通常通过{@link MockRestRequestMatchers#content()}访问此类的实例.
 */
public class ContentRequestMatchers {

	private final XmlExpectationsHelper xmlHelper;


	/**
	 * 类构造函数, 不用于直接实例化.
	 * 使用{@link MockRestRequestMatchers#content()}.
	 */
	protected ContentRequestMatchers() {
		this.xmlHelper = new XmlExpectationsHelper();
	}


	/**
	 * 断言请求内容类型为String.
	 */
	public RequestMatcher contentType(String expectedContentType) {
		return contentType(MediaType.parseMediaType(expectedContentType));
	}

	/**
	 * 断言请求内容类型为{@link MediaType}.
	 */
	public RequestMatcher contentType(final MediaType expectedContentType) {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				MediaType actualContentType = request.getHeaders().getContentType();
				assertTrue("Content type not set", actualContentType != null);
				assertEquals("Content type", expectedContentType, actualContentType);
			}
		};
	}

	/**
	 * 断言请求内容类型与{@link MediaType#isCompatibleWith(MediaType)}定义的给定内容类型兼容.
	 */
	public RequestMatcher contentTypeCompatibleWith(String contentType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(contentType));
	}

	/**
	 * 断言请求内容类型与{@link MediaType#isCompatibleWith(MediaType)}定义的给定内容类型兼容.
	 */
	public RequestMatcher contentTypeCompatibleWith(final MediaType contentType) {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				MediaType actualContentType = request.getHeaders().getContentType();
				assertTrue("Content type not set", actualContentType != null);
				assertTrue("Content type [" + actualContentType + "] is not compatible with [" + contentType + "]",
						actualContentType.isCompatibleWith(contentType));
			}
		};
	}

	/**
	 * 获取请求的正文, 作为UTF-8字符串, 并使用给定的{@link Matcher}.
	 */
	public RequestMatcher string(final Matcher<? super String> matcher) {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				assertThat("Request content", mockRequest.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * 获取请求的正文, 作为UTF-8字符串, 并将其与给定的String进行比较.
	 */
	public RequestMatcher string(final String expectedContent) {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				assertEquals("Request content", expectedContent, mockRequest.getBodyAsString());
			}
		};
	}

	/**
	 * 将请求的主体与给定的字节数组进行比较.
	 */
	public RequestMatcher bytes(final byte[] expectedContent) {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				assertEquals("Request content", expectedContent, mockRequest.getBodyAsBytes());
			}
		};
	}

	/**
	 * 将主体解析为表单数据并与给定的{@code MultiValueMap}进行比较.
	 */
	public RequestMatcher formData(final MultiValueMap<String, String> expectedContent) {
		return new RequestMatcher() {
			@Override
			public void match(final ClientHttpRequest request) throws IOException, AssertionError {
				HttpInputMessage inputMessage = new HttpInputMessage() {
					@Override
					public InputStream getBody() throws IOException {
						MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
						return new ByteArrayInputStream(mockRequest.getBodyAsBytes());
					}
					@Override
					public HttpHeaders getHeaders() {
						return request.getHeaders();
					}
				};
				FormHttpMessageConverter converter = new FormHttpMessageConverter();
				assertEquals("Request content", expectedContent, converter.read(null, inputMessage));
			}
		};
	}

	/**
	 * 将请求主体和给定的String解析为XML, 并断言两者是"相似的" - i.e. 它们包含相同的元素和属性, 而不管顺序如何.
	 * <p>使用此匹配器假定<a href="http://xmlunit.sourceforge.net/">XMLUnit<a/>库可用.
	 * 
	 * @param expectedXmlContent 预期的XML内容
	 */
	public RequestMatcher xml(final String expectedXmlContent) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertXmlEqual(expectedXmlContent, request.getBodyAsString());
			}
		};
	}

	/**
	 * 将请求内容解析为{@link Node}, 并应用给定的{@link Matcher}.
	 */
	public RequestMatcher node(final Matcher<? super Node> matcher) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertNode(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * 将请求内容解析为{@link DOMSource}, 并应用给定的{@link Matcher}.
	 * 
	 * @see <a href="http://code.google.com/p/xml-matchers/">http://code.google.com/p/xml-matchers/</a>
	 */
	public RequestMatcher source(final Matcher<? super Source> matcher) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertSource(request.getBodyAsString(), matcher);
			}
		};
	}


	/**
	 * XML {@link RequestMatcher}的抽象基类.
	 */
	private abstract static class AbstractXmlRequestMatcher implements RequestMatcher {

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse expected or actual XML request content: " + ex.getMessage());
			}
		}

		protected abstract void matchInternal(MockClientHttpRequest request) throws Exception;
	}

}

