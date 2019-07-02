package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;

import org.springframework.util.Assert;

/**
 * {@link WebConnection}的实现, 允许委托各种{@code WebConnection}实现.
 *
 * <p>例如, 如果在域{@code code.jquery.com}上托管JavaScript, 则可能需要使用以下内容.
 *
 * <pre class="code">
 * WebClient webClient = new WebClient();
 *
 * MockMvc mockMvc = ...
 * MockMvcWebConnection mockConnection = new MockMvcWebConnection(mockMvc, webClient);
 *
 * WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
 * WebConnection httpConnection = new HttpWebConnection(webClient);
 * WebConnection webConnection = new DelegatingWebConnection(mockConnection, new DelegateWebConnection(cdnMatcher, httpConnection));
 *
 * webClient.setWebConnection(webConnection);
 *
 * WebClient webClient = new WebClient();
 * webClient.setWebConnection(webConnection);
 * </pre>
 */
public final class DelegatingWebConnection implements WebConnection {

	private final List<DelegateWebConnection> connections;

	private final WebConnection defaultConnection;


	public DelegatingWebConnection(WebConnection defaultConnection, List<DelegateWebConnection> connections) {
		Assert.notNull(defaultConnection, "Default WebConnection must not be null");
		Assert.notEmpty(connections, "Connections List must not be empty");
		this.connections = connections;
		this.defaultConnection = defaultConnection;
	}

	public DelegatingWebConnection(WebConnection defaultConnection, DelegateWebConnection... connections) {
		this(defaultConnection, Arrays.asList(connections));
	}


	@Override
	public WebResponse getResponse(WebRequest request) throws IOException {
		for (DelegateWebConnection connection : this.connections) {
			if (connection.getMatcher().matches(request)) {
				return connection.getDelegate().getResponse(request);
			}
		}
		return this.defaultConnection.getResponse(request);
	}

	@Override
	public void close() {
	}


	public static final class DelegateWebConnection {

		private final WebRequestMatcher matcher;

		private final WebConnection delegate;

		public DelegateWebConnection(WebRequestMatcher matcher, WebConnection delegate) {
			this.matcher = matcher;
			this.delegate = delegate;
		}

		private WebRequestMatcher getMatcher() {
			return this.matcher;
		}

		private WebConnection getDelegate() {
			return this.delegate;
		}
	}

}
