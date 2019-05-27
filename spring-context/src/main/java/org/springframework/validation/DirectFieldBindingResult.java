package org.springframework.validation;

import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Errors和BindingResult接口的特殊实现, 支持在值对象上注册和评估绑定错误.
 * 执行直接字段访问, 而不是通过JavaBean getter.
 *
 * <p>从Spring 4.1开始, 此实现能够遍历嵌套字段.
 */
@SuppressWarnings("serial")
public class DirectFieldBindingResult extends AbstractPropertyBindingResult {

	private final Object target;

	private final boolean autoGrowNestedPaths;

	private transient ConfigurablePropertyAccessor directFieldAccessor;


	/**
	 * @param target 要绑定到的目标对象
	 * @param objectName 目标对象的名称
	 */
	public DirectFieldBindingResult(Object target, String objectName) {
		this(target, objectName, true);
	}

	/**
	 * @param target 要绑定到的目标对象
	 * @param objectName 目标对象的名称
	 * @param autoGrowNestedPaths 是否"自动增长"包含null值的嵌套路径
	 */
	public DirectFieldBindingResult(Object target, String objectName, boolean autoGrowNestedPaths) {
		super(objectName);
		this.target = target;
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}


	@Override
	public final Object getTarget() {
		return this.target;
	}

	/**
	 * 返回此实例使用的DirectFieldAccessor.
	 * 如果之前不存在, 则创建一个新的.
	 */
	@Override
	public final ConfigurablePropertyAccessor getPropertyAccessor() {
		if (this.directFieldAccessor == null) {
			this.directFieldAccessor = createDirectFieldAccessor();
			this.directFieldAccessor.setExtractOldValueForEditor(true);
			this.directFieldAccessor.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
		}
		return this.directFieldAccessor;
	}

	/**
	 * 为底层目标对象创建一个新的DirectFieldAccessor.
	 */
	protected ConfigurablePropertyAccessor createDirectFieldAccessor() {
		if (this.target == null) {
			throw new IllegalStateException("Cannot access fields on null target instance '" + getObjectName() + "'");
		}
		return PropertyAccessorFactory.forDirectFieldAccess(this.target);
	}

}
