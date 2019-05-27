package org.springframework.util;

/**
 * 解析文本中的占位符的帮助类. 通常应用于文件路径.
 *
 * <p>文本可能包含{@code ${...}}占位符, 以作为系统属性进行解析: e.g. {@code ${user.dir}}.
 * 可以使用键和值之间的 ":" 分隔符提供默认值.
 */
public abstract class SystemPropertyUtils {

	/** 系统属性占位符的前缀: "${" */
	public static final String PLACEHOLDER_PREFIX = "${";

	/** 系统属性占位符的后缀: "}" */
	public static final String PLACEHOLDER_SUFFIX = "}";

	/** 系统属性占位符的值分隔符: ":" */
	public static final String VALUE_SEPARATOR = ":";


	private static final PropertyPlaceholderHelper strictHelper =
			new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, false);

	private static final PropertyPlaceholderHelper nonStrictHelper =
			new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true);


	/**
	 * 解析给定文本中的{@code ${...}}占位符, 将其替换为相应的系统属性值.
	 * 
	 * @param text 要解析的字符串
	 * 
	 * @return 已解析的字符串
	 * @throws IllegalArgumentException 如果有一个无法解析的占位符
	 */
	public static String resolvePlaceholders(String text) {
		return resolvePlaceholders(text, false);
	}

	/**
	 * 解析给定文本中的{@code ${...}}占位符, 将其替换为相应的系统属性值.
	 * 如果标志设置为{@code true}, 则忽略没有默认值的不可解析的占位符, 并保持不变.
	 * 
	 * @param text 要解析的字符串
	 * @param ignoreUnresolvablePlaceholders 是否要忽略未解析的占位符
	 * 
	 * @return 已解析的字符串
	 * @throws IllegalArgumentException 如果存在无法解析的占位符, 且"ignoreUnresolvablePlaceholders"标志为{@code false}
	 */
	public static String resolvePlaceholders(String text, boolean ignoreUnresolvablePlaceholders) {
		PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
		return helper.replacePlaceholders(text, new SystemPropertyPlaceholderResolver(text));
	}


	/**
	 * PlaceholderResolver实现, 可解析系统属性和系统环境变量.
	 */
	private static class SystemPropertyPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

		private final String text;

		public SystemPropertyPlaceholderResolver(String text) {
			this.text = text;
		}

		@Override
		public String resolvePlaceholder(String placeholderName) {
			try {
				String propVal = System.getProperty(placeholderName);
				if (propVal == null) {
					// 回到搜索系统环境.
					propVal = System.getenv(placeholderName);
				}
				return propVal;
			}
			catch (Throwable ex) {
				System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" +
						this.text + "] as system property: " + ex);
				return null;
			}
		}
	}
}
