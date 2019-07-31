package org.springframework.web.util;

import org.springframework.util.Assert;

/**
 * 用于HTML转义的工具类.
 * Escapes和unescapes基于W3C HTML 4.01建议, 处理字符实体引用.
 *
 * <p>Reference:
 * <a href="http://www.w3.org/TR/html4/charset.html">http://www.w3.org/TR/html4/charset.html</a>
 *
 * <p>对于一组全面的String转义工具, 请考虑Apache Commons Lang及其StringEscapeUtils类.
 * 这里没有使用该类来避免对Commons Lang的运行时依赖, 仅用于HTML转义.
 * 此外, Spring的HTML转义更灵活, 100% 兼容HTML 4.0.
 */
public abstract class HtmlUtils {

	/**
	 * 预解析的HTML字符实体引用的共享实例.
	 */
	private static final HtmlCharacterEntityReferences characterEntityReferences =
			new HtmlCharacterEntityReferences();


	/**
	 * 将特殊字符转换为HTML字符引用.
	 * 处理HTML 4.01建议中定义的完整字符集.
	 * <p>将所有特殊字符转义为其对应的实体引用 (e.g. {@code &lt;}).
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * 
	 * @param input (未转义的)输入字符串
	 * 
	 * @return 转义后的字符串
	 */
	public static String htmlEscape(String input) {
		return htmlEscape(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 将特殊字符转换为HTML字符引用.
	 * 处理HTML 4.01建议中定义的完整字符集.
	 * <p>至少根据指定编码的要求, 将所有特殊字符转义为其对应的实体引用 (e.g. {@code &lt;}).
	 * 换句话说, 如果不必为给定的编码转义特殊字符, 则可能不是.
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * 
	 * @param input (未转义的)输入字符串
	 * @param encoding 受支持的{@link java.nio.charset.Charset charset}的名称
	 * 
	 * @return 转义后的字符串
	 */
	public static String htmlEscape(String input, String encoding) {
		Assert.notNull(encoding, "Encoding is required");
		if (input == null) {
			return null;
		}
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			String reference = characterEntityReferences.convertToReference(character, encoding);
			if (reference != null) {
				escaped.append(reference);
			}
			else {
				escaped.append(character);
			}
		}
		return escaped.toString();
	}

	/**
	 * 将特殊字符转换为HTML字符引用.
	 * 处理HTML 4.01建议中定义的完整字符集.
	 * <p>将所有特殊字符转义为十进制格式的相应数字引用 (&#<i>Decimal</i>;).
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * 
	 * @param input (未转义的)输入字符串
	 * 
	 * @return 转义后的字符串
	 */
	public static String htmlEscapeDecimal(String input) {
		return htmlEscapeDecimal(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 将特殊字符转换为HTML字符引用.
	 * 处理HTML 4.01建议中定义的完整字符集.
	 * <p>至少根据指定的编码要求, 将所有特殊字符转换为十进制格式的相应数字引用(&#<i>Decimal</i>;).
	 * 换句话说, 如果不必为给定的编码转义特殊字符, 则可能不是.
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * 
	 * @param input (未转义的)输入字符串
	 * @param encoding 受支持的{@link java.nio.charset.Charset charset}的名称
	 * 
	 * @return 转义后的字符串
	 */
	public static String htmlEscapeDecimal(String input, String encoding) {
		Assert.notNull(encoding, "Encoding is required");
		if (input == null) {
			return null;
		}
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			if (characterEntityReferences.isMappedToReference(character, encoding)) {
				escaped.append(HtmlCharacterEntityReferences.DECIMAL_REFERENCE_START);
				escaped.append((int) character);
				escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
			}
			else {
				escaped.append(character);
			}
		}
		return escaped.toString();
	}

	/**
	 * 将特殊字符转换为HTML字符引用.
	 * 处理HTML 4.01建议中定义的完整字符集.
	 * <p>将所有特殊字符转换为十六进制格式的相应数字引用 (&#x<i>Hex</i>;).
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * 
	 * @param input (未转义的)输入字符串
	 * 
	 * @return 转义后的字符串
	 */
	public static String htmlEscapeHex(String input) {
		return htmlEscapeHex(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 将特殊字符转换为HTML字符引用.
	 * 处理HTML 4.01建议中定义的完整字符集.
	 * <p>至少根据指定的编码要求, 将所有特殊字符转换为十六进制格式的相应数字引用(&#x<i>Hex</i>;).
	 * 换句话说, 如果不必为给定的编码转义特殊字符, 则可能不是.
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * 
	 * @param input (未转义的)输入字符串
	 * @param encoding 受支持的{@link java.nio.charset.Charset charset}的名称
	 * 
	 * @return 转义后的字符串
	 */
	public static String htmlEscapeHex(String input, String encoding) {
		Assert.notNull(encoding, "Encoding is required");
		if (input == null) {
			return null;
		}
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			if (characterEntityReferences.isMappedToReference(character, encoding)) {
				escaped.append(HtmlCharacterEntityReferences.HEX_REFERENCE_START);
				escaped.append(Integer.toString(character, 16));
				escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
			}
			else {
				escaped.append(character);
			}
		}
		return escaped.toString();
	}

	/**
	 * 将HTML字符引用转换为其纯文本UNICODE等效项.
	 * <p>处理HTML 4.01建议中定义的完整字符集和所有引用类型 (十进制, 十六进制和实体).
	 * <p>正确转换以下格式:
	 * <blockquote>
	 * &amp;#<i>Entity</i>; - <i>(Example: &amp;amp;)区分大小写</i>
	 * &amp;#<i>Decimal</i>; - <i>(Example: &amp;#68;)</i><br>
	 * &amp;#x<i>Hex</i>; - <i>(Example: &amp;#xE5;)区分大小写</i><br>
	 * </blockquote>
	 * 通过复制遇到的原始字符来优雅地处理格式错误的字符引用.<p>
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * 
	 * @param input (未转义的)输入字符串
	 * 
	 * @return 转义后的字符串
	 */
	public static String htmlUnescape(String input) {
		if (input == null) {
			return null;
		}
		return new HtmlCharacterEntityDecoder(characterEntityReferences, input).decode();
	}

}
