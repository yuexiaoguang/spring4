package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 当bean依赖于bean工厂定义中未指定的其他bean或简单属性时抛出异常, 尽管启用了依赖性检查.
 */
@SuppressWarnings("serial")
public class UnsatisfiedDependencyException extends BeanCreationException {

	private InjectionPoint injectionPoint;


	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param propertyName 无法满足的bean属性的名称
	 * @param msg the detail message
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, String propertyName, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through bean property '" + propertyName + "'" +
				(StringUtils.hasLength(msg) ? ": " + msg : ""));
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param propertyName 无法满足的bean属性的名称
	 * @param ex 表示不满足的依赖的bean创建异常
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, String propertyName, BeansException ex) {

		this(resourceDescription, beanName, propertyName, "");
		initCause(ex);
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param injectionPoint 注入点 (字段或方法/构造函数参数)
	 * @param msg the detail message
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, InjectionPoint injectionPoint, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through " + injectionPoint +
				(StringUtils.hasLength(msg) ? ": " + msg : ""));
		this.injectionPoint = injectionPoint;
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param injectionPoint 注入点 (字段或方法/构造函数参数)
	 * @param ex 表示不满足的依赖的bean创建异常
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, InjectionPoint injectionPoint, BeansException ex) {

		this(resourceDescription, beanName, injectionPoint, "");
		initCause(ex);
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param ctorArgIndex 无法满足的构造函数参数的索引
	 * @param ctorArgType 无法满足的构造函数参数的类型
	 * @param msg the detail message
	 * 
	 * @deprecated in favor of {@link #UnsatisfiedDependencyException(String, String, InjectionPoint, String)}
	 */
	@Deprecated
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, int ctorArgIndex, Class<?> ctorArgType, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through constructor argument with index " +
				ctorArgIndex + " of type [" + ClassUtils.getQualifiedName(ctorArgType) + "]" +
				(msg != null ? ": " + msg : ""));
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param ctorArgIndex 无法满足的构造函数参数的索引
	 * @param ctorArgType 无法满足的构造函数参数的类型
	 * @param ex 表示不满足的依赖的bean创建异常
	 * 
	 * @deprecated in favor of {@link #UnsatisfiedDependencyException(String, String, InjectionPoint, BeansException)}
	 */
	@Deprecated
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, int ctorArgIndex, Class<?> ctorArgType, BeansException ex) {

		this(resourceDescription, beanName, ctorArgIndex, ctorArgType, (ex != null ? ex.getMessage() : ""));
		initCause(ex);
	}


	/**
	 * 返回注入点 (字段或方法/构造函数参数).
	 */
	public InjectionPoint getInjectionPoint() {
		return this.injectionPoint;
	}

}
