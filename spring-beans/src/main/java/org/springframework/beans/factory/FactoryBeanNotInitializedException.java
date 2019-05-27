package org.springframework.beans.factory;

import org.springframework.beans.FatalBeanException;

/**
 * 如果bean尚未完全初始化, 则从FactoryBean的{@code getObject()}方法抛出异常,
 * 例如因为它涉及循环引用.
 *
 * <p>Note: 带有FactoryBean的循环引用无法通过像普通bean一样实时地缓存单例实例来解决.
 * 原因是每个FactoryBean都需要在返回创建的bean之前完全初始化,
 * 而只需要初始化特定的普通bean - 即, 如果协作bean实际上是在初始化时调用它们, 而不是仅存储引用.
 */
@SuppressWarnings("serial")
public class FactoryBeanNotInitializedException extends FatalBeanException {

	public FactoryBeanNotInitializedException() {
		super("FactoryBean is not fully initialized yet");
	}

	public FactoryBeanNotInitializedException(String msg) {
		super(msg);
	}
}
