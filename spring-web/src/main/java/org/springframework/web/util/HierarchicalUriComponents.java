package org.springframework.web.util;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 分层URI的{@link UriComponents}的扩展.
 */
@SuppressWarnings("serial")
final class HierarchicalUriComponents extends UriComponents {

	private static final char PATH_DELIMITER = '/';

	private static final String PATH_DELIMITER_STRING = "/";


	private final String userInfo;

	private final String host;

	private final String port;

	private final PathComponent path;

	private final MultiValueMap<String, String> queryParams;

	private final boolean encoded;


	/**
	 * 所有参数都是可选的, 可以是{@code null}.
	 * 
	 * @param scheme the scheme
	 * @param userInfo 用户信息
	 * @param host 主机
	 * @param port 端口
	 * @param path 路径
	 * @param queryParams 查询参数
	 * @param fragment 片段
	 * @param encoded 组件是否已编码
	 * @param verify 是否需要检查组件是否存在非法字符
	 */
	HierarchicalUriComponents(String scheme, String userInfo, String host, String port,
			PathComponent path, MultiValueMap<String, String> queryParams,
			String fragment, boolean encoded, boolean verify) {

		super(scheme, fragment);
		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.path = (path != null ? path : NULL_PATH_COMPONENT);
		this.queryParams = CollectionUtils.unmodifiableMultiValueMap(
				queryParams != null ? queryParams : new LinkedMultiValueMap<String, String>(0));
		this.encoded = encoded;

		if (verify) {
			verify();
		}
	}


	// Component getters

	@Override
	public String getSchemeSpecificPart() {
		return null;
	}

	@Override
	public String getUserInfo() {
		return this.userInfo;
	}

	@Override
	public String getHost() {
		return this.host;
	}

	@Override
	public int getPort() {
		if (this.port == null) {
			return -1;
		}
		else if (this.port.contains("{")) {
			throw new IllegalStateException(
					"The port contains a URI variable but has not been expanded yet: " + this.port);
		}
		return Integer.parseInt(this.port);
	}

	@Override
	public String getPath() {
		return this.path.getPath();
	}

	@Override
	public List<String> getPathSegments() {
		return this.path.getPathSegments();
	}

	@Override
	public String getQuery() {
		if (!this.queryParams.isEmpty()) {
			StringBuilder queryBuilder = new StringBuilder();
			for (Map.Entry<String, List<String>> entry : this.queryParams.entrySet()) {
				String name = entry.getKey();
				List<String> values = entry.getValue();
				if (CollectionUtils.isEmpty(values)) {
					if (queryBuilder.length() != 0) {
						queryBuilder.append('&');
					}
					queryBuilder.append(name);
				}
				else {
					for (Object value : values) {
						if (queryBuilder.length() != 0) {
							queryBuilder.append('&');
						}
						queryBuilder.append(name);
						if (value != null) {
							queryBuilder.append('=').append(value.toString());
						}
					}
				}
			}
			return queryBuilder.toString();
		}
		else {
			return null;
		}
	}

	/**
	 * 返回查询参数的映射. 如果未设置任何查询, 则为空.
	 */
	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return this.queryParams;
	}


	// Encoding

	/**
	 * 使用特定的编码规则对所有URI组件进行编码, 并将结果作为新的{@code UriComponents}实例返回.
	 * 
	 * @param encoding 值的编码
	 * 
	 * @return 编码的uri组件
	 * @throws UnsupportedEncodingException 如果不支持给定的编码
	 */
	@Override
	public HierarchicalUriComponents encode(String encoding) throws UnsupportedEncodingException {
		if (this.encoded) {
			return this;
		}
		Assert.hasLength(encoding, "Encoding must not be empty");
		String schemeTo = encodeUriComponent(getScheme(), encoding, Type.SCHEME);
		String userInfoTo = encodeUriComponent(this.userInfo, encoding, Type.USER_INFO);
		String hostTo = encodeUriComponent(this.host, encoding, getHostType());
		PathComponent pathTo = this.path.encode(encoding);
		MultiValueMap<String, String> paramsTo = encodeQueryParams(encoding);
		String fragmentTo = encodeUriComponent(getFragment(), encoding, Type.FRAGMENT);
		return new HierarchicalUriComponents(schemeTo, userInfoTo, hostTo, this.port,
				pathTo, paramsTo, fragmentTo, true, false);
	}

	private MultiValueMap<String, String> encodeQueryParams(String encoding) throws UnsupportedEncodingException {
		int size = this.queryParams.size();
		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(size);
		for (Map.Entry<String, List<String>> entry : this.queryParams.entrySet()) {
			String name = encodeUriComponent(entry.getKey(), encoding, Type.QUERY_PARAM);
			List<String> values = new ArrayList<String>(entry.getValue().size());
			for (String value : entry.getValue()) {
				values.add(encodeUriComponent(value, encoding, Type.QUERY_PARAM));
			}
			result.put(name, values);
		}
		return result;
	}

	/**
	 * 使用给定组件和给定选项指定的规则编码给定的源.
	 * 
	 * @param source 源字符串
	 * @param encoding 源String的编码
	 * @param type 源的URI组件
	 * 
	 * @return 编码后的URI
	 * @throws IllegalArgumentException 当给定值不是有效的URI组件时
	 */
	static String encodeUriComponent(String source, String encoding, Type type) throws UnsupportedEncodingException {
		if (source == null) {
			return null;
		}
		Assert.hasLength(encoding, "Encoding must not be empty");
		byte[] bytes = encodeBytes(source.getBytes(encoding), type);
		return new String(bytes, "US-ASCII");
	}

	private static byte[] encodeBytes(byte[] source, Type type) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(type, "Type must not be null");
		ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length);
		for (byte b : source) {
			if (b < 0) {
				b += 256;
			}
			if (type.isAllowed(b)) {
				bos.write(b);
			}
			else {
				bos.write('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				bos.write(hex1);
				bos.write(hex2);
			}
		}
		return bos.toByteArray();
	}

	private Type getHostType() {
		return (this.host != null && this.host.startsWith("[") ? Type.HOST_IPV6 : Type.HOST_IPV4);
	}


	// Verifying

	/**
	 * 验证所有URI组件以确定它们是否包含任何非法字符, 如果是, 则抛出{@code IllegalArgumentException}.
	 * 
	 * @throws IllegalArgumentException 如果任何组件有非法字符
	 */
	private void verify() {
		if (!this.encoded) {
			return;
		}
		verifyUriComponent(getScheme(), Type.SCHEME);
		verifyUriComponent(this.userInfo, Type.USER_INFO);
		verifyUriComponent(this.host, getHostType());
		this.path.verify();
		for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
			verifyUriComponent(entry.getKey(), Type.QUERY_PARAM);
			for (String value : entry.getValue()) {
				verifyUriComponent(value, Type.QUERY_PARAM);
			}
		}
		verifyUriComponent(getFragment(), Type.FRAGMENT);
	}

	private static void verifyUriComponent(String source, Type type) {
		if (source == null) {
			return;
		}
		int length = source.length();
		for (int i = 0; i < length; i++) {
			char ch = source.charAt(i);
			if (ch == '%') {
				if ((i + 2) < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" +
								source.substring(i) + "\"");
					}
					i += 2;
				}
				else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" +
							source.substring(i) + "\"");
				}
			}
			else if (!type.isAllowed(ch)) {
				throw new IllegalArgumentException("Invalid character '" + ch + "' for " +
						type.name() + " in \"" + source + "\"");
			}
		}
	}


	// Expanding

	@Override
	protected HierarchicalUriComponents expandInternal(UriTemplateVariables uriVariables) {
		Assert.state(!this.encoded, "Cannot expand an already encoded UriComponents object");

		String schemeTo = expandUriComponent(getScheme(), uriVariables);
		String userInfoTo = expandUriComponent(this.userInfo, uriVariables);
		String hostTo = expandUriComponent(this.host, uriVariables);
		String portTo = expandUriComponent(this.port, uriVariables);
		PathComponent pathTo = this.path.expand(uriVariables);
		MultiValueMap<String, String> paramsTo = expandQueryParams(uriVariables);
		String fragmentTo = expandUriComponent(getFragment(), uriVariables);

		return new HierarchicalUriComponents(schemeTo, userInfoTo, hostTo, portTo,
				pathTo, paramsTo, fragmentTo, false, false);
	}

	private MultiValueMap<String, String> expandQueryParams(UriTemplateVariables variables) {
		int size = this.queryParams.size();
		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(size);
		variables = new QueryUriTemplateVariables(variables);
		for (Map.Entry<String, List<String>> entry : this.queryParams.entrySet()) {
			String name = expandUriComponent(entry.getKey(), variables);
			List<String> values = new ArrayList<String>(entry.getValue().size());
			for (String value : entry.getValue()) {
				values.add(expandUriComponent(value, variables));
			}
			result.put(name, values);
		}
		return result;
	}

	/**
	 * 规范化路径删除序列, 例如"path/..".
	 */
	@Override
	public UriComponents normalize() {
		String normalizedPath = StringUtils.cleanPath(getPath());
		return new HierarchicalUriComponents(getScheme(), this.userInfo, this.host,
				this.port, new FullPathComponent(normalizedPath), this.queryParams,
				getFragment(), this.encoded, false);
	}


	// Other functionality

	/**
	 * 从此{@code UriComponents}实例返回URI字符串.
	 */
	@Override
	public String toUriString() {
		StringBuilder uriBuilder = new StringBuilder();
		if (getScheme() != null) {
			uriBuilder.append(getScheme());
			uriBuilder.append(':');
		}
		if (this.userInfo != null || this.host != null) {
			uriBuilder.append("//");
			if (this.userInfo != null) {
				uriBuilder.append(this.userInfo);
				uriBuilder.append('@');
			}
			if (this.host != null) {
				uriBuilder.append(host);
			}
			if (getPort() != -1) {
				uriBuilder.append(':');
				uriBuilder.append(port);
			}
		}
		String path = getPath();
		if (StringUtils.hasLength(path)) {
			if (uriBuilder.length() != 0 && path.charAt(0) != PATH_DELIMITER) {
				uriBuilder.append(PATH_DELIMITER);
			}
			uriBuilder.append(path);
		}
		String query = getQuery();
		if (query != null) {
			uriBuilder.append('?');
			uriBuilder.append(query);
		}
		if (getFragment() != null) {
			uriBuilder.append('#');
			uriBuilder.append(getFragment());
		}
		return uriBuilder.toString();
	}

	/**
	 * 从此{@code UriComponents}实例返回{@code URI}.
	 */
	@Override
	public URI toUri() {
		try {
			if (this.encoded) {
				return new URI(toString());
			}
			else {
				String path = getPath();
				if (StringUtils.hasLength(path) && path.charAt(0) != PATH_DELIMITER) {
					// 如果在它之前存在某些东西, 则只在路径分隔符前加上前缀
					if (getScheme() != null || getUserInfo() != null || getHost() != null || getPort() != -1) {
						path = PATH_DELIMITER + path;
					}
				}
				return new URI(getScheme(), getUserInfo(), getHost(), getPort(), path, getQuery(),
						getFragment());
			}
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		builder.scheme(getScheme());
		builder.userInfo(getUserInfo());
		builder.host(getHost());
		builder.port(getPort());
		builder.replacePath("");
		this.path.copyToUriComponentsBuilder(builder);
		builder.replaceQueryParams(getQueryParams());
		builder.fragment(getFragment());
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof HierarchicalUriComponents)) {
			return false;
		}
		HierarchicalUriComponents other = (HierarchicalUriComponents) obj;
		return ObjectUtils.nullSafeEquals(getScheme(), other.getScheme()) &&
				ObjectUtils.nullSafeEquals(getUserInfo(), other.getUserInfo()) &&
				ObjectUtils.nullSafeEquals(getHost(), other.getHost()) &&
				getPort() == other.getPort() &&
				this.path.equals(other.path) &&
				this.queryParams.equals(other.queryParams) &&
				ObjectUtils.nullSafeEquals(getFragment(), other.getFragment());
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(getScheme());
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.userInfo);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.host);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.port);
		result = 31 * result + this.path.hashCode();
		result = 31 * result + this.queryParams.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(getFragment());
		return result;
	}


	// Nested types

	/**
	 * 用于标识每个URI组件允许的字符的枚举.
	 * <p>包含指示给定字符在特定URI组件中是否有效的方法.
	 */
	enum Type {

		SCHEME {
			@Override
			public boolean isAllowed(int c) {
				return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
			}
		},
		AUTHORITY {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
			}
		},
		USER_INFO {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || ':' == c;
			}
		},
		HOST_IPV4 {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c);
			}
		},
		HOST_IPV6 {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || '[' == c || ']' == c || ':' == c;
			}
		},
		PORT {
			@Override
			public boolean isAllowed(int c) {
				return isDigit(c);
			}
		},
		PATH {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c;
			}
		},
		PATH_SEGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c);
			}
		},
		QUERY {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c || '?' == c;
			}
		},
		QUERY_PARAM {
			@Override
			public boolean isAllowed(int c) {
				if ('=' == c || '+' == c || '&' == c) {
					return false;
				}
				else {
					return isPchar(c) || '/' == c || '?' == c;
				}
			}
		},
		FRAGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c || '?' == c;
			}
		},
		URI {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c);
			}
		};

		/**
		 * 指示此URI组件中是否允许给定字符.
		 * 
		 * @return {@code true} 如果允许该字符; 否则{@code false}
		 */
		public abstract boolean isAllowed(int c);

		/**
		 * 指示给定字符是否在{@code ALPHA}集中.
		 */
		protected boolean isAlpha(int c) {
			return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
		}

		/**
		 * 指示给定字符是否在{@code DIGIT}集中.
		 */
		protected boolean isDigit(int c) {
			return (c >= '0' && c <= '9');
		}

		/**
		 * 指示给定字符是否在{@code gen-delims}集中.
		 */
		protected boolean isGenericDelimiter(int c) {
			return (':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c);
		}

		/**
		 * 指示给定字符是否在{@code sub-delims}集中.
		 */
		protected boolean isSubDelimiter(int c) {
			return ('!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
					',' == c || ';' == c || '=' == c);
		}

		/**
		 * 指示给定字符是否在{@code reserved}集中.
		 */
		protected boolean isReserved(int c) {
			return (isGenericDelimiter(c) || isSubDelimiter(c));
		}

		/**
		 * 指示给定字符是否在{@code unreserved}集中.
		 */
		protected boolean isUnreserved(int c) {
			return (isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c);
		}

		/**
		 * 指示给定字符是否在{@code pchar}集中.
		 */
		protected boolean isPchar(int c) {
			return (isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c);
		}
	}


	/**
	 * 定义路径 (分段)的约定.
	 */
	interface PathComponent extends Serializable {

		String getPath();

		List<String> getPathSegments();

		PathComponent encode(String encoding) throws UnsupportedEncodingException;

		void verify();

		PathComponent expand(UriTemplateVariables uriVariables);

		void copyToUriComponentsBuilder(UriComponentsBuilder builder);
	}


	/**
	 * 表示由String支持的路径.
	 */
	static final class FullPathComponent implements PathComponent {

		private final String path;

		public FullPathComponent(String path) {
			this.path = path;
		}

		@Override
		public String getPath() {
			return this.path;
		}

		@Override
		public List<String> getPathSegments() {
			String[] segments = StringUtils.tokenizeToStringArray(getPath(), PATH_DELIMITER_STRING);
			if (segments == null) {
				return Collections.emptyList();
			}
			return Collections.unmodifiableList(Arrays.asList(segments));
		}

		@Override
		public PathComponent encode(String encoding) throws UnsupportedEncodingException {
			String encodedPath = encodeUriComponent(getPath(), encoding, Type.PATH);
			return new FullPathComponent(encodedPath);
		}

		@Override
		public void verify() {
			verifyUriComponent(getPath(), Type.PATH);
		}

		@Override
		public PathComponent expand(UriTemplateVariables uriVariables) {
			String expandedPath = expandUriComponent(getPath(), uriVariables);
			return new FullPathComponent(expandedPath);
		}

		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
			builder.path(getPath());
		}

		@Override
		public boolean equals(Object obj) {
			return (this == obj || (obj instanceof FullPathComponent &&
					ObjectUtils.nullSafeEquals(getPath(), ((FullPathComponent) obj).getPath())));
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(getPath());
		}
	}


	/**
	 * 表示由字符串列表支持的路径 (i.e. 路径分段).
	 */
	static final class PathSegmentComponent implements PathComponent {

		private final List<String> pathSegments;

		public PathSegmentComponent(List<String> pathSegments) {
			Assert.notNull(pathSegments, "List must not be null");
			this.pathSegments = Collections.unmodifiableList(new ArrayList<String>(pathSegments));
		}

		@Override
		public String getPath() {
			StringBuilder pathBuilder = new StringBuilder();
			pathBuilder.append(PATH_DELIMITER);
			for (Iterator<String> iterator = this.pathSegments.iterator(); iterator.hasNext(); ) {
				String pathSegment = iterator.next();
				pathBuilder.append(pathSegment);
				if (iterator.hasNext()) {
					pathBuilder.append(PATH_DELIMITER);
				}
			}
			return pathBuilder.toString();
		}

		@Override
		public List<String> getPathSegments() {
			return this.pathSegments;
		}

		@Override
		public PathComponent encode(String encoding) throws UnsupportedEncodingException {
			List<String> pathSegments = getPathSegments();
			List<String> encodedPathSegments = new ArrayList<String>(pathSegments.size());
			for (String pathSegment : pathSegments) {
				String encodedPathSegment = encodeUriComponent(pathSegment, encoding, Type.PATH_SEGMENT);
				encodedPathSegments.add(encodedPathSegment);
			}
			return new PathSegmentComponent(encodedPathSegments);
		}

		@Override
		public void verify() {
			for (String pathSegment : getPathSegments()) {
				verifyUriComponent(pathSegment, Type.PATH_SEGMENT);
			}
		}

		@Override
		public PathComponent expand(UriTemplateVariables uriVariables) {
			List<String> pathSegments = getPathSegments();
			List<String> expandedPathSegments = new ArrayList<String>(pathSegments.size());
			for (String pathSegment : pathSegments) {
				String expandedPathSegment = expandUriComponent(pathSegment, uriVariables);
				expandedPathSegments.add(expandedPathSegment);
			}
			return new PathSegmentComponent(expandedPathSegments);
		}

		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
			builder.pathSegment(StringUtils.toStringArray(getPathSegments()));
		}

		@Override
		public boolean equals(Object obj) {
			return (this == obj || (obj instanceof PathSegmentComponent &&
					getPathSegments().equals(((PathSegmentComponent) obj).getPathSegments())));
		}

		@Override
		public int hashCode() {
			return getPathSegments().hashCode();
		}
	}


	/**
	 * 表示PathComponents的集合.
	 */
	static final class PathComponentComposite implements PathComponent {

		private final List<PathComponent> pathComponents;

		public PathComponentComposite(List<PathComponent> pathComponents) {
			Assert.notNull(pathComponents, "PathComponent List must not be null");
			this.pathComponents = pathComponents;
		}

		@Override
		public String getPath() {
			StringBuilder pathBuilder = new StringBuilder();
			for (PathComponent pathComponent : this.pathComponents) {
				pathBuilder.append(pathComponent.getPath());
			}
			return pathBuilder.toString();
		}

		@Override
		public List<String> getPathSegments() {
			List<String> result = new ArrayList<String>();
			for (PathComponent pathComponent : this.pathComponents) {
				result.addAll(pathComponent.getPathSegments());
			}
			return result;
		}

		@Override
		public PathComponent encode(String encoding) throws UnsupportedEncodingException {
			List<PathComponent> encodedComponents = new ArrayList<PathComponent>(this.pathComponents.size());
			for (PathComponent pathComponent : this.pathComponents) {
				encodedComponents.add(pathComponent.encode(encoding));
			}
			return new PathComponentComposite(encodedComponents);
		}

		@Override
		public void verify() {
			for (PathComponent pathComponent : this.pathComponents) {
				pathComponent.verify();
			}
		}

		@Override
		public PathComponent expand(UriTemplateVariables uriVariables) {
			List<PathComponent> expandedComponents = new ArrayList<PathComponent>(this.pathComponents.size());
			for (PathComponent pathComponent : this.pathComponents) {
				expandedComponents.add(pathComponent.expand(uriVariables));
			}
			return new PathComponentComposite(expandedComponents);
		}

		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
			for (PathComponent pathComponent : this.pathComponents) {
				pathComponent.copyToUriComponentsBuilder(builder);
			}
		}
	}


	/**
	 * 表示空路径.
	 */
	static final PathComponent NULL_PATH_COMPONENT = new PathComponent() {
		@Override
		public String getPath() {
			return null;
		}
		@Override
		public List<String> getPathSegments() {
			return Collections.emptyList();
		}
		@Override
		public PathComponent encode(String encoding) throws UnsupportedEncodingException {
			return this;
		}
		@Override
		public void verify() {
		}
		@Override
		public PathComponent expand(UriTemplateVariables uriVariables) {
			return this;
		}
		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		}
		@Override
		public boolean equals(Object obj) {
			return (this == obj);
		}
		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	};


	private static class QueryUriTemplateVariables implements UriTemplateVariables {

		private final UriTemplateVariables delegate;

		public QueryUriTemplateVariables(UriTemplateVariables delegate) {
			this.delegate = delegate;
		}

		@Override
		public Object getValue(String name) {
			Object value = this.delegate.getValue(name);
			if (ObjectUtils.isArray(value)) {
				value = StringUtils.arrayToCommaDelimitedString(ObjectUtils.toObjectArray(value));
			}
			return value;
		}
	}

}
