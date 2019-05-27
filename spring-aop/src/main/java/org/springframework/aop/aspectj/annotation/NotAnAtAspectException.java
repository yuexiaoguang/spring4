package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.framework.AopConfigException;

/**
 * 尝试对不是AspectJ注解风格的切面的类执行切面生成操作时, 抛出的AopConfigException的扩展.
 */
@SuppressWarnings("serial")
public class NotAnAtAspectException extends AopConfigException {

	private Class<?> nonAspectClass;


	/**
	 * @param nonAspectClass 违规的类
	 */
	public NotAnAtAspectException(Class<?> nonAspectClass) {
		super(nonAspectClass.getName() + " is not an @AspectJ aspect");
		this.nonAspectClass = nonAspectClass;
	}

	/**
	 * 违规的类.
	 */
	public Class<?> getNonAspectClass() {
		return this.nonAspectClass;
	}

}
