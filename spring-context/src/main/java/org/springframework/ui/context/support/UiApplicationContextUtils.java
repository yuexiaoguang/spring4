package org.springframework.ui.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.ThemeSource;

/**
 * UI应用程序上下文实现的实用程序类.
 * 为名为"themeSource"的特殊bean提供支持, 类型为{@link org.springframework.ui.context.ThemeSource}.
 */
public abstract class UiApplicationContextUtils {

	/**
	 * 工厂中ThemeSource bean的名称.
	 * 如果未提供, 则将主题解析委托给父级.
	 */
	public static final String THEME_SOURCE_BEAN_NAME = "themeSource";


	private static final Log logger = LogFactory.getLog(UiApplicationContextUtils.class);


	/**
	 * 初始化给定应用程序上下文的ThemeSource, 自动检测名为"themeSource"的bean.
	 * 如果没有找到这样的bean, 将使用默认(empty) ThemeSource.
	 * 
	 * @param context 当前的应用程序上下文
	 * 
	 * @return 初始化的主题源 (will never be {@code null})
	 */
	public static ThemeSource initThemeSource(ApplicationContext context) {
		if (context.containsLocalBean(THEME_SOURCE_BEAN_NAME)) {
			ThemeSource themeSource = context.getBean(THEME_SOURCE_BEAN_NAME, ThemeSource.class);
			// Make ThemeSource aware of parent ThemeSource.
			if (context.getParent() instanceof ThemeSource && themeSource instanceof HierarchicalThemeSource) {
				HierarchicalThemeSource hts = (HierarchicalThemeSource) themeSource;
				if (hts.getParentThemeSource() == null) {
					// 如果尚未注册父级ThemeSource, 则仅将父级上下文设置为父级ThemeSource.
					hts.setParentThemeSource((ThemeSource) context.getParent());
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Using ThemeSource [" + themeSource + "]");
			}
			return themeSource;
		}
		else {
			// 使用默认的ThemeSource可以接受getTheme调用, 委托给父级上下文的默认值或本地ResourceBundleThemeSource.
			HierarchicalThemeSource themeSource = null;
			if (context.getParent() instanceof ThemeSource) {
				themeSource = new DelegatingThemeSource();
				themeSource.setParentThemeSource((ThemeSource) context.getParent());
			}
			else {
				themeSource = new ResourceBundleThemeSource();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate ThemeSource with name '" + THEME_SOURCE_BEAN_NAME +
						"': using default [" + themeSource + "]");
			}
			return themeSource;
		}
	}
}
