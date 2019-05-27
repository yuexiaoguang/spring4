package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * 前置建议的常用标记接口, 例如{@link MethodBeforeAdvice}.
 *
 * <p>Spring只支持方法前置增强. 虽然这不太可能改变, 如果需要, 此API旨在允许将来提供字段增强.
 */
public interface BeforeAdvice extends Advice {

}
