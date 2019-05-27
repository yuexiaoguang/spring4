package org.springframework.aop.support;

import java.io.Serializable;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.util.ObjectUtils;

/**
 * 用于保存增强的正则表达式方法切点的方便的类, 使它们成为{@link org.springframework.aop.Advisor}.
 *
 * <p>使用“pattern”和“patterns”传递属性配置此类. 这些类似于{@link AbstractRegexpMethodPointcut}的模式和模式属性.
 *
 * <p>可以委托给任何{@link AbstractRegexpMethodPointcut}子类.
 * 默认情况下, 将使用{@link JdkRegexpMethodPointcut}. 选择一个特定的, 覆盖{@link #createPointcut}方法.
 */
@SuppressWarnings("serial")
public class RegexpMethodPointcutAdvisor extends AbstractGenericPointcutAdvisor {

	private String[] patterns;

	private AbstractRegexpMethodPointcut pointcut;

	private final Object pointcutMonitor = new SerializableMonitor();


	public RegexpMethodPointcutAdvisor() {
	}

	/**
	 * 为给定的增强创建一个RegexpMethodPointcutAdvisor.
	 * 之后仍需要指定模式.
	 * 
	 * @param advice 要使用的增强
	 */
	public RegexpMethodPointcutAdvisor(Advice advice) {
		setAdvice(advice);
	}

	/**
	 * @param pattern 要使用的模式
	 * @param advice 要使用的增强
	 */
	public RegexpMethodPointcutAdvisor(String pattern, Advice advice) {
		setPattern(pattern);
		setAdvice(advice);
	}

	/**
	 * @param patterns 要使用的模式
	 * @param advice 要使用的增强
	 */
	public RegexpMethodPointcutAdvisor(String[] patterns, Advice advice) {
		setPatterns(patterns);
		setAdvice(advice);
	}


	/**
	 * 设置正则表达式定义要匹配的方法.
	 * <p>使用此方法或{@link #setPatterns}，而不是两个都用.
	 */
	public void setPattern(String pattern) {
		setPatterns(pattern);
	}

	/**
	 * 设置正则表达式定义要匹配的方法. 要传递给切点实现.
	 * <p>匹配将是所有这些的结合; 如果任何模式匹配, 切点匹配.
	 */
	public void setPatterns(String... patterns) {
		this.patterns = patterns;
	}


	/**
	 * 初始化此Advisor中保存的单例切点.
	 */
	@Override
	public Pointcut getPointcut() {
		synchronized (this.pointcutMonitor) {
			if (this.pointcut == null) {
				this.pointcut = createPointcut();
				this.pointcut.setPatterns(this.patterns);
			}
			return pointcut;
		}
	}

	/**
	 * 创建实际的切点: 默认情况下, 将使用{@link JdkRegexpMethodPointcut}.
	 * 
	 * @return Pointcut实例 (never {@code null})
	 */
	protected AbstractRegexpMethodPointcut createPointcut() {
		return new JdkRegexpMethodPointcut();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": advice [" + getAdvice() +
				"], pointcut patterns " + ObjectUtils.nullSafeToString(this.patterns);
	}


	/**
	 * 用于可序列化监视器对象的空类.
	 */
	private static class SerializableMonitor implements Serializable {
	}

}
