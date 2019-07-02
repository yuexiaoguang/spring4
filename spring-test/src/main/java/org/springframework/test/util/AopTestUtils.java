package org.springframework.test.util;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;

/**
 * {@code AopTestUtils}是一组与AOP相关的工具方法, 用于单元和集成测试场景.
 *
 * <p>对于Spring的核心AOP实用程序, 请参阅
 * {@link org.springframework.aop.support.AopUtils AopUtils}
 * 和{@link org.springframework.aop.framework.AopProxyUtils AopProxyUtils}.
 */
public abstract class AopTestUtils {

	/**
	 * 获取提供的{@code candidate}对象的<em>目标</em>对象.
	 * <p>如果提供的{@code candidate}是Spring {@linkplain AopUtils#isAopProxy proxy}, 则将返回代理的目标;
	 * 否则, {@code candidate}将<em>按原样</em>返回.
	 * 
	 * @param candidate 要检查的实例 (可能是Spring AOP代理; never {@code null})
	 * 
	 * @return 目标对象或{@code candidate} (never {@code null})
	 * @throws IllegalStateException 如果在解包代理时发生错误
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getTargetObject(Object candidate) {
		Assert.notNull(candidate, "Candidate must not be null");
		try {
			if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised) {
				return (T) ((Advised) candidate).getTargetSource().getTarget();
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to unwrap proxied object", ex);
		}
		return (T) candidate;
	}

	/**
	 * 获取所提供的{@code candidate}对象的最<em>目标</em>对象, 不仅解包顶级代理, 还解包任意数量的嵌套代理.
	 * <p>如果提供的{@code candidate}是Spring {@linkplain AopUtils#isAopProxy proxy},
	 * 则将返回所有嵌套代理的最终目标; otherwise, the {@code candidate} will be returned <em>as is</em>.
	 * 
	 * @param candidate 要检查的实例 (可能是Spring AOP代理; never {@code null})
	 * 
	 * @return 目标对象或{@code candidate} (never {@code null})
	 * @throws IllegalStateException 如果在解包代理时发生错误
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getUltimateTargetObject(Object candidate) {
		Assert.notNull(candidate, "Candidate must not be null");
		try {
			if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised) {
				return (T) getUltimateTargetObject(((Advised) candidate).getTargetSource().getTarget());
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to unwrap proxied object", ex);
		}
		return (T) candidate;
	}

}
