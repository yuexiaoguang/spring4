package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.StringUtils;

/**
 * 用于评估给定目标对象的属性路径的{@link FactoryBean}.
 *
 * <p>可以直接指定目标对象, 也可以通过bean名称指定目标对象.
 *
 * <p>用法示例:
 *
 * <pre class="code">&lt;!-- target bean to be referenced by name --&gt;
 * &lt;bean id="tb" class="org.springframework.beans.TestBean" singleton="false"&gt;
 *   &lt;property name="age" value="10"/&gt;
 *   &lt;property name="spouse"&gt;
 *     &lt;bean class="org.springframework.beans.TestBean"&gt;
 *       &lt;property name="age" value="11"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 12, which is the value of property 'age' of the inner bean --&gt;
 * &lt;bean id="propertyPath1" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetObject"&gt;
 *     &lt;bean class="org.springframework.beans.TestBean"&gt;
 *       &lt;property name="age" value="12"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 *   &lt;property name="propertyPath" value="age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 11, which is the value of property 'spouse.age' of bean 'tb' --&gt;
 * &lt;bean id="propertyPath2" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetBeanName" value="tb"/&gt;
 *   &lt;property name="propertyPath" value="spouse.age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 10, which is the value of property 'age' of bean 'tb' --&gt;
 * &lt;bean id="tb.age" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"/&gt;</pre>
 *
 * <p>如果在配置文件中使用Spring 2.0和XML Schema支持, 则还可以使用以下配置样式进行属性路径访问.
 * (有关更多示例, 另请参阅Spring参考手册中标题为“基于XML模式的配置”的附录.)
 *
 * <pre class="code"> &lt;!-- will result in 10, which is the value of property 'age' of bean 'tb' --&gt;
 * &lt;util:property-path id="name" path="testBean.age"/&gt;</pre>
 *
 * 感谢Matthias Ernst提出的建议和初始原型!
 */
public class PropertyPathFactoryBean implements FactoryBean<Object>, BeanNameAware, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(PropertyPathFactoryBean.class);

	private BeanWrapper targetBeanWrapper;

	private String targetBeanName;

	private String propertyPath;

	private Class<?> resultType;

	private String beanName;

	private BeanFactory beanFactory;


	/**
	 * 指定要应用属性路径的目标对象.
	 * 或者, 指定目标bean名称.
	 * 
	 * @param targetObject 目标对象, 例如bean引用或内部bean
	 */
	public void setTargetObject(Object targetObject) {
		this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(targetObject);
	}

	/**
	 * 指定要应用属性路径的目标bean的名称.
	 * 或者, 直接指定目标对象.
	 * 
	 * @param targetBeanName 要在bean工厂中查找的bean名称 (e.g. "testBean")
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = StringUtils.trimAllWhitespace(targetBeanName);
	}

	/**
	 * 指定要应用于目标的属性路径.
	 * 
	 * @param propertyPath 属性路径, 可能是嵌套的 (e.g. "age" or "spouse.age")
	 */
	public void setPropertyPath(String propertyPath) {
		this.propertyPath = StringUtils.trimAllWhitespace(propertyPath);
	}

	/**
	 * 通过评估属性路径指定结果的类型.
	 * <p>Note: 对于直接指定的目标对象或单例目标bean, 这不是必需的, 其中类型可以通过内省来确定.
	 * 如果您需要按类型匹配, 只需在原型目标的情况下指定它 (例如自动装配).
	 * 
	 * @param resultType 结果类型, 例如 "java.lang.Integer"
	 */
	public void setResultType(Class<?> resultType) {
		this.resultType = resultType;
	}

	/**
	 * 如果既未指定"targetObject", 也未指定"targetBeanName"或"propertyPath",
	 * 则此PropertyPathFactoryBean的bean名称将被解释为"beanName.property"模式.
	 * 这允许简单的bean定义只有id/name.
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;

		if (this.targetBeanWrapper != null && this.targetBeanName != null) {
			throw new IllegalArgumentException("Specify either 'targetObject' or 'targetBeanName', not both");
		}

		if (this.targetBeanWrapper == null && this.targetBeanName == null) {
			if (this.propertyPath != null) {
				throw new IllegalArgumentException(
						"Specify 'targetObject' or 'targetBeanName' in combination with 'propertyPath'");
			}

			// No other properties specified: check bean name.
			int dotIndex = this.beanName.indexOf('.');
			if (dotIndex == -1) {
				throw new IllegalArgumentException(
						"Neither 'targetObject' nor 'targetBeanName' specified, and PropertyPathFactoryBean " +
						"bean name '" + this.beanName + "' does not follow 'beanName.property' syntax");
			}
			this.targetBeanName = this.beanName.substring(0, dotIndex);
			this.propertyPath = this.beanName.substring(dotIndex + 1);
		}

		else if (this.propertyPath == null) {
			// either targetObject or targetBeanName specified
			throw new IllegalArgumentException("'propertyPath' is required");
		}

		if (this.targetBeanWrapper == null && this.beanFactory.isSingleton(this.targetBeanName)) {
			// Eagerly fetch singleton target bean, and determine result type.
			Object bean = this.beanFactory.getBean(this.targetBeanName);
			this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
			this.resultType = this.targetBeanWrapper.getPropertyType(this.propertyPath);
		}
	}


	@Override
	public Object getObject() throws BeansException {
		BeanWrapper target = this.targetBeanWrapper;
		if (target != null) {
			if (logger.isWarnEnabled() && this.targetBeanName != null &&
					this.beanFactory instanceof ConfigurableBeanFactory &&
					((ConfigurableBeanFactory) this.beanFactory).isCurrentlyInCreation(this.targetBeanName)) {
				logger.warn("Target bean '" + this.targetBeanName + "' is still in creation due to a circular " +
						"reference - obtained value for property '" + this.propertyPath + "' may be outdated!");
			}
		}
		else {
			// Fetch prototype target bean...
			Object bean = this.beanFactory.getBean(this.targetBeanName);
			target = PropertyAccessorFactory.forBeanPropertyAccess(bean);
		}
		return target.getPropertyValue(this.propertyPath);
	}

	@Override
	public Class<?> getObjectType() {
		return this.resultType;
	}

	/**
	 * 虽然此FactoryBean通常用于单例目标, 但属性路径的getter可能会为每个调用返回一个新对象,
	 * 所以必须假设我们没有为每个 {@link #getObject()}调用返回相同的对象.
	 */
	@Override
	public boolean isSingleton() {
		return false;
	}

}
