package org.springframework.web.util;

import javax.servlet.ServletContext;

import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * 解析文本中的占位符的工具类. 通常应用于文件路径.
 *
 * <p>文本可能包含 {@code ${...}}占位符, 需要解析为servlet上下文init参数或系统属性: e.g. {@code ${user.dir}}.
 * 可以使用键和值之间的":" 分隔符提供默认值.
 */
public abstract class ServletContextPropertyUtils {

    private static final PropertyPlaceholderHelper strictHelper =
            new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, false);

    private static final PropertyPlaceholderHelper nonStrictHelper =
            new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true);


	/**
	 * 解析给定文本中的 ${...}占位符, 将其替换为相应的servlet上下文init参数或系统属性值.
     * 
     * @param text 要解析的字符串
     * @param servletContext 用于查找的servletContext.
	 * 
	 * @return 解析的String
	 * @throws IllegalArgumentException 如果有一个无法解析的占位符
	 */
	public static String resolvePlaceholders(String text, ServletContext servletContext) {
		return resolvePlaceholders(text, servletContext, false);
	}

	/**
	 * 解析给定文本中的 ${...}占位符, 将其替换为相应的servlet上下文init参数或系统属性值.
	 * 如果标志设置为true, 则忽略没有默认值的不可解析的占位符, 并保持不变.
	 * 
	 * @param text 要解析的字符串
     * @param servletContext 用于查找的servletContext
	 * @param ignoreUnresolvablePlaceholders 是否忽略不可解析的占位符
	 * 
	 * @return 已解析的字符串
	 * @throws IllegalArgumentException 如果存在无法解析的占位符且标志为false
	 */
	public static String resolvePlaceholders(String text, ServletContext servletContext, boolean ignoreUnresolvablePlaceholders) {
		PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
		return helper.replacePlaceholders(text, new ServletContextPlaceholderResolver(text, servletContext));
	}


	private static class ServletContextPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

        private final String text;

        private final ServletContext servletContext;

        public ServletContextPlaceholderResolver(String text, ServletContext servletContext) {
            this.text = text;
            this.servletContext = servletContext;
        }

        @Override
		public String resolvePlaceholder(String placeholderName) {
            try {
                String propVal = this.servletContext.getInitParameter(placeholderName);
				if (propVal == null) {
					// 回退到系统属性.
					propVal = System.getProperty(placeholderName);
					if (propVal == null) {
						// 回退到搜索系统环境.
						propVal = System.getenv(placeholderName);
					}
				}
				return propVal;
			}
            catch (Throwable ex) {
                System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" +
                        this.text + "] as ServletContext init-parameter or system property: " + ex);
                return null;
            }
        }
    }
}
