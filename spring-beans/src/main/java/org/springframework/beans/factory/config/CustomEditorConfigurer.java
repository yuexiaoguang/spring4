package org.springframework.beans.factory.config;

import java.beans.PropertyEditor;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanFactoryPostProcessor}实现, 允许方便地注册自定义{@link PropertyEditor 属性编辑器}.
 *
 * <p>如果您想注册{@link PropertyEditor}实例, 从Spring 2.0开始的推荐用法是使用自定义{@link PropertyEditorRegistrar}实现,
 * 然后在给定的{@link org.springframework.beans.PropertyEditorRegistry 注册表}上注册任何所需的编辑器实例.
 * 每个PropertyEditorRegistrar都可以注册任意数量的自定义编辑器.
 *
 * <pre class="code">
 * &lt;bean id="customEditorConfigurer" class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *   &lt;property name="propertyEditorRegistrars"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="mypackage.MyCustomDateEditorRegistrar"/&gt;
 *       &lt;bean class="mypackage.MyObjectEditorRegistrar"/&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>
 * 通过{@code customEditors}属性注册{@link PropertyEditor}类完全没问题.
 * Spring将为每次编辑尝试创建它们的新实例:
 *
 * <pre class="code">
 * &lt;bean id="customEditorConfigurer" class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *   &lt;property name="customEditors"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="java.util.Date" value="mypackage.MyCustomDateEditor"/&gt;
 *       &lt;entry key="mypackage.MyObject" value="mypackage.MyObjectEditor"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>
 * Note, 不应该通过{@code customEditors}属性注册{@link PropertyEditor} bean实例,
 * 因为{@link PropertyEditor}是有状态的, 然后必须为每次编辑尝试synchronize实例.
 * 如果需要控制{@link PropertyEditor}的实例化过程, 使用{@link PropertyEditorRegistrar}注册它们.
 *
 * <p>
 * 还支持"java.lang.String[]"-样式数组类名和基本类名 (e.g. "boolean").
 * 委托给{@link ClassUtils}进行实际的类名解析.
 *
 * <p><b>NOTE:</b> 使用此配置器注册的自定义属性编辑器不适用于数据绑定.
 * 需要在{@link org.springframework.validation.DataBinder}上注册用于数据绑定的自定义编辑器:
 * 使用公共基类或委托给常见的PropertyEditorRegistrar实现来重用编辑器注册.
 */
public class CustomEditorConfigurer implements BeanFactoryPostProcessor, Ordered {

	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	private PropertyEditorRegistrar[] propertyEditorRegistrars;

	private Map<Class<?>, Class<? extends PropertyEditor>> customEditors;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 指定{@link PropertyEditorRegistrar PropertyEditorRegistrars}以应用于当前应用程序上下文中定义的bean.
	 * <p>这允许与{@link org.springframework.validation.DataBinder DataBinders}等共享{@code PropertyEditorRegistrars}.
	 * 此外, 它避免了在自定义编辑器上进行同步的需要:
	 * {@code PropertyEditorRegistrar}将始终为每个bean创建尝试创建新的编辑器实例.
	 */
	public void setPropertyEditorRegistrars(PropertyEditorRegistrar[] propertyEditorRegistrars) {
		this.propertyEditorRegistrars = propertyEditorRegistrars;
	}

	/**
	 * 指定要通过{@link Map}注册的自定义编辑器, 使用所需类型的类名作为键, 并将关联的{@link PropertyEditor}的类名作为值.
	 */
	public void setCustomEditors(Map<Class<?>, Class<? extends PropertyEditor>> customEditors) {
		this.customEditors = customEditors;
	}


	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.propertyEditorRegistrars != null) {
			for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
				beanFactory.addPropertyEditorRegistrar(propertyEditorRegistrar);
			}
		}
		if (this.customEditors != null) {
			for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : this.customEditors.entrySet()) {
				Class<?> requiredType = entry.getKey();
				Class<? extends PropertyEditor> propertyEditorClass = entry.getValue();
				beanFactory.registerCustomEditor(requiredType, propertyEditorClass);
			}
		}
	}

}
