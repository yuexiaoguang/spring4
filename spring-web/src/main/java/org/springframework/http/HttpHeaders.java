package org.springframework.http;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * 表示HTTP请求和响应header, 将字符串header名称映射到字符串值列表.
 *
 * <p>除了{@link Map}定义的常规方法之外, 此类还提供以下便捷方法:
 * <ul>
 * <li>{@link #getFirst(String)} 返回与给定header名称关联的第一个值</li>
 * <li>{@link #add(String, String)} 将header值添加到header名称的值列表中</li>
 * <li>{@link #set(String, String)} 将header值设置为单个字符串值</li>
 * </ul>
 *
 * <p>灵感来自{@code com.sun.net.httpserver.Headers}.
 */
public class HttpHeaders implements MultiValueMap<String, String>, Serializable {

	private static final long serialVersionUID = -8578554704772377436L;

	/**
	 * The HTTP {@code Accept} header field name.
	 */
	public static final String ACCEPT = "Accept";
	/**
	 * The HTTP {@code Accept-Charset} header field name.
	 */
	public static final String ACCEPT_CHARSET = "Accept-Charset";
	/**
	 * The HTTP {@code Accept-Encoding} header field name.
	 */
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	/**
	 * The HTTP {@code Accept-Language} header field name.
	 */
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	/**
	 * The HTTP {@code Accept-Ranges} header field name.
	 */
	public static final String ACCEPT_RANGES = "Accept-Ranges";
	/**
	 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	/**
	 * The CORS {@code Access-Control-Allow-Headers} response header field name.
	 */
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	/**
	 * The CORS {@code Access-Control-Allow-Methods} response header field name.
	 */
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	/**
	 * The CORS {@code Access-Control-Allow-Origin} response header field name.
	 */
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	/**
	 * The CORS {@code Access-Control-Expose-Headers} response header field name.
	 */
	public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	/**
	 * The CORS {@code Access-Control-Max-Age} response header field name.
	 */
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	/**
	 * The CORS {@code Access-Control-Request-Headers} request header field name.
	 */
	public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
	/**
	 * The CORS {@code Access-Control-Request-Method} request header field name.
	 */
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	/**
	 * The HTTP {@code Age} header field name.
	 */
	public static final String AGE = "Age";
	/**
	 * The HTTP {@code Allow} header field name.
	 */
	public static final String ALLOW = "Allow";
	/**
	 * The HTTP {@code Authorization} header field name.
	 */
	public static final String AUTHORIZATION = "Authorization";
	/**
	 * The HTTP {@code Cache-Control} header field name.
	 */
	public static final String CACHE_CONTROL = "Cache-Control";
	/**
	 * The HTTP {@code Connection} header field name.
	 */
	public static final String CONNECTION = "Connection";
	/**
	 * The HTTP {@code Content-Encoding} header field name.
	 */
	public static final String CONTENT_ENCODING = "Content-Encoding";
	/**
	 * The HTTP {@code Content-Disposition} header field name.
	 */
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	/**
	 * The HTTP {@code Content-Language} header field name.
	 */
	public static final String CONTENT_LANGUAGE = "Content-Language";
	/**
	 * The HTTP {@code Content-Length} header field name.
	 */
	public static final String CONTENT_LENGTH = "Content-Length";
	/**
	 * The HTTP {@code Content-Location} header field name.
	 */
	public static final String CONTENT_LOCATION = "Content-Location";
	/**
	 * The HTTP {@code Content-Range} header field name.
	 */
	public static final String CONTENT_RANGE = "Content-Range";
	/**
	 * The HTTP {@code Content-Type} header field name.
	 */
	public static final String CONTENT_TYPE = "Content-Type";
	/**
	 * The HTTP {@code Cookie} header field name.
	 */
	public static final String COOKIE = "Cookie";
	/**
	 * The HTTP {@code Date} header field name.
	 */
	public static final String DATE = "Date";
	/**
	 * The HTTP {@code ETag} header field name.
	 */
	public static final String ETAG = "ETag";
	/**
	 * The HTTP {@code Expect} header field name.
	 */
	public static final String EXPECT = "Expect";
	/**
	 * The HTTP {@code Expires} header field name.
	 */
	public static final String EXPIRES = "Expires";
	/**
	 * The HTTP {@code From} header field name.
	 */
	public static final String FROM = "From";
	/**
	 * The HTTP {@code Host} header field name.
	 */
	public static final String HOST = "Host";
	/**
	 * The HTTP {@code If-Match} header field name.
	 */
	public static final String IF_MATCH = "If-Match";
	/**
	 * The HTTP {@code If-Modified-Since} header field name.
	 */
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	/**
	 * The HTTP {@code If-None-Match} header field name.
	 */
	public static final String IF_NONE_MATCH = "If-None-Match";
	/**
	 * The HTTP {@code If-Range} header field name.
	 */
	public static final String IF_RANGE = "If-Range";
	/**
	 * The HTTP {@code If-Unmodified-Since} header field name.
	 */
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	/**
	 * The HTTP {@code Last-Modified} header field name.
	 */
	public static final String LAST_MODIFIED = "Last-Modified";
	/**
	 * The HTTP {@code Link} header field name.
	 */
	public static final String LINK = "Link";
	/**
	 * The HTTP {@code Location} header field name.
	 */
	public static final String LOCATION = "Location";
	/**
	 * The HTTP {@code Max-Forwards} header field name.
	 */
	public static final String MAX_FORWARDS = "Max-Forwards";
	/**
	 * The HTTP {@code Origin} header field name.
	 */
	public static final String ORIGIN = "Origin";
	/**
	 * The HTTP {@code Pragma} header field name.
	 */
	public static final String PRAGMA = "Pragma";
	/**
	 * The HTTP {@code Proxy-Authenticate} header field name.
	 */
	public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
	/**
	 * The HTTP {@code Proxy-Authorization} header field name.
	 */
	public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
	/**
	 * The HTTP {@code Range} header field name.
	 */
	public static final String RANGE = "Range";
	/**
	 * The HTTP {@code Referer} header field name.
	 */
	public static final String REFERER = "Referer";
	/**
	 * The HTTP {@code Retry-After} header field name.
	 */
	public static final String RETRY_AFTER = "Retry-After";
	/**
	 * The HTTP {@code Server} header field name.
	 */
	public static final String SERVER = "Server";
	/**
	 * The HTTP {@code Set-Cookie} header field name.
	 */
	public static final String SET_COOKIE = "Set-Cookie";
	/**
	 * The HTTP {@code Set-Cookie2} header field name.
	 */
	public static final String SET_COOKIE2 = "Set-Cookie2";
	/**
	 * The HTTP {@code TE} header field name.
	 */
	public static final String TE = "TE";
	/**
	 * The HTTP {@code Trailer} header field name.
	 */
	public static final String TRAILER = "Trailer";
	/**
	 * The HTTP {@code Transfer-Encoding} header field name.
	 */
	public static final String TRANSFER_ENCODING = "Transfer-Encoding";
	/**
	 * The HTTP {@code Upgrade} header field name.
	 */
	public static final String UPGRADE = "Upgrade";
	/**
	 * The HTTP {@code User-Agent} header field name.
	 */
	public static final String USER_AGENT = "User-Agent";
	/**
	 * The HTTP {@code Vary} header field name.
	 */
	public static final String VARY = "Vary";
	/**
	 * The HTTP {@code Via} header field name.
	 */
	public static final String VIA = "Via";
	/**
	 * The HTTP {@code Warning} header field name.
	 */
	public static final String WARNING = "Warning";
	/**
	 * The HTTP {@code WWW-Authenticate} header field name.
	 */
	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

	/**
	 * 匹配header中的ETag多个字段值的模式, 例如"If-Match", "If-None-Match".
	 */
	private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	/**
	 * HTTP RFC中指定的日期格式.
	 */
	private static final String[] DATE_FORMATS = new String[] {
			"EEE, dd MMM yyyy HH:mm:ss zzz",
			"EEE, dd-MMM-yy HH:mm:ss zzz",
			"EEE MMM dd HH:mm:ss yyyy"
	};


	private final Map<String, List<String>> headers;


	public HttpHeaders() {
		this(new LinkedCaseInsensitiveMap<List<String>>(8, Locale.ENGLISH), false);
	}

	/**
	 * 可以创建只读的{@code HttpHeader}实例的私有构造函数.
	 */
	private HttpHeaders(Map<String, List<String>> headers, boolean readOnly) {
		if (readOnly) {
			Map<String, List<String>> map =
					new LinkedCaseInsensitiveMap<List<String>>(headers.size(), Locale.ENGLISH);
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				List<String> values = Collections.unmodifiableList(entry.getValue());
				map.put(entry.getKey(), values);
			}
			this.headers = Collections.unmodifiableMap(map);
		}
		else {
			this.headers = headers;
		}
	}


	/**
	 * 设置可接受的{@linkplain MediaType 媒体类型}列表, 由{@code Accept} header指定.
	 */
	public void setAccept(List<MediaType> acceptableMediaTypes) {
		set(ACCEPT, MediaType.toString(acceptableMediaTypes));
	}

	/**
	 * 返回可接受的{@linkplain MediaType 媒体类型}列表, 由{@code Accept} header指定.
	 * <p>未指定可接受的媒体类型时返回空列表.
	 */
	public List<MediaType> getAccept() {
		return MediaType.parseMediaTypes(get(ACCEPT));
	}

	/**
	 * 设置{@code Access-Control-Allow-Credentials}响应 header的值.
	 */
	public void setAccessControlAllowCredentials(boolean allowCredentials) {
		set(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(allowCredentials));
	}

	/**
	 * 返回{@code Access-Control-Allow-Credentials}响应header的值.
	 */
	public boolean getAccessControlAllowCredentials() {
		return Boolean.parseBoolean(getFirst(ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	/**
	 * 设置{@code Access-Control-Allow-Headers}响应header的值.
	 */
	public void setAccessControlAllowHeaders(List<String> allowedHeaders) {
		set(ACCESS_CONTROL_ALLOW_HEADERS, toCommaDelimitedString(allowedHeaders));
	}

	/**
	 * 返回{@code Access-Control-Allow-Headers}响应 header的值.
	 */
	public List<String> getAccessControlAllowHeaders() {
		return getValuesAsList(ACCESS_CONTROL_ALLOW_HEADERS);
	}

	/**
	 * 设置{@code Access-Control-Allow-Methods}响应header的值.
	 */
	public void setAccessControlAllowMethods(List<HttpMethod> allowedMethods) {
		set(ACCESS_CONTROL_ALLOW_METHODS, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * 返回{@code Access-Control-Allow-Methods}响应 header的值.
	 */
	public List<HttpMethod> getAccessControlAllowMethods() {
		List<HttpMethod> result = new ArrayList<HttpMethod>();
		String value = getFirst(ACCESS_CONTROL_ALLOW_METHODS);
		if (value != null) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			for (String token : tokens) {
				HttpMethod resolved = HttpMethod.resolve(token);
				if (resolved != null) {
					result.add(resolved);
				}
			}
		}
		return result;
	}

	/**
	 * 设置{@code Access-Control-Allow-Origin}响应 header的值.
	 */
	public void setAccessControlAllowOrigin(String allowedOrigin) {
		set(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
	}

	/**
	 * 返回{@code Access-Control-Allow-Origin}响应 header的值.
	 */
	public String getAccessControlAllowOrigin() {
		return getFieldValues(ACCESS_CONTROL_ALLOW_ORIGIN);
	}

	/**
	 * 设置{@code Access-Control-Expose-Headers}响应 header的值.
	 */
	public void setAccessControlExposeHeaders(List<String> exposedHeaders) {
		set(ACCESS_CONTROL_EXPOSE_HEADERS, toCommaDelimitedString(exposedHeaders));
	}

	/**
	 * 返回{@code Access-Control-Expose-Headers}响应 header的值.
	 */
	public List<String> getAccessControlExposeHeaders() {
		return getValuesAsList(ACCESS_CONTROL_EXPOSE_HEADERS);
	}

	/**
	 * 设置{@code Access-Control-Max-Age}响应header的值.
	 */
	public void setAccessControlMaxAge(long maxAge) {
		set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge));
	}

	/**
	 * 返回{@code Access-Control-Max-Age}响应 header的值.
	 * <p>当最大时间未知时返回-1.
	 */
	public long getAccessControlMaxAge() {
		String value = getFirst(ACCESS_CONTROL_MAX_AGE);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * 设置{@code Access-Control-Request-Headers}请求header的值.
	 */
	public void setAccessControlRequestHeaders(List<String> requestHeaders) {
		set(ACCESS_CONTROL_REQUEST_HEADERS, toCommaDelimitedString(requestHeaders));
	}

	/**
	 * 返回{@code Access-Control-Request-Headers}请求header的值.
	 */
	public List<String> getAccessControlRequestHeaders() {
		return getValuesAsList(ACCESS_CONTROL_REQUEST_HEADERS);
	}

	/**
	 * 设置{@code Access-Control-Request-Method}请求header的值.
	 */
	public void setAccessControlRequestMethod(HttpMethod requestMethod) {
		set(ACCESS_CONTROL_REQUEST_METHOD, requestMethod.name());
	}

	/**
	 * 返回{@code Access-Control-Request-Method}请求header的值.
	 */
	public HttpMethod getAccessControlRequestMethod() {
		return HttpMethod.resolve(getFirst(ACCESS_CONTROL_REQUEST_METHOD));
	}

	/**
	 * 设置可接受的{@linkplain Charset 字符集}列表, 由{@code Accept-Charset} header指定.
	 */
	public void setAcceptCharset(List<Charset> acceptableCharsets) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<Charset> iterator = acceptableCharsets.iterator(); iterator.hasNext();) {
			Charset charset = iterator.next();
			builder.append(charset.name().toLowerCase(Locale.ENGLISH));
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		set(ACCEPT_CHARSET, builder.toString());
	}

	/**
	 * 返回可接受的{@linkplain Charset 字符集}列表, 由{@code Accept-Charset} header指定.
	 */
	public List<Charset> getAcceptCharset() {
		String value = getFirst(ACCEPT_CHARSET);
		if (value != null) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			List<Charset> result = new ArrayList<Charset>(tokens.length);
			for (String token : tokens) {
				int paramIdx = token.indexOf(';');
				String charsetName;
				if (paramIdx == -1) {
					charsetName = token;
				}
				else {
					charsetName = token.substring(0, paramIdx);
				}
				if (!charsetName.equals("*")) {
					result.add(Charset.forName(charsetName));
				}
			}
			return result;
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * 设置允许的{@link HttpMethod HTTP方法}的集合, 由{@code Allow} header指定.
	 */
	public void setAllow(Set<HttpMethod> allowedMethods) {
		set(ALLOW, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * 返回允许的{@link HttpMethod HTTP方法}的集合, 由{@code Allow} header指定.
	 * <p>未指定允许的方法时返回空集.
	 */
	public Set<HttpMethod> getAllow() {
		String value = getFirst(ALLOW);
		if (!StringUtils.isEmpty(value)) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			List<HttpMethod> result = new ArrayList<HttpMethod>(tokens.length);
			for (String token : tokens) {
				HttpMethod resolved = HttpMethod.resolve(token);
				if (resolved != null) {
					result.add(resolved);
				}
			}
			return EnumSet.copyOf(result);
		}
		else {
			return EnumSet.noneOf(HttpMethod.class);
		}
	}

	/**
	 * 设置{@code Cache-Control} header的值.
	 */
	public void setCacheControl(String cacheControl) {
		set(CACHE_CONTROL, cacheControl);
	}

	/**
	 * 返回{@code Cache-Control} header的值.
	 */
	public String getCacheControl() {
		return getFieldValues(CACHE_CONTROL);
	}

	/**
	 * 设置{@code Connection} header的值.
	 */
	public void setConnection(String connection) {
		set(CONNECTION, connection);
	}

	/**
	 * 设置{@code Connection} header的值.
	 */
	public void setConnection(List<String> connection) {
		set(CONNECTION, toCommaDelimitedString(connection));
	}

	/**
	 * 返回{@code Connection} header的值.
	 */
	public List<String> getConnection() {
		return getValuesAsList(CONNECTION);
	}

	/**
	 * 在创建{@code "multipart/form-data"}请求时设置{@code Content-Disposition} header.
	 * <p>应用程序通常不会直接设置此header, 而是为每个部分准备一个{@code MultiValueMap<String, Object>},
	 * 其中包含一个Object或{@link org.springframework.core.io.Resource}, 然后将其传递给{@code RestTemplate}或{@code WebClient}.
	 * 
	 * @param name 控制名称
	 * @param filename filename (may be {@code null})
	 */
	public void setContentDispositionFormData(String name, String filename) {
		Assert.notNull(name, "'name' must not be null");
		StringBuilder builder = new StringBuilder("form-data; name=\"");
		builder.append(name).append('\"');
		if (filename != null) {
			builder.append("; filename=\"");
			builder.append(filename).append('\"');
		}
		set(CONTENT_DISPOSITION, builder.toString());
	}

	/**
	 * 设置{@code form-data}的{@code Content-Disposition} header, 可选择使用RFC 5987对文件名进行编码.
	 * <p>仅支持US-ASCII, UTF-8 和 ISO-8859-1字符集.
	 * 
	 * @param name 控制名称
	 * @param filename the filename (may be {@code null})
	 * @param charset 用于文件名的字符集 (may be {@code null})
	 * 
	 * @deprecated deprecated in 4.3.11 and removed from 5.0; as per
	 * <a link="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>,
	 * an RFC 5987 style encoding should not be used for multipart/form-data requests.
	 * Furthermore there should be no reason for applications to set this header explicitly; for more details also read
	 * {@link #setContentDispositionFormData(String, String)}
	 */
	@Deprecated
	public void setContentDispositionFormData(String name, String filename, Charset charset) {
		Assert.notNull(name, "'name' must not be null");
		StringBuilder builder = new StringBuilder("form-data; name=\"");
		builder.append(name).append('\"');
		if (filename != null) {
			if (charset == null || charset.name().equals("US-ASCII")) {
				builder.append("; filename=\"");
				builder.append(filename).append('\"');
			}
			else {
				builder.append("; filename*=");
				builder.append(encodeHeaderFieldParam(filename, charset));
			}
		}
		set(CONTENT_DISPOSITION, builder.toString());
	}

	/**
	 * 设置正文的长度, 以字节为单位, 由{@code Content-Length} header指定.
	 */
	public void setContentLength(long contentLength) {
		set(CONTENT_LENGTH, Long.toString(contentLength));
	}

	/**
	 * 返回正文的长度, 以字节为单位, 由{@code Content-Length} header指定.
	 * <p>当内容长度未知时返回-1.
	 */
	public long getContentLength() {
		String value = getFirst(CONTENT_LENGTH);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * 设置正文的{@linkplain MediaType 媒体类型}, 由{@code Content-Type} header指定.
	 */
	public void setContentType(MediaType mediaType) {
		Assert.isTrue(!mediaType.isWildcardType(), "'Content-Type' cannot contain wildcard type '*'");
		Assert.isTrue(!mediaType.isWildcardSubtype(), "'Content-Type' cannot contain wildcard subtype '*'");
		set(CONTENT_TYPE, mediaType.toString());
	}

	/**
	 * 返回正文的{@linkplain MediaType 媒体类型}, 由{@code Content-Type} header指定.
	 * <p>当内容类型未知时返回{@code null}.
	 */
	public MediaType getContentType() {
		String value = getFirst(CONTENT_TYPE);
		return (StringUtils.hasLength(value) ? MediaType.parseMediaType(value) : null);
	}

	/**
	 * 设置创建消息的日期和时间, 由{@code Date} header指定.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数.
	 */
	public void setDate(long date) {
		setDate(DATE, date);
	}

	/**
	 * 返回创建消息的日期和时间, 由{@code Date} header指定.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数. 当日期未知时返回-1.
	 * 
	 * @throws IllegalArgumentException 如果该值无法转换为日期
	 */
	public long getDate() {
		return getFirstDate(DATE);
	}

	/**
	 * 设置正文的实体标签, 由{@code ETag} header指定.
	 */
	public void setETag(String etag) {
		if (etag != null) {
			Assert.isTrue(etag.startsWith("\"") || etag.startsWith("W/"),
					"Invalid ETag: does not start with W/ or \"");
			Assert.isTrue(etag.endsWith("\""), "Invalid ETag: does not end with \"");
		}
		set(ETAG, etag);
	}

	/**
	 * 返回正文的实体标签, 由{@code ETag} header指定.
	 */
	public String getETag() {
		return getFirst(ETAG);
	}

	/**
	 * 设置消息不再有效的日期和时间, 由{@code Expires} header指定.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数.
	 */
	public void setExpires(long expires) {
		setDate(EXPIRES, expires);
	}

	/**
	 * 返回消息不再有效的日期和时间, 由{@code Expires} header指定.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数. 当日期未知时返回 -1.
	 */
	public long getExpires() {
		return getFirstDate(EXPIRES, false);
	}

	/**
	 * 设置{@code If-Match} header的值.
	 */
	public void setIfMatch(String ifMatch) {
		set(IF_MATCH, ifMatch);
	}

	/**
	 * 设置{@code If-Match} header的值.
	 */
	public void setIfMatch(List<String> ifMatchList) {
		set(IF_MATCH, toCommaDelimitedString(ifMatchList));
	}

	/**
	 * 返回{@code If-Match} header的值.
	 */
	public List<String> getIfMatch() {
		return getETagValuesAsList(IF_MATCH);
	}

	/**
	 * 设置{@code If-Modified-Since} header的值.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数.
	 */
	public void setIfModifiedSince(long ifModifiedSince) {
		setDate(IF_MODIFIED_SINCE, ifModifiedSince);
	}

	/**
	 * 返回{@code If-Modified-Since} header的值.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数. 当日期未知时返回 -1.
	 */
	public long getIfModifiedSince() {
		return getFirstDate(IF_MODIFIED_SINCE, false);
	}

	/**
	 * 设置{@code If-None-Match} header的值.
	 */
	public void setIfNoneMatch(String ifNoneMatch) {
		set(IF_NONE_MATCH, ifNoneMatch);
	}

	/**
	 * 设置{@code If-None-Match} header的值.
	 */
	public void setIfNoneMatch(List<String> ifNoneMatchList) {
		set(IF_NONE_MATCH, toCommaDelimitedString(ifNoneMatchList));
	}

	/**
	 * 返回{@code If-None-Match} header的值.
	 */
	public List<String> getIfNoneMatch() {
		return getETagValuesAsList(IF_NONE_MATCH);
	}

	/**
	 * 设置{@code If-Unmodified-Since} header的值.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数.
	 */
	public void setIfUnmodifiedSince(long ifUnmodifiedSince) {
		setDate(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
	}

	/**
	 * 返回{@code If-Unmodified-Since} header的值.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数. 当日期未知时返回 -1.
	 */
	public long getIfUnmodifiedSince() {
		return getFirstDate(IF_UNMODIFIED_SINCE, false);
	}

	/**
	 * 设置资源上次更改的时间, 由{@code Last-Modified} header指定.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数.
	 */
	public void setLastModified(long lastModified) {
		setDate(LAST_MODIFIED, lastModified);
	}

	/**
	 * 返回资源上次更改的时间, 由{@code Last-Modified} header指定.
	 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数. 当日期未知时返回 -1.
	 */
	public long getLastModified() {
		return getFirstDate(LAST_MODIFIED, false);
	}

	/**
	 * 设置资源的位置, 由{@code Location} header指定.
	 */
	public void setLocation(URI location) {
		set(LOCATION, location.toASCIIString());
	}

	/**
	 * 返回{@code Location} header指定的资源的位置.
	 * <p>当位置未知时返回{@code null}.
	 */
	public URI getLocation() {
		String value = getFirst(LOCATION);
		return (value != null ? URI.create(value) : null);
	}

	/**
	 * 设置{@code Origin} header的值.
	 */
	public void setOrigin(String origin) {
		set(ORIGIN, origin);
	}

	/**
	 * 返回{@code Origin} header的值.
	 */
	public String getOrigin() {
		return getFirst(ORIGIN);
	}

	/**
	 * 设置{@code Pragma} header的值.
	 */
	public void setPragma(String pragma) {
		set(PRAGMA, pragma);
	}

	/**
	 * 返回{@code Pragma} header的值.
	 */
	public String getPragma() {
		return getFirst(PRAGMA);
	}

	/**
	 * 设置{@code Range} header的值.
	 */
	public void setRange(List<HttpRange> ranges) {
		String value = HttpRange.toString(ranges);
		set(RANGE, value);
	}

	/**
	 * 返回{@code Range} header的值.
	 * <p>范围未知时返回空列表.
	 */
	public List<HttpRange> getRange() {
		String value = getFirst(RANGE);
		return HttpRange.parseRanges(value);
	}

	/**
	 * 设置{@code Upgrade} header的值.
	 */
	public void setUpgrade(String upgrade) {
		set(UPGRADE, upgrade);
	}

	/**
	 * 返回{@code Upgrade} header的值.
	 */
	public String getUpgrade() {
		return getFirst(UPGRADE);
	}

	/**
	 * 根据这些请求header的值, 设置响应受内容协商和差异影响的请求header的(e.g. "Accept-Language")名称.
	 * 
	 * @param requestHeaders 请求header的名称
	 */
	public void setVary(List<String> requestHeaders) {
		set(VARY, toCommaDelimitedString(requestHeaders));
	}

	/**
	 * 根据内容协商返回请求header的名称.
	 */
	public List<String> getVary() {
		return getValuesAsList(VARY);
	}

	/**
	 * 在使用模式{@code "EEE, dd MMM yyyy HH:mm:ss zzz"}将给定日期格式化为字符串后, 在给定header名称下设置.
	 * 相当于{@link #set(String, String)}, 但确是date header.
	 */
	public void setDate(String headerName, long date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMATS[0], Locale.US);
		dateFormat.setTimeZone(GMT);
		set(headerName, dateFormat.format(new Date(date)));
	}

	/**
	 * 将给定header名称的第一个header值解析为日期, 如果没有值, 则返回-1,
	 * 如果无法将值解析为日期, 则引发{@link IllegalArgumentException}.
	 * 
	 * @param headerName header名称
	 * 
	 * @return 解析的date header, 或 -1
	 */
	public long getFirstDate(String headerName) {
		return getFirstDate(headerName, true);
	}

	/**
	 * 将给定header名称的第一个值解析为日期, 如果没有值则返回-1, 或者如果值无效 (如果{@code rejectInvalid=false}),
	 * 或者如果值无法解析为日期, 则抛出 {@link IllegalArgumentException}.
	 * 
	 * @param headerName header名称
	 * @param rejectInvalid 是否使用{@link IllegalArgumentException} ({@code true})拒绝无效值, 或者在这种情况下返回-1 ({@code false})
	 * 
	 * @return 解析后的日期header, 或 -1
	 */
	private long getFirstDate(String headerName, boolean rejectInvalid) {
		String headerValue = getFirst(headerName);
		if (headerValue == null) {
			// 根本没有发送header值
			return -1;
		}
		if (headerValue.length() >= 3) {
			// "0" 或 "-1" 之类的值永远不会是有效的HTTP日期 header...
			// 解析足够长的值.
			for (String dateFormat : DATE_FORMATS) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
				simpleDateFormat.setTimeZone(GMT);
				try {
					return simpleDateFormat.parse(headerValue).getTime();
				}
				catch (ParseException ex) {
					// ignore
				}
			}
		}
		if (rejectInvalid) {
			throw new IllegalArgumentException("Cannot parse date value \"" + headerValue +
					"\" for \"" + headerName + "\" header");
		}
		return -1;
	}

	/**
	 * 返回给定header名称的所有值, 即使此header已多次设置也是如此.
	 * 
	 * @param headerName the header name
	 * 
	 * @return 所有关联的值
	 */
	public List<String> getValuesAsList(String headerName) {
		List<String> values = get(headerName);
		if (values != null) {
			List<String> result = new ArrayList<String>();
			for (String value : values) {
				if (value != null) {
					String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
					for (String token : tokens) {
						result.add(token);
					}
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	/**
	 * 从ETag header的字段值中检索组合结果.
	 * 
	 * @param headerName header名称
	 * 
	 * @return 组合的结果
	 */
	protected List<String> getETagValuesAsList(String headerName) {
		List<String> values = get(headerName);
		if (values != null) {
			List<String> result = new ArrayList<String>();
			for (String value : values) {
				if (value != null) {
					Matcher matcher = ETAG_HEADER_VALUE_PATTERN.matcher(value);
					while (matcher.find()) {
						if ("*".equals(matcher.group())) {
							result.add(matcher.group());
						}
						else {
							result.add(matcher.group(1));
						}
					}
					if (result.isEmpty()) {
						throw new IllegalArgumentException(
								"Could not parse header '" + headerName + "' with value '" + value + "'");
					}
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	/**
	 * 从多值header的字段值中检索组合结果.
	 * 
	 * @param headerName the header name
	 * 
	 * @return 组合的结果
	 */
	protected String getFieldValues(String headerName) {
		List<String> headerValues = get(headerName);
		return (headerValues != null ? toCommaDelimitedString(headerValues) : null);
	}

	/**
	 * 将给定的header值列表转换为逗号分隔的结果.
	 * 
	 * @param headerValues header值的列表
	 * 
	 * @return 以逗号分隔的组合结果
	 */
	protected String toCommaDelimitedString(List<String> headerValues) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> it = headerValues.iterator(); it.hasNext(); ) {
			String val = it.next();
			builder.append(val);
			if (it.hasNext()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}


	// MultiValueMap implementation

	/**
	 * 返回给定header名称的第一个值.
	 * 
	 * @param headerName header名称
	 * 
	 * @return 第一个值, 或{@code null}
	 */
	@Override
	public String getFirst(String headerName) {
		List<String> headerValues = this.headers.get(headerName);
		return (headerValues != null ? headerValues.get(0) : null);
	}

	/**
	 * 在给定名称下添加给定的单个值.
	 * 
	 * @param headerName header名称
	 * @param headerValue header值
	 * 
	 * @throws UnsupportedOperationException 如果不支持添加header
	 */
	@Override
	public void add(String headerName, String headerValue) {
		List<String> headerValues = this.headers.get(headerName);
		if (headerValues == null) {
			headerValues = new LinkedList<String>();
			this.headers.put(headerName, headerValues);
		}
		headerValues.add(headerValue);
	}

	/**
	 * 在给定名称下设置给定的单个值.
	 * 
	 * @param headerName header名称
	 * @param headerValue header值
	 * 
	 * @throws UnsupportedOperationException 如果不支持添加header
	 */
	@Override
	public void set(String headerName, String headerValue) {
		List<String> headerValues = new LinkedList<String>();
		headerValues.add(headerValue);
		this.headers.put(headerName, headerValues);
	}

	@Override
	public void setAll(Map<String, String> values) {
		for (Entry<String, String> entry : values.entrySet()) {
			set(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		LinkedHashMap<String, String> singleValueMap = new LinkedHashMap<String,String>(this.headers.size());
		for (Entry<String, List<String>> entry : this.headers.entrySet()) {
			singleValueMap.put(entry.getKey(), entry.getValue().get(0));
		}
		return singleValueMap;
	}


	// Map implementation

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		this.headers.putAll(map);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HttpHeaders)) {
			return false;
		}
		HttpHeaders otherHeaders = (HttpHeaders) other;
		return this.headers.equals(otherHeaders.headers);
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return this.headers.toString();
	}


	/**
	 * 返回只能读取但不能写入的{@code HttpHeaders}对象.
	 */
	public static HttpHeaders readOnlyHttpHeaders(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		return new HttpHeaders(headers, true);
	}

	/**
	 * 按照RFC 5987中的描述对给定的header字段参数进行编码.
	 * 
	 * @param input header字段参数
	 * @param charset header字段参数字符串的字符集
	 * 
	 * @return 编码的header字段参数
	 */
	static String encodeHeaderFieldParam(String input, Charset charset) {
		Assert.notNull(input, "Input String should not be null");
		Assert.notNull(charset, "Charset should not be null");
		if (charset.name().equals("US-ASCII")) {
			return input;
		}
		Assert.isTrue(charset.name().equals("UTF-8") || charset.name().equals("ISO-8859-1"),
				"Charset should be UTF-8 or ISO-8859-1");
		byte[] source = input.getBytes(charset);
		int len = source.length;
		StringBuilder sb = new StringBuilder(len << 1);
		sb.append(charset.name());
		sb.append("''");
		for (byte b : source) {
			if (isRFC5987AttrChar(b)) {
				sb.append((char) b);
			}
			else {
				sb.append('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				sb.append(hex1);
				sb.append(hex2);
			}
		}
		return sb.toString();
	}

	private static boolean isRFC5987AttrChar(byte c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
				c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
				c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
	}

}
