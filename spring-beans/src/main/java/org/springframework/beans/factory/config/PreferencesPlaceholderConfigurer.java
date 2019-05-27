package org.springframework.beans.factory.config;

import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.InitializingBean;

/**
 * PropertyPlaceholderConfigurer的子类, 支持JDK 1.4的Preferences API ({@code java.util.prefs}).
 *
 * <p>尝试首先将占位符解析为用户首选项中的键, 然后到系统首选项中, 然后到此配置程序的属性中.
 * 因此, 如果没有定义相应的首选项, 则行为类似于PropertyPlaceholderConfigurer.
 *
 * <p>支持系统和用户首选项树的自定义路径.
 * 还支持占位符中指定的自定义路径 ("myPath/myPlaceholderKey"). 如果未指定, 则使用相应的根节点.
 */
public class PreferencesPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements InitializingBean {

	private String systemTreePath;

	private String userTreePath;

	private Preferences systemPrefs;

	private Preferences userPrefs;


	/**
	 * 在系统首选项树中设置路径以用于解析占位符. 默认为根节点.
	 */
	public void setSystemTreePath(String systemTreePath) {
		this.systemTreePath = systemTreePath;
	}

	/**
	 * 在系统首选项树中设置路径以用于解析占位符. 默认为根节点.
	 */
	public void setUserTreePath(String userTreePath) {
		this.userTreePath = userTreePath;
	}


	/**
	 * 此实现实时地获取所需系统和用户树节点的Preferences实例.
	 */
	@Override
	public void afterPropertiesSet() {
		this.systemPrefs = (this.systemTreePath != null) ?
				Preferences.systemRoot().node(this.systemTreePath) : Preferences.systemRoot();
		this.userPrefs = (this.userTreePath != null) ?
				Preferences.userRoot().node(this.userTreePath) : Preferences.userRoot();
	}

	/**
	 * 此实现尝试首先将占位符解析为用户首选项中的键, 然后在系统首选项中, 然后在传入的属性中.
	 */
	@Override
	protected String resolvePlaceholder(String placeholder, Properties props) {
		String path = null;
		String key = placeholder;
		int endOfPath = placeholder.lastIndexOf('/');
		if (endOfPath != -1) {
			path = placeholder.substring(0, endOfPath);
			key = placeholder.substring(endOfPath + 1);
		}
		String value = resolvePlaceholder(path, key, this.userPrefs);
		if (value == null) {
			value = resolvePlaceholder(path, key, this.systemPrefs);
			if (value == null) {
				value = props.getProperty(placeholder);
			}
		}
		return value;
	}

	/**
	 * 根据给定的首选项解析给定的路径和键.
	 * 
	 * @param path 首选项路径 ('/' 之前的占位符部分)
	 * @param key 首选项key ('/' 之后的占位符部分)
	 * @param preferences 要解析的首选项
	 * 
	 * @return 占位符的值, 或{@code null}
	 */
	protected String resolvePlaceholder(String path, String key, Preferences preferences) {
		if (path != null) {
			 // Do not create the node if it does not exist...
			try {
				if (preferences.nodeExists(path)) {
					return preferences.node(path).get(key, null);
				}
				else {
					return null;
				}
			}
			catch (BackingStoreException ex) {
				throw new BeanDefinitionStoreException("Cannot access specified node path [" + path + "]", ex);
			}
		}
		else {
			return preferences.get(key, null);
		}
	}

}
