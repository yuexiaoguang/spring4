package org.springframework.validation;

import java.io.Serializable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * {@link Errors}和{@link BindingResult}接口的默认实现, 用于注册和评估JavaBean对象上的绑定错误.
 *
 * <p>执行标准JavaBean属性访问, 也支持嵌套属性.
 * 通常, 应用程序代码将与{@code Errors}接口或{@code BindingResult}接口一起使用.
 * {@link DataBinder}通过{@link DataBinder#getBindingResult()}返回其{@code BindingResult}.
 */
@SuppressWarnings("serial")
public class BeanPropertyBindingResult extends AbstractPropertyBindingResult implements Serializable {

	private final Object target;

	private final boolean autoGrowNestedPaths;

	private final int autoGrowCollectionLimit;

	private transient BeanWrapper beanWrapper;


	/**
	 * @param target 要绑定到的目标bean
	 * @param objectName 目标对象的名称
	 */
	public BeanPropertyBindingResult(Object target, String objectName) {
		this(target, objectName, true, Integer.MAX_VALUE);
	}

	/**
	 * @param target 要绑定到的目标bean
	 * @param objectName 目标对象的名称
	 * @param autoGrowNestedPaths 是否"自动增长"包含null值的嵌套路径
	 * @param autoGrowCollectionLimit 数组和集合自动增长的限制
	 */
	public BeanPropertyBindingResult(Object target, String objectName, boolean autoGrowNestedPaths, int autoGrowCollectionLimit) {
		super(objectName);
		this.target = target;
		this.autoGrowNestedPaths = autoGrowNestedPaths;
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}


	@Override
	public final Object getTarget() {
		return this.target;
	}

	/**
	 * 返回此实例使用的{@link BeanWrapper}.
	 * 如果之前不存在, 则创建一个新的.
	 */
	@Override
	public final ConfigurablePropertyAccessor getPropertyAccessor() {
		if (this.beanWrapper == null) {
			this.beanWrapper = createBeanWrapper();
			this.beanWrapper.setExtractOldValueForEditor(true);
			this.beanWrapper.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
			this.beanWrapper.setAutoGrowCollectionLimit(this.autoGrowCollectionLimit);
		}
		return this.beanWrapper;
	}

	/**
	 * 为底层目标对象创建一个新的{@link BeanWrapper}.
	 */
	protected BeanWrapper createBeanWrapper() {
		if (this.target == null) {
			throw new IllegalStateException("Cannot access properties on null bean instance '" + getObjectName() + "'");
		}
		return PropertyAccessorFactory.forBeanPropertyAccess(this.target);
	}

}
