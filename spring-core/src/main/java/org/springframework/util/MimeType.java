package org.springframework.util;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * 表示MIME类型, 最初在RFC 2046中定义, 随后用于其他Internet协议, 包括HTTP.
 *
 * <p>但是, 此类不包含对HTTP内容协商中使用的q参数的支持.
 * 这些可以在{@code spring-web}模块的子类{@code org.springframework.http.MediaType}中找到.
 *
 * <p>由{@linkplain #getType() 类型}和{@linkplain #getSubtype() 子类型}组成.
 * 还具有使用{@link #valueOf(String)}从{@code String}解析MIME类型值的功能.
 * 有关更多解析选项, 请参阅{@link MimeTypeUtils}.
 */
public class MimeType implements Comparable<MimeType>, Serializable {

	private static final long serialVersionUID = 4085923477777865903L;


	protected static final String WILDCARD_TYPE = "*";

	private static final String PARAM_CHARSET = "charset";

	private static final BitSet TOKEN;

	static {
		// variable names refer to RFC 2616, section 2.2
		BitSet ctl = new BitSet(128);
		for (int i = 0; i <= 31; i++) {
			ctl.set(i);
		}
		ctl.set(127);

		BitSet separators = new BitSet(128);
		separators.set('(');
		separators.set(')');
		separators.set('<');
		separators.set('>');
		separators.set('@');
		separators.set(',');
		separators.set(';');
		separators.set(':');
		separators.set('\\');
		separators.set('\"');
		separators.set('/');
		separators.set('[');
		separators.set(']');
		separators.set('?');
		separators.set('=');
		separators.set('{');
		separators.set('}');
		separators.set(' ');
		separators.set('\t');

		TOKEN = new BitSet(128);
		TOKEN.set(0, 128);
		TOKEN.andNot(ctl);
		TOKEN.andNot(separators);
	}


	private final String type;

	private final String subtype;

	private final Map<String, String> parameters;


	/**
	 * <p>{@linkplain #getSubtype() 子类型}被设置为<code>"&#42;"</code>, 并且参数为空.
	 * 
	 * @param type 主要类型
	 * 
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MimeType(String type) {
		this(type, WILDCARD_TYPE);
	}

	/**
	 * 为给定的主要类型和子类型创建新的{@code MimeType}.
	 * <p>参数为空.
	 * 
	 * @param type 主要类型
	 * @param subtype 子类型
	 * 
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MimeType(String type, String subtype) {
		this(type, subtype, Collections.<String, String>emptyMap());
	}

	/**
	 * 为给定的类型, 子类型和字符集创建新的{@code MimeType}.
	 * 
	 * @param type 主要类型
	 * @param subtype 子类型
	 * @param charset 字符集
	 * 
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MimeType(String type, String subtype, Charset charset) {
		this(type, subtype, Collections.singletonMap(PARAM_CHARSET, charset.name()));
	}

	/**
	 * 复制构造函数, 复制给定{@code MimeType}的类型, 子类型, 参数, 并允许设置指定的字符集.
	 * 
	 * @param other 另一个MimeType
	 * @param charset 字符集
	 * 
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MimeType(MimeType other, Charset charset) {
		this(other.getType(), other.getSubtype(), addCharsetParameter(charset, other.getParameters()));
	}

	/**
	 * 复制构造函数, 复制给定{@code MimeType}的类型, 子类型, 并允许不同的参数.
	 * 
	 * @param other 另一个MimeType
	 * @param parameters 参数 (may be {@code null})
	 * 
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MimeType(MimeType other, Map<String, String> parameters) {
		this(other.getType(), other.getSubtype(), parameters);
	}

	/**
	 * @param type 主要类型
	 * @param subtype 子类型
	 * @param parameters 参数 (may be {@code null})
	 * 
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MimeType(String type, String subtype, Map<String, String> parameters) {
		Assert.hasLength(type, "'type' must not be empty");
		Assert.hasLength(subtype, "'subtype' must not be empty");
		checkToken(type);
		checkToken(subtype);
		this.type = type.toLowerCase(Locale.ENGLISH);
		this.subtype = subtype.toLowerCase(Locale.ENGLISH);
		if (!CollectionUtils.isEmpty(parameters)) {
			Map<String, String> map = new LinkedCaseInsensitiveMap<String>(parameters.size(), Locale.ENGLISH);
			for (Map.Entry<String, String> entry : parameters.entrySet()) {
				String attribute = entry.getKey();
				String value = entry.getValue();
				checkParameters(attribute, value);
				map.put(attribute, value);
			}
			this.parameters = Collections.unmodifiableMap(map);
		}
		else {
			this.parameters = Collections.emptyMap();
		}
	}

	/**
	 * 检查给定的令牌字符串是否存在非法字符, 如 RFC 2616, section 2.2中所定义.
	 * 
	 * @throws IllegalArgumentException 非法字符
	 * @see <a href="http://tools.ietf.org/html/rfc2616#section-2.2">HTTP 1.1, section 2.2</a>
	 */
	private void checkToken(String token) {
		for (int i = 0; i < token.length(); i++ ) {
			char ch = token.charAt(i);
			if (!TOKEN.get(ch)) {
				throw new IllegalArgumentException("Invalid token character '" + ch + "' in token \"" + token + "\"");
			}
		}
	}

	protected void checkParameters(String attribute, String value) {
		Assert.hasLength(attribute, "'attribute' must not be empty");
		Assert.hasLength(value, "'value' must not be empty");
		checkToken(attribute);
		if (PARAM_CHARSET.equals(attribute)) {
			value = unquote(value);
			Charset.forName(value);
		}
		else if (!isQuotedString(value)) {
			checkToken(value);
		}
	}

	private boolean isQuotedString(String s) {
		if (s.length() < 2) {
			return false;
		}
		else {
			return ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));
		}
	}

	protected String unquote(String s) {
		if (s == null) {
			return null;
		}
		return (isQuotedString(s) ? s.substring(1, s.length() - 1) : s);
	}

	/**
	 * 指示{@linkplain #getType() 类型}是否为通配符<code>&#42;</code>.
	 */
	public boolean isWildcardType() {
		return WILDCARD_TYPE.equals(getType());
	}

	/**
	 * 指示{@linkplain #getSubtype() 子类型}是通配符<code>&#42;</code>, 还是通配符后跟后缀 (e.g. <code>&#42;+xml</code>).
	 * 
	 * @return 子类型是否为通配符
	 */
	public boolean isWildcardSubtype() {
		return WILDCARD_TYPE.equals(getSubtype()) || getSubtype().startsWith("*+");
	}

	/**
	 * 指示此MIME类型是否具体, i.e. 类型和子类型是否都不是通配符<code>&#42;</code>.
	 * 
	 * @return 此MIME类型是否具体
	 */
	public boolean isConcrete() {
		return !isWildcardType() && !isWildcardSubtype();
	}

	/**
	 * 返回主要类型.
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * 返回子类型.
	 */
	public String getSubtype() {
		return this.subtype;
	}

	/**
	 * 返回字符集, 如{@code charset}参数所示.
	 * 
	 * @return 字符集, 或{@code null}
	 */
	public Charset getCharset() {
		String charset = getParameter(PARAM_CHARSET);
		return (charset != null ? Charset.forName(unquote(charset)) : null);
	}

	/**
	 * 返回字符集, 如{@code charset}参数所示.
	 * 
	 * @return 字符集, 或{@code null}
	 * @deprecated as of Spring 4.3, in favor of {@link #getCharset()} with its name
	 * aligned with the Java return type name
	 */
	@Deprecated
	public Charset getCharSet() {
		return getCharset();
	}

	/**
	 * 给定参数名称, 返回通用参数值.
	 * 
	 * @param name 参数名称
	 * 
	 * @return 参数值, 或{@code null}
	 */
	public String getParameter(String name) {
		return this.parameters.get(name);
	}

	/**
	 * 返回所有通用参数值.
	 * 
	 * @return 只读 map (可能为空, never {@code null})
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	/**
	 * 指示此MIME类型是否包含给定的MIME类型.
	 * <p>例如, {@code text/*}包括{@code text/plain} 和 {@code text/html},
	 * {@code application/*+xml}包括{@code application/soap+xml}, etc.
	 * 此方法不是对称的, 反过来就不一定了.
	 * 
	 * @param other 要与之比较的引用MIME类型
	 * 
	 * @return {@code true}如果此MIME类型包含给定的MIME类型; 否则{@code false}
	 */
	public boolean includes(MimeType other) {
		if (other == null) {
			return false;
		}
		if (isWildcardType()) {
			// */* includes anything
			return true;
		}
		else if (getType().equals(other.getType())) {
			if (getSubtype().equals(other.getSubtype())) {
				return true;
			}
			if (isWildcardSubtype()) {
				// Wildcard with suffix, e.g. application/*+xml
				int thisPlusIdx = getSubtype().lastIndexOf('+');
				if (thisPlusIdx == -1) {
					return true;
				}
				else {
					// application/*+xml includes application/soap+xml
					int otherPlusIdx = other.getSubtype().indexOf('+');
					if (otherPlusIdx != -1) {
						String thisSubtypeNoSuffix = getSubtype().substring(0, thisPlusIdx);
						String thisSubtypeSuffix = getSubtype().substring(thisPlusIdx + 1);
						String otherSubtypeSuffix = other.getSubtype().substring(otherPlusIdx + 1);
						if (thisSubtypeSuffix.equals(otherSubtypeSuffix) && WILDCARD_TYPE.equals(thisSubtypeNoSuffix)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * 指示此MIME类型是否与给定的MIME类型兼容.
	 * <p>例如, {@code text/*}与{@code text/plain}, {@code text/html}兼容, 反之亦然.
	 * 实际上, 此方法类似于 {@link #includes}, 除了它<b>是</b>对称的.
	 * 
	 * @param other 要与之比较的引用MIME类型
	 * 
	 * @return {@code true}如果此MIME类型与给定的MIME类型兼容; 否则{@code false}
	 */
	public boolean isCompatibleWith(MimeType other) {
		if (other == null) {
			return false;
		}
		if (isWildcardType() || other.isWildcardType()) {
			return true;
		}
		else if (getType().equals(other.getType())) {
			if (getSubtype().equals(other.getSubtype())) {
				return true;
			}
			// Wildcard with suffix? e.g. application/*+xml
			if (isWildcardSubtype() || other.isWildcardSubtype()) {
				int thisPlusIdx = getSubtype().indexOf('+');
				int otherPlusIdx = other.getSubtype().indexOf('+');
				if (thisPlusIdx == -1 && otherPlusIdx == -1) {
					return true;
				}
				else if (thisPlusIdx != -1 && otherPlusIdx != -1) {
					String thisSubtypeNoSuffix = getSubtype().substring(0, thisPlusIdx);
					String otherSubtypeNoSuffix = other.getSubtype().substring(0, otherPlusIdx);
					String thisSubtypeSuffix = getSubtype().substring(thisPlusIdx + 1);
					String otherSubtypeSuffix = other.getSubtype().substring(otherPlusIdx + 1);
					if (thisSubtypeSuffix.equals(otherSubtypeSuffix) &&
							(WILDCARD_TYPE.equals(thisSubtypeNoSuffix) || WILDCARD_TYPE.equals(otherSubtypeNoSuffix))) {
						return true;
					}
				}
			}
		}
		return false;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MimeType)) {
			return false;
		}
		MimeType otherType = (MimeType) other;
		return (this.type.equalsIgnoreCase(otherType.type) &&
				this.subtype.equalsIgnoreCase(otherType.subtype) &&
				parametersAreEqual(otherType));
	}

	/**
	 * 确定此{@code MimeType}和提供的{@code MimeType}中的参数是否相等,
	 * 对{@link Charset}执行不区分大小写的比较.
	 */
	private boolean parametersAreEqual(MimeType other) {
		if (this.parameters.size() != other.parameters.size()) {
			return false;
		}

		for (String key : this.parameters.keySet()) {
			if (!other.parameters.containsKey(key)) {
				return false;
			}
			if (PARAM_CHARSET.equals(key)) {
				if (!ObjectUtils.nullSafeEquals(getCharset(), other.getCharset())) {
					return false;
				}
			}
			else if (!ObjectUtils.nullSafeEquals(this.parameters.get(key), other.parameters.get(key))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = this.type.hashCode();
		result = 31 * result + this.subtype.hashCode();
		result = 31 * result + this.parameters.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		appendTo(builder);
		return builder.toString();
	}

	protected void appendTo(StringBuilder builder) {
		builder.append(this.type);
		builder.append('/');
		builder.append(this.subtype);
		appendTo(this.parameters, builder);
	}

	private void appendTo(Map<String, String> map, StringBuilder builder) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			builder.append(';');
			builder.append(entry.getKey());
			builder.append('=');
			builder.append(entry.getValue());
		}
	}

	/**
	 * 按字母顺序将此MIME类型与另一个进行比较.
	 * 
	 * @param other 要比较的MIME类型
	 */
	@Override
	public int compareTo(MimeType other) {
		int comp = getType().compareToIgnoreCase(other.getType());
		if (comp != 0) {
			return comp;
		}
		comp = getSubtype().compareToIgnoreCase(other.getSubtype());
		if (comp != 0) {
			return comp;
		}
		comp = getParameters().size() - other.getParameters().size();
		if (comp != 0) {
			return comp;
		}

		TreeSet<String> thisAttributes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		thisAttributes.addAll(getParameters().keySet());
		TreeSet<String> otherAttributes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		otherAttributes.addAll(other.getParameters().keySet());
		Iterator<String> thisAttributesIterator = thisAttributes.iterator();
		Iterator<String> otherAttributesIterator = otherAttributes.iterator();

		while (thisAttributesIterator.hasNext()) {
			String thisAttribute = thisAttributesIterator.next();
			String otherAttribute = otherAttributesIterator.next();
			comp = thisAttribute.compareToIgnoreCase(otherAttribute);
			if (comp != 0) {
				return comp;
			}
			if (PARAM_CHARSET.equals(thisAttribute)) {
				Charset thisCharset = getCharset();
				Charset otherCharset = other.getCharset();
				if (thisCharset != otherCharset) {
					if (thisCharset == null) {
						return -1;
					}
					if (otherCharset == null) {
						return 1;
					}
					comp = thisCharset.compareTo(otherCharset);
					if (comp != 0) {
						return comp;
					}
				}
			}
			else {
				String thisValue = getParameters().get(thisAttribute);
				String otherValue = other.getParameters().get(otherAttribute);
				if (otherValue == null) {
					otherValue = "";
				}
				comp = thisValue.compareTo(otherValue);
				if (comp != 0) {
					return comp;
				}
			}
		}

		return 0;
	}


	/**
	 * 将给定的String值解析为{@code MimeType}对象, 此方法名称遵循'valueOf'命名约定
	 * (由{@link org.springframework.core.convert.ConversionService}支持.
	 */
	public static MimeType valueOf(String value) {
		return MimeTypeUtils.parseMimeType(value);
	}

	private static Map<String, String> addCharsetParameter(Charset charset, Map<String, String> parameters) {
		Map<String, String> map = new LinkedHashMap<String, String>(parameters);
		map.put(PARAM_CHARSET, charset.name());
		return map;
	}


	public static class SpecificityComparator<T extends MimeType> implements Comparator<T> {

		@Override
		public int compare(T mimeType1, T mimeType2) {
			if (mimeType1.isWildcardType() && !mimeType2.isWildcardType()) {  // */* < audio/*
				return 1;
			}
			else if (mimeType2.isWildcardType() && !mimeType1.isWildcardType()) {  // audio/* > */*
				return -1;
			}
			else if (!mimeType1.getType().equals(mimeType2.getType())) {  // audio/basic == text/html
				return 0;
			}
			else {  // mediaType1.getType().equals(mediaType2.getType())
				if (mimeType1.isWildcardSubtype() && !mimeType2.isWildcardSubtype()) {  // audio/* < audio/basic
					return 1;
				}
				else if (mimeType2.isWildcardSubtype() && !mimeType1.isWildcardSubtype()) {  // audio/basic > audio/*
					return -1;
				}
				else if (!mimeType1.getSubtype().equals(mimeType2.getSubtype())) {  // audio/basic == audio/wave
					return 0;
				}
				else {  // mediaType2.getSubtype().equals(mediaType2.getSubtype())
					return compareParameters(mimeType1, mimeType2);
				}
			}
		}

		protected int compareParameters(T mimeType1, T mimeType2) {
			int paramsSize1 = mimeType1.getParameters().size();
			int paramsSize2 = mimeType2.getParameters().size();
			return (paramsSize2 < paramsSize1 ? -1 : (paramsSize2 == paramsSize1 ? 0 : 1));  // audio/basic;level=1 < audio/basic
		}
	}
}
