package org.springframework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * <p>主要供框架内部使用;
 * 考虑<a href="http://commons.apache.org/proper/commons-lang/">Apache's Commons Lang</a>
 * 用于更全面的{@code String}实用工具套件.
 *
 * <p>这个类提供了一些应该由核心Java {@link String}和{@link StringBuilder}类提供的简单功能.
 * 它还提供了易于使用的方法来转换分隔字符串, 如CSV字符串, 集合和数组.
 */
public abstract class StringUtils {

	private static final String FOLDER_SEPARATOR = "/";

	private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

	private static final String TOP_PATH = "..";

	private static final String CURRENT_PATH = ".";

	private static final char EXTENSION_SEPARATOR = '.';


	//---------------------------------------------------------------------
	// General convenience methods for working with Strings
	//---------------------------------------------------------------------

	/**
	 * 检查给定的{@code String}是否为空.
	 * <p>此方法接受任何Object作为参数, 将其与{@code null}和空String进行比较.
	 * 因此, 对于非null非String对象, 此方法永远不会返回{@code true}.
	 * <p>Object签名对于通常处理字符串的一般属性处理代码很有用, 但通常必须迭代对象, 因为属性可能基本类型.
	 * 
	 * @param str 候选String
	 */
	public static boolean isEmpty(Object str) {
		return (str == null || "".equals(str));
	}

	/**
	 * 检查给定的{@code CharSequence}既不是{@code null}也不是长度0.
	 * <p>Note: 对于纯粹由空格组成的{@code CharSequence}, 此方法返回{@code true}.
	 * <p><pre class="code">
	 * StringUtils.hasLength(null) = false
	 * StringUtils.hasLength("") = false
	 * StringUtils.hasLength(" ") = true
	 * StringUtils.hasLength("Hello") = true
	 * </pre>
	 * 
	 * @param str 要检查的{@code CharSequence} (may be {@code null})
	 * 
	 * @return {@code true}如果{@code CharSequence}不是{@code null}并且有长度
	 */
	public static boolean hasLength(CharSequence str) {
		return (str != null && str.length() > 0);
	}

	/**
	 * 检查给定的{@code String}既不是{@code null}也不是长度为0.
	 * <p>Note: 对于纯粹由空格组成的{@code String}, 此方法返回{@code true}.
	 * 
	 * @param str 要检查的{@code String} (may be {@code null})
	 * 
	 * @return {@code true}如果{@code String}不是{@code null}并且有长度
	 */
	public static boolean hasLength(String str) {
		return (str != null && !str.isEmpty());
	}

	/**
	 * 检查给定的{@code CharSequence}是否包含实际的<em>文本</em>.
	 * <p>更具体地说, 如果{@code CharSequence}不是{@code null}, 它的长度大于0,
	 * 并且它包含至少一个非空格字符, 则此方法返回{@code true}.
	 * <p><pre class="code">
	 * StringUtils.hasText(null) = false
	 * StringUtils.hasText("") = false
	 * StringUtils.hasText(" ") = false
	 * StringUtils.hasText("12345") = true
	 * StringUtils.hasText(" 12345 ") = true
	 * </pre>
	 * 
	 * @param str 要检查的{@code CharSequence} (may be {@code null})
	 * 
	 * @return {@code true} 如果{@code CharSequence}不是{@code null}, 其长度大于0, 并且它不包含空格
	 */
	public static boolean hasText(CharSequence str) {
		return (hasLength(str) && containsText(str));
	}

	/**
	 * 检查给定的{@code String}是否包含实际的<em>文本</em>.
	 * <p>更具体地说, 如果{@code String}不是{@code null}, 它的长度大于0,
	 * 并且它包含至少一个非空格字符, 则此方法返回{@code true}.
	 * 
	 * @param str 要检查的{@code String} (may be {@code null})
	 * 
	 * @return {@code true} 如果{@code String}不是{@code null}, 则其长度大于0, 并且它不包含空格
	 */
	public static boolean hasText(String str) {
		return (hasLength(str) && containsText(str));
	}

	private static boolean containsText(CharSequence str) {
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的{@code CharSequence}是否包含任何空格字符.
	 * 
	 * @param str 要检查的{@code CharSequence} (may be {@code null})
	 * 
	 * @return {@code true} 如果{@code CharSequence}不为空并且包含至少1个空白字符
	 */
	public static boolean containsWhitespace(CharSequence str) {
		if (!hasLength(str)) {
			return false;
		}

		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的{@code String}是否包含任何空格字符.
	 * 
	 * @param str 要检查的{@code String} (may be {@code null})
	 * 
	 * @return {@code true} 如果{@code String}不为空并且包含至少1个空格字符
	 */
	public static boolean containsWhitespace(String str) {
		return containsWhitespace((CharSequence) str);
	}

	/**
	 * 修剪给定{@code String}中的前导和尾随空格.
	 * 
	 * @param str 要检查的{@code String}
	 * 
	 * @return 修剪后的{@code String}
	 */
	public static String trimWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str);
		while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
			sb.deleteCharAt(0);
		}
		while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * 从给定的{@code String}中修剪<i>所有</i>空格: 前导, 尾随, 和字符之间.
	 * 
	 * @param str 要检查的{@code String}
	 * 
	 * @return 修剪后的{@code String}
	 */
	public static String trimAllWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int len = str.length();
		StringBuilder sb = new StringBuilder(str.length());
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			if (!Character.isWhitespace(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 从给定的{@code String}中修剪前导空格.
	 * 
	 * @param str 要检查的{@code String}
	 * 
	 * @return 修剪后的{@code String}
	 */
	public static String trimLeadingWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str);
		while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
			sb.deleteCharAt(0);
		}
		return sb.toString();
	}

	/**
	 * 修剪给定{@code String}的尾随空格.
	 * 
	 * @param str 要检查的{@code String}
	 * 
	 * @return 修剪后的{@code String}
	 */
	public static String trimTrailingWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str);
		while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * 从给定的{@code String}中修剪所有提供的前导字符.
	 * 
	 * @param str 要检查的{@code String}
	 * @param leadingCharacter 要修剪的前导字符
	 * 
	 * @return 修剪后的{@code String}
	 */
	public static String trimLeadingCharacter(String str, char leadingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str);
		while (sb.length() > 0 && sb.charAt(0) == leadingCharacter) {
			sb.deleteCharAt(0);
		}
		return sb.toString();
	}

	/**
	 * 从给定的{@code String}中修剪所有提供的尾随字符.
	 * 
	 * @param str 要检查的{@code String}
	 * @param trailingCharacter 要修剪的尾随字符
	 * 
	 * @return 修剪后的{@code String}
	 */
	public static String trimTrailingCharacter(String str, char trailingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str);
		while (sb.length() > 0 && sb.charAt(sb.length() - 1) == trailingCharacter) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * 测试给定的{@code String}是否以指定的前缀开头, 忽略大写/小写.
	 * 
	 * @param str 要检查的{@code String}
	 * @param prefix 要查找的前缀
	 */
	public static boolean startsWithIgnoreCase(String str, String prefix) {
		return (str != null && prefix != null && str.length() >= prefix.length() &&
				str.regionMatches(true, 0, prefix, 0, prefix.length()));
	}

	/**
	 * 测试给定的{@code String}是否以指定的后缀结尾, 忽略大写/小写.
	 * 
	 * @param str 要检查的{@code String}
	 * @param suffix 要查找的前缀
	 */
	public static boolean endsWithIgnoreCase(String str, String suffix) {
		return (str != null && suffix != null && str.length() >= suffix.length() &&
				str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length()));
	}

	/**
	 * 测试给定字符串是否与给定索引处的给定子字符串匹配.
	 * 
	 * @param str 原始字符串 (或 StringBuilder)
	 * @param index 原始字符串中开始匹配的索引
	 * @param substring 要在给定索引处匹配的子字符串
	 */
	public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
		if (index + substring.length() > str.length()) {
			return false;
		}
		for (int i = 0; i < substring.length(); i++) {
			if (str.charAt(index + i) != substring.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 计算字符串{@code str}中子字符串{@code sub}的出现次数.
	 * 
	 * @param str 要在其中搜索的字符串
	 * @param sub 要搜索的字符串
	 */
	public static int countOccurrencesOf(String str, String sub) {
		if (!hasLength(str) || !hasLength(sub)) {
			return 0;
		}

		int count = 0;
		int pos = 0;
		int idx;
		while ((idx = str.indexOf(sub, pos)) != -1) {
			++count;
			pos = idx + sub.length();
		}
		return count;
	}

	/**
	 * 用另一个字符串替换字符串中所有出现的子字符串.
	 * 
	 * @param inString 要检查的{@code String}
	 * @param oldPattern 要替换的{@code String}
	 * @param newPattern 要插入的{@code String}
	 * 
	 * @return 替换后的{@code String}
	 */
	public static String replace(String inString, String oldPattern, String newPattern) {
		if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
			return inString;
		}
		int index = inString.indexOf(oldPattern);
		if (index == -1) {
			// no occurrence -> can return input as-is
			return inString;
		}

		int capacity = inString.length();
		if (newPattern.length() > oldPattern.length()) {
			capacity += 16;
		}
		StringBuilder sb = new StringBuilder(capacity);

		int pos = 0;  // our position in the old string
		int patLen = oldPattern.length();
		while (index >= 0) {
			sb.append(inString.substring(pos, index));
			sb.append(newPattern);
			pos = index + patLen;
			index = inString.indexOf(oldPattern, pos);
		}

		// append any characters to the right of a match
		sb.append(inString.substring(pos));
		return sb.toString();
	}

	/**
	 * 删除所有给定的子字符串.
	 * 
	 * @param inString 原始{@code String}
	 * @param pattern 删除所有出现的模式
	 * 
	 * @return 结果{@code String}
	 */
	public static String delete(String inString, String pattern) {
		return replace(inString, pattern, "");
	}

	/**
	 * 删除给定{@code String}中的任何字符.
	 * 
	 * @param inString 原始{@code String}
	 * @param charsToDelete 要删除的一组字符.
	 * E.g. "az\n"将删除 'a', 'z', 和新行.
	 * 
	 * @return 结果{@code String}
	 */
	public static String deleteAny(String inString, String charsToDelete) {
		if (!hasLength(inString) || !hasLength(charsToDelete)) {
			return inString;
		}

		StringBuilder sb = new StringBuilder(inString.length());
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (charsToDelete.indexOf(c) == -1) {
				sb.append(c);
			}
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// Convenience methods for working with formatted Strings
	//---------------------------------------------------------------------

	/**
	 * 用单引号引用给定的{@code String.
	 * 
	 * @param str 输入{@code String} (e.g. "myString")
	 * 
	 * @return 带引号的{@code String} (e.g. "'myString'"), 或{@code null}如果输入是{@code null}
	 */
	public static String quote(String str) {
		return (str != null ? "'" + str + "'" : null);
	}

	/**
	 * 如果它是{@code String}, 则将给定的Object转换为带有单引号的{@code String}; 否则保持Object原样.
	 * 
	 * @param obj 输入Object (e.g. "myString")
	 * 
	 * @return 带引号的{@code String} (e.g. "'myString'"); 或输入对象, 如果不是{@code String}
	 */
	public static Object quoteIfString(Object obj) {
		return (obj instanceof String ? quote((String) obj) : obj);
	}

	/**
	 * 取消限定由 '.' 限定的字符串.
	 * 例如, "this.name.is.qualified", 返回"qualified".
	 * 
	 * @param qualifiedName 限定的名称
	 */
	public static String unqualify(String qualifiedName) {
		return unqualify(qualifiedName, '.');
	}

	/**
	 * 取消限定由分隔符限定的字符串.
	 * 例如, "this:name:is:qualified"返回 "qualified", 如果使用':' 分隔符.
	 * 
	 * @param qualifiedName 限定的名称
	 * @param separator 分隔符
	 */
	public static String unqualify(String qualifiedName, char separator) {
		return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
	}

	/**
	 * 大写{@code String}, 根据{@link Character#toUpperCase(char)}将第一个字母更改为大写字母.
	 * 其他字母不会被更改.
	 * 
	 * @param str 要大写的{@code String}
	 * 
	 * @return 大写后的{@code String}
	 */
	public static String capitalize(String str) {
		return changeFirstCharacterCase(str, true);
	}

	/**
	 * 取消{@code String}的大写, 根据{@link Character#toLowerCase(char)}将第一个字母改为小写.
	 * 其他字母不会被更改.
	 * 
	 * @param str 要小写的{@code String}
	 * 
	 * @return 小写后的{@code String}
	 */
	public static String uncapitalize(String str) {
		return changeFirstCharacterCase(str, false);
	}

	private static String changeFirstCharacterCase(String str, boolean capitalize) {
		if (!hasLength(str)) {
			return str;
		}

		char baseChar = str.charAt(0);
		char updatedChar;
		if (capitalize) {
			updatedChar = Character.toUpperCase(baseChar);
		}
		else {
			updatedChar = Character.toLowerCase(baseChar);
		}
		if (baseChar == updatedChar) {
			return str;
		}

		char[] chars = str.toCharArray();
		chars[0] = updatedChar;
		return new String(chars, 0, chars.length);
	}

	/**
	 * 从给定的Java资源路径中提取文件名, e.g. {@code "mypath/myfile.txt" -> "myfile.txt"}.
	 * 
	 * @param path 文件路径 (may be {@code null})
	 * 
	 * @return 提取的文件名, 或{@code null}
	 */
	public static String getFilename(String path) {
		if (path == null) {
			return null;
		}

		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
	}

	/**
	 * 从给定的Java资源路径中提取文件扩展名, e.g. "mypath/myfile.txt" -> "txt".
	 * 
	 * @param path 文件路径 (may be {@code null})
	 * 
	 * @return 提取的文件名, 或{@code null}
	 */
	public static String getFilenameExtension(String path) {
		if (path == null) {
			return null;
		}

		int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
		if (extIndex == -1) {
			return null;
		}

		int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		if (folderIndex > extIndex) {
			return null;
		}

		return path.substring(extIndex + 1);
	}

	/**
	 * 从给定的Java资源路径中剥离文件扩展名, e.g. "mypath/myfile.txt" -> "mypath/myfile".
	 * 
	 * @param path 文件路径
	 * 
	 * @return 没有文件扩展名的路径
	 */
	public static String stripFilenameExtension(String path) {
		if (path == null) {
			return null;
		}

		int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
		if (extIndex == -1) {
			return path;
		}

		int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		if (folderIndex > extIndex) {
			return path;
		}

		return path.substring(0, extIndex);
	}

	/**
	 * 假定标准Java文件夹分隔符 (i.e. "/" 分隔符), 将给定的相对路径应用于给定的Java资源路径.
	 * 
	 * @param path 开始路径 (通常是完整的文件路径)
	 * @param relativePath 应用的相对路径 (相对于上面的完整文件路径)
	 * 
	 * @return 应用相对路径产生的完整文件路径
	 */
	public static String applyRelativePath(String path, String relativePath) {
		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		if (separatorIndex != -1) {
			String newPath = path.substring(0, separatorIndex);
			if (!relativePath.startsWith(FOLDER_SEPARATOR)) {
				newPath += FOLDER_SEPARATOR;
			}
			return newPath + relativePath;
		}
		else {
			return relativePath;
		}
	}

	/**
	 * 通过抑制 "path/.." 和内部简单点等序列来规范化路径.
	 * <p>结果便于路径比较. 对于其他用途, 请注意Windows分隔符 ("\") 由简单斜杠替换.
	 * 
	 * @param path 原始路径
	 * 
	 * @return 规范化的路径
	 */
	public static String cleanPath(String path) {
		if (path == null) {
			return null;
		}
		String pathToUse = replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);

		// 从路径中去除前缀​​以分析, 而不是将其视为第一个路径元素的一部分. 
		// 这对于正确解析像 "file:core/../core/io/Resource.class"这样的路径是必要的,
		// 其中".."应该只删除第一个"core"目录, 同时保留"file:"前缀.
		int prefixIndex = pathToUse.indexOf(':');
		String prefix = "";
		if (prefixIndex != -1) {
			prefix = pathToUse.substring(0, prefixIndex + 1);
			if (prefix.contains(FOLDER_SEPARATOR)) {
				prefix = "";
			}
			else {
				pathToUse = pathToUse.substring(prefixIndex + 1);
			}
		}
		if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
			prefix = prefix + FOLDER_SEPARATOR;
			pathToUse = pathToUse.substring(1);
		}

		String[] pathArray = delimitedListToStringArray(pathToUse, FOLDER_SEPARATOR);
		List<String> pathElements = new LinkedList<String>();
		int tops = 0;

		for (int i = pathArray.length - 1; i >= 0; i--) {
			String element = pathArray[i];
			if (CURRENT_PATH.equals(element)) {
				// 指向当前目录 - 删除它.
			}
			else if (TOP_PATH.equals(element)) {
				// 注册找到的顶级路径.
				tops++;
			}
			else {
				if (tops > 0) {
					// 将路径元素与对应于顶部路径的元素合并.
					tops--;
				}
				else {
					// 找到的正常路径元素.
					pathElements.add(0, element);
				}
			}
		}

		// 需要保留剩余的顶部路径.
		for (int i = 0; i < tops; i++) {
			pathElements.add(0, TOP_PATH);
		}

		return prefix + collectionToDelimitedString(pathElements, FOLDER_SEPARATOR);
	}

	/**
	 * 在规范化它们之后比较两个路径.
	 * 
	 * @param path1 第一个路径
	 * @param path2 第二个路径
	 * 
	 * @return 标准化后两个路径是否相等
	 */
	public static boolean pathEquals(String path1, String path2) {
		return cleanPath(path1).equals(cleanPath(path2));
	}

	/**
	 * 将给定的{@code String}表示解析为{@link Locale}.
	 * <p>这是{@link Locale#toString Locale's toString}的逆操作.
	 * 
	 * @param localeString 区域设置{@code String}:
	 * 遵循{@code Locale}的 {@code toString()}格式 ("en", "en_UK", etc), 也接受空格作为分隔符 (作为下划线的替代)
	 * 
	 * @return 相应的{@code Locale}实例, 或{@code null}
	 * @throws IllegalArgumentException 如果区域设置规范无效
	 */
	public static Locale parseLocaleString(String localeString) {
		String[] parts = tokenizeToStringArray(localeString, "_ ", false, false);
		String language = (parts.length > 0 ? parts[0] : "");
		String country = (parts.length > 1 ? parts[1] : "");

		validateLocalePart(language);
		validateLocalePart(country);

		String variant = "";
		if (parts.length > 2) {
			// 肯定有一个变体, 它是国家代码在国家代码和变体之间没有分隔符之后的所有内容.
			int endIndexOfCountryCode = localeString.indexOf(country, language.length()) + country.length();
			// 剥离任何前导'_'和空格, 剩下的就是变体.
			variant = trimLeadingWhitespace(localeString.substring(endIndexOfCountryCode));
			if (variant.startsWith("_")) {
				variant = trimLeadingCharacter(variant, '_');
			}
		}
		return (language.length() > 0 ? new Locale(language, country, variant) : null);
	}

	private static void validateLocalePart(String localePart) {
		for (int i = 0; i < localePart.length(); i++) {
			char ch = localePart.charAt(i);
			if (ch != ' ' && ch != '_' && ch != '#' && !Character.isLetterOrDigit(ch)) {
				throw new IllegalArgumentException(
						"Locale part \"" + localePart + "\" contains invalid characters");
			}
		}
	}

	/**
	 * 确定符合RFC 3066的语言标签, 用于 HTTP "Accept-Language" header.
	 * 
	 * @param locale 要转换为语言标签的区域设置
	 * 
	 * @return 符合RFC 3066的语言标签
	 */
	public static String toLanguageTag(Locale locale) {
		return locale.getLanguage() + (hasText(locale.getCountry()) ? "-" + locale.getCountry() : "");
	}

	/**
	 * 将给定的{@code timeZoneString}值解析为{@link TimeZone}.
	 * 
	 * @param timeZoneString 时区{@code String},
	 * 遵循{@link TimeZone#getTimeZone(String)}, 但在时区规范无效的情况下抛出{@link IllegalArgumentException}
	 * 
	 * @return 相应的{@link TimeZone}实例
	 * @throws IllegalArgumentException 如果时区规范无效
	 */
	public static TimeZone parseTimeZoneString(String timeZoneString) {
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
		if ("GMT".equals(timeZone.getID()) && !timeZoneString.startsWith("GMT")) {
			// We don't want that GMT fallback...
			throw new IllegalArgumentException("Invalid time zone specification '" + timeZoneString + "'");
		}
		return timeZone;
	}


	//---------------------------------------------------------------------
	// Convenience methods for working with String arrays
	//---------------------------------------------------------------------

	/**
	 * 将给定的{@code String}附加到给定的{@code String}数组, 返回一个由输入数组内容和给定{@code String}组成的新数组.
	 * 
	 * @param array 要追加的数组(can be {@code null})
	 * @param str 要附加的{@code String}
	 * 
	 * @return 新数组 (never {@code null})
	 */
	public static String[] addStringToArray(String[] array, String str) {
		if (ObjectUtils.isEmpty(array)) {
			return new String[] {str};
		}

		String[] newArr = new String[array.length + 1];
		System.arraycopy(array, 0, newArr, 0, array.length);
		newArr[array.length] = str;
		return newArr;
	}

	/**
	 * 将给定的{@code String}数组连接成一个, 重叠的数组元素包含两次.
	 * <p>保留原始数组中元素的顺序.
	 * 
	 * @param array1 第一个数组 (can be {@code null})
	 * @param array2 第二个数组 (can be {@code null})
	 * 
	 * @return 新数组 ({@code null} 如果给定的数组都是{@code null})
	 */
	public static String[] concatenateStringArrays(String[] array1, String[] array2) {
		if (ObjectUtils.isEmpty(array1)) {
			return array2;
		}
		if (ObjectUtils.isEmpty(array2)) {
			return array1;
		}

		String[] newArr = new String[array1.length + array2.length];
		System.arraycopy(array1, 0, newArr, 0, array1.length);
		System.arraycopy(array2, 0, newArr, array1.length, array2.length);
		return newArr;
	}

	/**
	 * 将给定的{@code String}数组合并为一个, 重叠的数组元素只包含一次.
	 * <p>保留原始数组中元素的顺序 (重叠元素除外, 它们仅在第一次出现时包含在内).
	 * 
	 * @param array1 第一个数组 (can be {@code null})
	 * @param array2 第二个数组 (can be {@code null})
	 * 
	 * @return 新数组 ({@code null} 如果给定的数组都是{@code null})
	 * @deprecated as of 4.3.15, in favor of manual merging via {@link LinkedHashSet}
	 * (with every entry included at most once, even entries within the first array)
	 */
	@Deprecated
	public static String[] mergeStringArrays(String[] array1, String[] array2) {
		if (ObjectUtils.isEmpty(array1)) {
			return array2;
		}
		if (ObjectUtils.isEmpty(array2)) {
			return array1;
		}

		List<String> result = new ArrayList<String>();
		result.addAll(Arrays.asList(array1));
		for (String str : array2) {
			if (!result.contains(str)) {
				result.add(str);
			}
		}
		return toStringArray(result);
	}

	/**
	 * 将给定的源{@code String}数组转换为有序数组.
	 * 
	 * @param array 源数组
	 * 
	 * @return 有序数组 (never {@code null})
	 */
	public static String[] sortStringArray(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return new String[0];
		}

		Arrays.sort(array);
		return array;
	}

	/**
	 * 将给定的{@code Collection}复制到{@code String}数组中.
	 * <p>{@code Collection}只能包含{@code String}元素.
	 * 
	 * @param collection 要复制的{@code Collection}
	 * 
	 * @return {@code String}数组
	 */
	public static String[] toStringArray(Collection<String> collection) {
		if (collection == null) {
			return null;
		}
		return collection.toArray(new String[collection.size()]);
	}

	/**
	 * 将给定的枚举复制到{@code String}数组中.
	 * 枚举必须仅包含{@code String}元素.
	 * 
	 * @param enumeration 要复制的枚举
	 * 
	 * @return {@code String}数组
	 */
	public static String[] toStringArray(Enumeration<String> enumeration) {
		if (enumeration == null) {
			return null;
		}
		return toStringArray(Collections.list(enumeration));
	}

	/**
	 * 修剪给定{@code String}数组的元素, 在每个数组上调用{@code String.trim()}.
	 * 
	 * @param array 原始的{@code String}数组
	 * 
	 * @return 带有修剪元素的结果数组 (大小相同)
	 */
	public static String[] trimArrayElements(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return new String[0];
		}

		String[] result = new String[array.length];
		for (int i = 0; i < array.length; i++) {
			String element = array[i];
			result[i] = (element != null ? element.trim() : null);
		}
		return result;
	}

	/**
	 * 从给定数组中删除重复的字符串.
	 * <p>从4.2开始, 它保留了原始顺序, 因为它使用了{@link LinkedHashSet}.
	 * 
	 * @param array {@code String}数组
	 * 
	 * @return 一个没有重复元素的数组, 按自然顺序排序
	 */
	public static String[] removeDuplicateStrings(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		Set<String> set = new LinkedHashSet<String>();
		for (String element : array) {
			set.add(element);
		}
		return toStringArray(set);
	}

	/**
	 * 在第一次出现分隔符时拆分{@code String}.
	 * 结果中不包括分隔符.
	 * 
	 * @param toSplit 要拆分的字符串
	 * @param delimiter 拆分字符串的分隔符
	 * 
	 * @return 索引为0位于分隔符之前, 索引1位于分隔符之后的的两个元素数组 (两个元素都不包含分隔符);
	 * 或{@code null}如果在给定输入{@code String}中找不到分隔符
	 */
	public static String[] split(String toSplit, String delimiter) {
		if (!hasLength(toSplit) || !hasLength(delimiter)) {
			return null;
		}
		int offset = toSplit.indexOf(delimiter);
		if (offset < 0) {
			return null;
		}

		String beforeDelimiter = toSplit.substring(0, offset);
		String afterDelimiter = toSplit.substring(offset + delimiter.length());
		return new String[] {beforeDelimiter, afterDelimiter};
	}

	/**
	 * 获取一个字符串数组, 并根据给定的分隔符拆分每个元素.
	 * 然后生成{@code Properties}实例, 分隔符的左侧提供Key, 分隔符的右侧提供值.
	 * <p>在将它们添加到{@code Properties}之前修剪键和值.
	 * 
	 * @param array 要处理的数组
	 * @param delimiter 拆分每个元素的分隔符 (通常是等号)
	 * 
	 * @return 表示数组内容的{@code Properties}实例, 或{@code null} 如果要处理的数组为{@code null}或为空
	 */
	public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter) {
		return splitArrayElementsIntoProperties(array, delimiter, null);
	}

	/**
	 * 获取一个字符串数组, 并根据给定的分隔符拆分每个元素.
	 * 然后生成{@code Properties}实例, 分隔符的左侧提供键, 分隔符的右侧提供值.
	 * <p>在将它们添加到{@code Properties}实例之前, 将修剪键和值.
	 * 
	 * @param array 要处理的数组
	 * @param delimiter 拆分每个元素的分隔符 (通常是等号)
	 * @param charsToDelete 在尝试拆分操作之前从每个元素中删除一个或多个字符 (通常是引号符号), 或{@code null} 如果不应该删除
	 * 
	 * @return 表示数组内容的{@code Properties}实例, 或{@code null} 如果要处理的数组为{@code null}或为空
	 */
	public static Properties splitArrayElementsIntoProperties(
			String[] array, String delimiter, String charsToDelete) {

		if (ObjectUtils.isEmpty(array)) {
			return null;
		}

		Properties result = new Properties();
		for (String element : array) {
			if (charsToDelete != null) {
				element = deleteAny(element, charsToDelete);
			}
			String[] splittedElement = split(element, delimiter);
			if (splittedElement == null) {
				continue;
			}
			result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
		}
		return result;
	}

	/**
	 * 通过{@link StringTokenizer}将给定的{@code String}切分为{@code String}数组.
	 * <p>修剪标记并省略空标记.
	 * <p>给定的{@code delimiters}字符串可以包含任意数量的分隔符.
	 * 这些字符中的每一个都可用于分隔令牌.
	 * 分隔符始终是单个字符; 对于多字符分隔符, 考虑使用{@link #delimitedListToStringArray}.
	 * 
	 * @param str 要切分的{@code String}
	 * @param delimiters 分隔符, 合并为{@code String} (每个字符都被单独视为分隔符)
	 * 
	 * @return 切分后的数组
	 */
	public static String[] tokenizeToStringArray(String str, String delimiters) {
		return tokenizeToStringArray(str, delimiters, true, true);
	}

	/**
	 * 通过{@link StringTokenizer}将给定的{@code String}切分为{@code String}数组.
	 * <p>给定的{@code delimiters}字符串可以包含任意数量的分隔符.
	 * 这些字符中的每一个都可用于分隔token.
	 * 分隔符始终是单个字符; 对于多字符分隔符, 考虑使用{@link #delimitedListToStringArray}.
	 * 
	 * @param str 要切分的{@code String}
	 * @param delimiters 分隔符, 合并为{@code String} (每个字符都被单独视为分隔符)
	 * @param trimTokens 通过{@link String#trim()}修剪token
	 * @param ignoreEmptyTokens 省略结果数组中的空token
	 * (仅适用于修剪后空的token; StringTokenizer首先不会将后续分隔符视为token).
	 * 
	 * @return 切分后的数组
	 */
	public static String[] tokenizeToStringArray(
			String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

		if (str == null) {
			return null;
		}

		StringTokenizer st = new StringTokenizer(str, delimiters);
		List<String> tokens = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (trimTokens) {
				token = token.trim();
			}
			if (!ignoreEmptyTokens || token.length() > 0) {
				tokens.add(token);
			}
		}
		return toStringArray(tokens);
	}

	/**
	 * 获取作为分隔列表的{@code String}, 并将其转换为{@code String}数组.
	 * <p>单个{@code delimiter}可能包含多个字符, 但它仍将被视为单个分隔符字符串,
	 * 而不是一堆潜在的分隔符字符, 与{@link #tokenizeToStringArray}相反.
	 * 
	 * @param str 输入{@code String}
	 * @param delimiter 元素之间的分隔符 (这是一个单独的分隔符, 而不是一堆单独的分隔符字符)
	 * 
	 * @return 列表中的token数组
	 */
	public static String[] delimitedListToStringArray(String str, String delimiter) {
		return delimitedListToStringArray(str, delimiter, null);
	}

	/**
	 * 获取作为分隔列表的{@code String}, 并将其转换为{@code String}数组.
	 * <p>单个{@code delimiter}可能包含多个字符, 但它仍将被视为单个分隔符字符串,
	 * 而不是一堆潜在的分隔符字符, 与{@link #tokenizeToStringArray}相反.
	 * 
	 * @param str 输入{@code String}
	 * @param delimiter 元素之间的分隔符 (这是一个单独的分隔符, 而不是一堆单独的分隔符字符)
	 * @param charsToDelete 要删除的一组字符;
	 * 用于删除不需要的换行符: e.g. "\r\n\f" 将删除{@code String}中的所有新行和换行符
	 * 
	 * @return 列表中的token数组
	 */
	public static String[] delimitedListToStringArray(String str, String delimiter, String charsToDelete) {
		if (str == null) {
			return new String[0];
		}
		if (delimiter == null) {
			return new String[] {str};
		}

		List<String> result = new ArrayList<String>();
		if ("".equals(delimiter)) {
			for (int i = 0; i < str.length(); i++) {
				result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
			}
		}
		else {
			int pos = 0;
			int delPos;
			while ((delPos = str.indexOf(delimiter, pos)) != -1) {
				result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
				pos = delPos + delimiter.length();
			}
			if (str.length() > 0 && pos <= str.length()) {
				// 添加其余的String, 但不是空输入.
				result.add(deleteAny(str.substring(pos), charsToDelete));
			}
		}
		return toStringArray(result);
	}

	/**
	 * 将逗号分隔的列表 (e.g., CSV文件中的行) 转换为字符串数组.
	 * 
	 * @param str 输入{@code String}
	 * 
	 * @return 一个字符串数组, 或者在空输入的情况下为空数组
	 */
	public static String[] commaDelimitedListToStringArray(String str) {
		return delimitedListToStringArray(str, ",");
	}

	/**
	 * 将逗号分隔的列表 (e.g., CSV文件中的行) 转换为字符串数组.
	 * <p>请注意, 这将禁止重复, 从4.2开始, 返回的集合中的元素将保留{@link LinkedHashSet}中的原始顺序.
	 * 
	 * @param str 输入{@code String}
	 * 
	 * @return 列表中的一组{@code String}条目
	 */
	public static Set<String> commaDelimitedListToSet(String str) {
		Set<String> set = new LinkedHashSet<String>();
		String[] tokens = commaDelimitedListToStringArray(str);
		for (String token : tokens) {
			set.add(token);
		}
		return set;
	}

	/**
	 * 将{@link Collection}转换为分隔的{@code String} (e.g. CSV).
	 * <p>对{@code toString()}实现很有用.
	 * 
	 * @param coll 要转换的{@code Collection}
	 * @param delim 要使用的分隔符 (通常为 ",")
	 * @param prefix 每个元素的前缀
	 * @param suffix 每个元素的后缀
	 * 
	 * @return 带分隔符的{@code String}
	 */
	public static String collectionToDelimitedString(Collection<?> coll, String delim, String prefix, String suffix) {
		if (CollectionUtils.isEmpty(coll)) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		Iterator<?> it = coll.iterator();
		while (it.hasNext()) {
			sb.append(prefix).append(it.next()).append(suffix);
			if (it.hasNext()) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}

	/**
	 * 将{@code Collection}转换为分隔的{@code String} (e.g. CSV).
	 * <p>对{@code toString()}实现很有用.
	 * 
	 * @param coll 要转换的{@code Collection}
	 * @param delim 要使用的分隔符 (通常为 ",")
	 * 
	 * @return 带分隔符的{@code String}
	 */
	public static String collectionToDelimitedString(Collection<?> coll, String delim) {
		return collectionToDelimitedString(coll, delim, "", "");
	}

	/**
	 * 将{@code Collection}转换为分隔的{@code String} (e.g. CSV).
	 * <p>对{@code toString()}实现很有用.
	 * 
	 * @param coll 要转换的{@code Collection}
	 * 
	 * @return 带分隔符的{@code String}
	 */
	public static String collectionToCommaDelimitedString(Collection<?> coll) {
		return collectionToDelimitedString(coll, ",");
	}

	/**
	 * 将{@code String}数组转换为分隔的{@code String} (e.g. CSV).
	 * <p>对{@code toString()}实现很有用.
	 * 
	 * @param arr 要显示的数组
	 * @param delim 要使用的分隔符 (通常为 ",")
	 * 
	 * @return 带分隔符的{@code String}
	 */
	public static String arrayToDelimitedString(Object[] arr, String delim) {
		if (ObjectUtils.isEmpty(arr)) {
			return "";
		}
		if (arr.length == 1) {
			return ObjectUtils.nullSafeToString(arr[0]);
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				sb.append(delim);
			}
			sb.append(arr[i]);
		}
		return sb.toString();
	}

	/**
	 * 将{@code String}数组转换为逗号分隔的{@code String} (i.e., CSV).
	 * <p>对{@code toString()}实现很有用.
	 * 
	 * @param arr 要显示的数组
	 * 
	 * @return 带分隔符的{@code String}
	 */
	public static String arrayToCommaDelimitedString(Object[] arr) {
		return arrayToDelimitedString(arr, ",");
	}
}
