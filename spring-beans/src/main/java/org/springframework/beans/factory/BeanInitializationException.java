package org.springframework.beans.factory;

import org.springframework.beans.FatalBeanException;

/**
 * 如果bean实现的工厂感知代码初始化失败, 抛出的异常.
 * Bean工厂方法本身抛出的BeansExceptions应该按原样传播.
 *
 * <p>请注意{@code afterPropertiesSet()}或自定义“init-method”可以抛出任何异常.
 */
@SuppressWarnings("serial")
public class BeanInitializationException extends FatalBeanException {

	public BeanInitializationException(String msg) {
		super(msg);
	}

	public BeanInitializationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
