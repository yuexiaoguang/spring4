package org.springframework.test.web.servlet.htmlunit;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * A {@link WebRequestMatcher} that allows matching on the host and optionally
 * the port of {@code WebRequest#getUrl()}.
 *
 * <p>For example, the following would match any request to the host
 * {@code "code.jquery.com"} without regard for the port.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com");</pre>
 *
 * <p>Multiple hosts can also be passed in. For example, the following would
 * match any request to the host {@code "code.jquery.com"} or the host
 * {@code "cdn.com"} without regard for the port.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com", "cdn.com");</pre>
 *
 * <p>Alternatively, one can also specify the port. For example, the following would match
 * any request to the host {@code "code.jquery.com"} with the port of {@code 80}.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com:80");</pre>
 *
 * <p>The above {@code cdnMatcher} would match {@code "http://code.jquery.com/jquery.js"}
 * which has a default port of {@code 80} and {@code "http://code.jquery.com:80/jquery.js"}.
 * However, it would not match {@code "https://code.jquery.com/jquery.js"}
 * which has a default port of {@code 443}.
 */
public final class HostRequestMatcher implements WebRequestMatcher {

	private final Set<String> hosts = new HashSet<String>();


	/**
	 * Create a new {@code HostRequestMatcher} for the given hosts &mdash;
	 * for example: {@code "localhost"}, {@code "example.com:443"}, etc.
	 * @param hosts the hosts to match on
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
