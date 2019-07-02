package org.springframework.test.web.servlet.htmlunit;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * {@link WebRequestMatcher}, 它允许在{@code WebRequest#getUrl()}的主机和端口上进行匹配.
 *
 * <p>例如, 以下内容将匹配对主机{@code "code.jquery.com"}的任何请求, 而不考虑端口.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com");</pre>
 *
 * <p>也可以传入多个主机. 例如, 以下内容将匹配对主机{@code "code.jquery.com"}或主机{@code "cdn.com"}的任何请求, 而不考虑端口.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com", "cdn.com");</pre>
 *
 * <p>或者, 也可以指定端口. 例如, 以下内容将匹配对主机{@code "code.jquery.com"}以及{@code 80}端口的任何请求.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com:80");</pre>
 *
 * <p>以上{@code cdnMatcher}将匹配{@code "http://code.jquery.com/jquery.js"},
 * 其默认端口为{@code 80}和{@code "http://code.jquery.com:80/jquery.js"}.
 * 但是, 它与 {@code "https://code.jquery.com/jquery.js"}不匹配, 其默认端口为{@code 443}.
 */
public final class HostRequestMatcher implements WebRequestMatcher {

	private final Set<String> hosts = new HashSet<String>();


	/**
	 * 为给定的主机创建一个新的{@code HostRequestMatcher} &mdash;
	 * 例如: {@code "localhost"}, {@code "example.com:443"}, etc.
	 * 
	 * @param hosts 要匹配的主机
	 */
	public HostRequestMatcher(String... hosts) {
		this.hosts.addAll(Arrays.asList(hosts));
	}


	@Override
	public boolean matches(WebRequest request) {
		URL url = request.getUrl();
		String host = url.getHost();

		if (this.hosts.contains(host)) {
			return true;
		}

		int port = url.getPort();
		if (port == -1) {
			port = url.getDefaultPort();
		}
		String hostAndPort = host + ":" + port;

		return this.hosts.contains(hostAndPort);
	}

}
