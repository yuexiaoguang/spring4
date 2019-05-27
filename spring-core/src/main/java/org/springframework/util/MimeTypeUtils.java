package org.springframework.util;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.util.MimeType.SpecificityComparator;

/**
 * 其他{@link MimeType}实用程序方法.
 */
@SuppressWarnings("deprecation")
public abstract class MimeTypeUtils {

	private static final byte[] BOUNDARY_CHARS =
			new byte[] {'-', '_', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
					'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A',
					'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
					'V', 'W', 'X', 'Y', 'Z'};

	private static final Random RND = new SecureRandom();

	private static Charset US_ASCII = Charset.forName("US-ASCII");

	/**
	 * {@link #sortBySpecificity(List)}使用的比较器.
	 */
	public static final Comparator<MimeType> SPECIFICITY_COMPARATOR = new SpecificityComparator<MimeType>();

	/**
	 * 公共常量mime类型, 包括所有媒体范围 (i.e. "&#42;/&#42;").
	 */
	public static final MimeType ALL;

	/**
	 * 字符串相当于{@link MimeTypeUtils#ALL}.
	 */
	public static final String ALL_VALUE = "*/*";

	/**
	 * {@code application/atom+xml}的公共常量mime类型.
	 * @deprecated as of 4.3.6, in favor of {@code MediaType} constants
	 */
	@Deprecated
	public static final MimeType APPLICATION_ATOM_XML;

	/**
	 * {@link MimeTypeUtils#APPLICATION_ATOM_XML}.
	 * @deprecated as of 4.3.6, in favor of {@code MediaType} constants
	 */
	@Deprecated
	public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

	/**
	 * {@code application/x-www-form-urlencoded}的公共常量mime类型.
	 * @deprecated as of 4.3.6, in favor of {@code MediaType} constants
	 *  */
	@Deprecated
	public static final MimeType APPLICATION_FORM_URLENCODED;

	/**
	 * {@link MimeTypeUtils#APPLICATION_FORM_URLENCODED}.
	 * @deprecated as of 4.3.6, in favor of {@code MediaType} constants
	 */
	@Deprecated
	public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

	/**
	 * {@code application/json}的公共常量mime类型.
	 */
	public static final MimeType APPLICATION_JSON;

	/**
	 * {@link MimeTypeUtils#APPLICATION_JSON}.
	 */
	public static final String APPLICATION_JSON_VALUE = "application/json";

	/**
	 * {@code application/octet-stream}的公共常量mime类型.
	 */
	public static final MimeType APPLICATION_OCTET_STREAM;

	/**
	 * {@link MimeTypeUtils#APPLICATION_OCTET_STREAM}.
	 */
	public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

	/**
	 * {@code application/xhtml+xml}的公共常量mime类型.
	 * @deprecated as of 4.3.6, in favor of {@code MediaType} constants
	 */
	@Deprecated
	public static final MimeType APPLICATION_XHTML_XML;

	/**
	 * {@link MimeTypeUtils#APPLICATION_XHTML_XML}.
	 * @deprecated as of 4.3.6, in favor of {@code MediaType} constants
	 */
	@Deprecated
	public static final String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

	/**
	 * {@code application/xml}的公共常量mime类型.
	 */
	public static final MimeType APPLICATION_XML;

	/**
	 * {@link MimeTypeUtils#APPLICATION_XML}.
	 */
	public static final String APPLICATION_XML_VALUE = "application/xml";

	/**
	 * {@code image/gif}的公共常量mime类型.
	 */
	public static final MimeType IMAGE_GIF;

	/**
	 * {@link MimeTypeUtils#IMAGE_GIF}.
	 */
	public static final String IMAGE_GIF_VALUE = "image/gif";

	/**
	 * {@code image/jpeg}的公共常量mime类型.
	 */
	public static final MimeType IMAGE_JPEG;

	/**
	 * {@link MimeTypeUtils#IMAGE_JPEG}.
	 */
	public static final String IMAGE_JPEG_VALUE = "image/jpeg";

	/**
	 * {@code image/png}的公共常量mime类型.
	 */
	public static final MimeType IMAGE_PNG;

	/**
	 * {@link MimeTypeUtils#IMAGE_PNG}.
	 */
	public static final String IMAGE_PNG_VALUE = "image/png";

	/**
	 * {@code multipart/form-data}的公共常量mime类型.
	 * @deprecated as of 4.3.6, in favor of {@code MediaType} constants
	 */
	@Deprecated
	public static final MimeType MULTIPART_FORM_DATA;

	/**
	 * {@link MimeTypeUtils#MULTIPART_FORM_DATA}.
	 * @deprecated as of 4.3.6, in favor of {@code MediaType} constants
	 */
	@Deprecated
	public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

	/**
	 * {@code text/html}的公共常量mime类型.
	 */
	public static final MimeType TEXT_HTML;

	/**
	 * {@link MimeTypeUtils#TEXT_HTML}.
	 */
	public static final String TEXT_HTML_VALUE = "text/html";

	/**
	 * {@code text/plain}的公共常量mime类型.
	 */
	public static final MimeType TEXT_PLAIN;

	/**
	 * {@link MimeTypeUtils#TEXT_PLAIN}.
	 */
	public static final String TEXT_PLAIN_VALUE = "text/plain";

	/**
	 * {@code text/xml}的公共常量mime类型.
	 */
	public static final MimeType TEXT_XML;

	/**
	 * {@link MimeTypeUtils#TEXT_XML}.
	 */
	public static final String TEXT_XML_VALUE = "text/xml";


	static {
		ALL = MimeType.valueOf(ALL_VALUE);
		APPLICATION_ATOM_XML = MimeType.valueOf(APPLICATION_ATOM_XML_VALUE);
		APPLICATION_FORM_URLENCODED = MimeType.valueOf(APPLICATION_FORM_URLENCODED_VALUE);
		APPLICATION_JSON = MimeType.valueOf(APPLICATION_JSON_VALUE);
		APPLICATION_OCTET_STREAM = MimeType.valueOf(APPLICATION_OCTET_STREAM_VALUE);
		APPLICATION_XHTML_XML = MimeType.valueOf(APPLICATION_XHTML_XML_VALUE);
		APPLICATION_XML = MimeType.valueOf(APPLICATION_XML_VALUE);
		IMAGE_GIF = MimeType.valueOf(IMAGE_GIF_VALUE);
		IMAGE_JPEG = MimeType.valueOf(IMAGE_JPEG_VALUE);
		IMAGE_PNG = MimeType.valueOf(IMAGE_PNG_VALUE);
		MULTIPART_FORM_DATA = MimeType.valueOf(MULTIPART_FORM_DATA_VALUE);
		TEXT_HTML = MimeType.valueOf(TEXT_HTML_VALUE);
		TEXT_PLAIN = MimeType.valueOf(TEXT_PLAIN_VALUE);
		TEXT_XML = MimeType.valueOf(TEXT_XML_VALUE);
	}


	/**
	 * 将给定的String解析为单个{@code MimeType}.
	 * 
	 * @param mimeType 要解析的字符串
	 * 
	 * @return mime类型
	 * @throws InvalidMimeTypeException 如果字符串无法解析
	 */
	public static MimeType parseMimeType(String mimeType) {
		if (!StringUtils.hasLength(mimeType)) {
			throw new InvalidMimeTypeException(mimeType, "'mimeType' must not be empty");
		}

		int index = mimeType.indexOf(';');
		String fullType = (index >= 0 ? mimeType.substring(0, index) : mimeType).trim();
		if (fullType.isEmpty()) {
			throw new InvalidMimeTypeException(mimeType, "'mimeType' must not be empty");
		}

		// java.net.HttpURLConnection returns a *; q=.2 Accept header
		if (MimeType.WILDCARD_TYPE.equals(fullType)) {
			fullType = "*/*";
		}
		int subIndex = fullType.indexOf('/');
		if (subIndex == -1) {
			throw new InvalidMimeTypeException(mimeType, "does not contain '/'");
		}
		if (subIndex == fullType.length() - 1) {
			throw new InvalidMimeTypeException(mimeType, "does not contain subtype after '/'");
		}
		String type = fullType.substring(0, subIndex);
		String subtype = fullType.substring(subIndex + 1, fullType.length());
		if (MimeType.WILDCARD_TYPE.equals(type) && !MimeType.WILDCARD_TYPE.equals(subtype)) {
			throw new InvalidMimeTypeException(mimeType, "wildcard type is legal only in '*/*' (all mime types)");
		}

		Map<String, String> parameters = null;
		do {
			int nextIndex = index + 1;
			boolean quoted = false;
			while (nextIndex < mimeType.length()) {
				char ch = mimeType.charAt(nextIndex);
				if (ch == ';') {
					if (!quoted) {
						break;
					}
				}
				else if (ch == '"') {
					quoted = !quoted;
				}
				nextIndex++;
			}
			String parameter = mimeType.substring(index + 1, nextIndex).trim();
			if (parameter.length() > 0) {
				if (parameters == null) {
					parameters = new LinkedHashMap<String, String>(4);
				}
				int eqIndex = parameter.indexOf('=');
				if (eqIndex >= 0) {
					String attribute = parameter.substring(0, eqIndex).trim();
					String value = parameter.substring(eqIndex + 1, parameter.length()).trim();
					parameters.put(attribute, value);
				}
			}
			index = nextIndex;
		}
		while (index < mimeType.length());

		try {
			return new MimeType(type, subtype, parameters);
		}
		catch (UnsupportedCharsetException ex) {
			throw new InvalidMimeTypeException(mimeType, "unsupported charset '" + ex.getCharsetName() + "'");
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidMimeTypeException(mimeType, ex.getMessage());
		}
	}

	/**
	 * 将给定的逗号分隔字符串解析为{@code MimeType}对象列表.
	 * 
	 * @param mimeTypes 要解析的字符串
	 * 
	 * @return mime类型列表
	 * @throws IllegalArgumentException 如果字符串无法解析
	 */
	public static List<MimeType> parseMimeTypes(String mimeTypes) {
		if (!StringUtils.hasLength(mimeTypes)) {
			return Collections.emptyList();
		}
		String[] tokens = StringUtils.tokenizeToStringArray(mimeTypes, ",");
		List<MimeType> result = new ArrayList<MimeType>(tokens.length);
		for (String token : tokens) {
			result.add(parseMimeType(token));
		}
		return result;
	}

	/**
	 * 返回给定{@code MimeType}对象列表的字符串表示形式.
	 * 
	 * @param mimeTypes 要解析的字符串
	 * 
	 * @return mime类型列表
	 * @throws IllegalArgumentException 如果字符串无法解析
	 */
	public static String toString(Collection<? extends MimeType> mimeTypes) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<? extends MimeType> iterator = mimeTypes.iterator(); iterator.hasNext();) {
			MimeType mimeType = iterator.next();
			mimeType.appendTo(builder);
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}


	/**
	 * 按特定方式对{@code MimeType}对象的给定列表进行排序.
	 * <p>给定两种mime类型:
	 * <ol>
	 * <li>如果mime类型有{@linkplain MimeType#isWildcardType() 通配符类型}, 那么没有通配符的mime类型在另一个之前排序.</li>
	 * <li>如果两个mime类型具有不同的{@linkplain MimeType#getType() 类型}, 那么它们被认为是相等的并保持其当前顺序.</li>
	 * <li>如果任一mime类型具有{@linkplain MimeType#isWildcardSubtype() 通配符子类型}, 则不带通配符的mime类型在另一个之前排序.</li>
	 * <li>如果两个mime类型具有不同的{@linkplain MimeType#getSubtype() 子类型}, 那么它们被认为是相等的并保持其当前顺序.</li>
	 * <li>如果两个mime类型具有不同的{@linkplain MimeType#getParameter(String) 参数}, 则具有最多参数的mime类型在另一个之前排序.</li>
	 * </ol>
	 * <p>例如: <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
	 * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
	 * <blockquote>audio/basic == text/html</blockquote> <blockquote>audio/basic == audio/wave</blockquote>
	 * 
	 * @param mimeTypes 要排序的mime类型列表
	 * 
	 * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics and Content, section 5.3.2</a>
	 */
	public static void sortBySpecificity(List<MimeType> mimeTypes) {
		Assert.notNull(mimeTypes, "'mimeTypes' must not be null");
		if (mimeTypes.size() > 1) {
			Collections.sort(mimeTypes, SPECIFICITY_COMPARATOR);
		}
	}

	/**
	 * 生成随机MIME边界作为字节, 通常用于多部分mime类型.
	 */
	public static byte[] generateMultipartBoundary() {
		byte[] boundary = new byte[RND.nextInt(11) + 30];
		for (int i = 0; i < boundary.length; i++) {
			boundary[i] = BOUNDARY_CHARS[RND.nextInt(BOUNDARY_CHARS.length)];
		}
		return boundary;
	}

	/**
	 * 生成随机MIME边界作为String, 通常用于多部分mime类型.
	 */
	public static String generateMultipartBoundaryString() {
		return new String(generateMultipartBoundary(), US_ASCII);
	}
}
