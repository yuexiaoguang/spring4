package org.springframework.context.support;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractRefreshableApplicationContext} 子类, 用于添加指定配置位置的常见处理.
 * 用作基于XML的应用程序上下文实现的基类, 例如{@link ClassPathXmlApplicationContext} 和 {@link FileSystemXmlApplicationContext},
 * 以及 {@link org.springframework.web.context.support.XmlWebApplicationContext}和
 * {@link org.springframework.web.portlet.context.XmlPortletApplicationContext}.
 */
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext
		implements BeanNameAware, InitializingBean {

	private String[] configLocations;

	private boolean setIdCalled = false;


	public AbstractRefreshableConfigApplicationContext() {
	}

	/**
	 * @param parent 父级上下文
	 */
	public AbstractRefreshableConfigApplicationContext(ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 以init-param样式设置此应用程序上下文的配置位置, i.e. 不同的位置用逗号, 分号或空格分隔.
	 * <p>如果未设置, 则实现可以适当地使用默认值.
	 */
	public void setConfigLocation(String location) {
		setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
	}

	/**
	 * 设置此应用程序上下文的配置位置.
	 * <p>如果未设置, 则实现可以适当地使用默认值.
	 */
	public void setConfigLocations(String... locations) {
		if (locations != null) {
			Assert.noNullElements(locations, "Config locations must not be null");
			this.configLocations = new String[locations.length];
			for (int i = 0; i < locations.length; i++) {
				this.configLocations[i] = resolvePath(locations[i]).trim();
			}
		}
		else {
			this.configLocations = null;
		}
	}

	/**
	 * 返回一个资源位置数组, 引用应该构建此上下文的XML bean定义文件.
	 * 还可以包含位置模式, 这些模式将通过ResourcePatternResolver解析.
	 * <p>默认实现返回 {@code null}.
	 * 子类可以覆盖它以提供一组资源位置来加载bean定义.
	 * 
	 * @return 资源位置数组, 或{@code null}
	 */
	protected String[] getConfigLocations() {
		return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
	}

	/**
	 * 如果未指定显式配置位置, 则返回要使用的默认配置位置.
	 * <p>默认实现返回 {@code null}, 需要显式配置位置.
	 * 
	 * @return 一组默认配置位置
	 */
	protected String[] getDefaultConfigLocations() {
		return null;
	}

	/**
	 * 解析给定路径, 必要时用相应的环境属性值替换占位符. 应用于配置位置.
	 * 
	 * @param path 原始文件路径
	 * 
	 * @return 已解析的文件路径
	 */
	protected String resolvePath(String path) {
		return getEnvironment().resolveRequiredPlaceholders(path);
	}


	@Override
	public void setId(String id) {
		super.setId(id);
		this.setIdCalled = true;
	}

	/**
	 * 对于上下文实例本身定义为bean的情况, 默认情况下将此上下文的id设置为bean名称.
	 */
	@Override
	public void setBeanName(String name) {
		if (!this.setIdCalled) {
			super.setId(name);
			setDisplayName("ApplicationContext '" + name + "'");
		}
	}

	/**
	 * 如果未在具体上下文的构造函数中刷新, 则触发{@link #refresh()}.
	 */
	@Override
	public void afterPropertiesSet() {
		if (!isActive()) {
			refresh();
		}
	}

}
