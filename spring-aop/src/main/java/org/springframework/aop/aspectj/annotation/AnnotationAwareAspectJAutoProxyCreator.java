package org.springframework.aop.aspectj.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;

/**
 * {@link AspectJAwareAdvisorAutoProxyCreator}子类, 处理当前应用程序上下文中的所有AspectJ注解方面, 以及Spring Advisor.
 *
 * <p>任何AspectJ注解的类都将自动被识别, 如果Spring AOP的基于代理的模型能够应用它, 那么它们的增强就适用了.
 * 这包括方法执行连接点.
 *
 * <p>如果使用 &lt;aop:include&gt; 元素, 只有名称与包含模式匹配的@AspectJ bean, 才会被视为定义用于Spring自动代理的切面.
 *
 * <p>Spring Advisor的处理遵循{@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}中建立的规则.
 */
@SuppressWarnings("serial")
public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {

	private List<Pattern> includePatterns;

	private AspectJAdvisorFactory aspectJAdvisorFactory;

	private BeanFactoryAspectJAdvisorsBuilder aspectJAdvisorsBuilder;


	/**
	 * 设置正则表达式模式列表, 匹配符合条件的@AspectJ bean名称.
	 * <p>默认是将所有@AspectJ bean视为合格.
	 */
	public void setIncludePatterns(List<String> patterns) {
		this.includePatterns = new ArrayList<Pattern>(patterns.size());
		for (String patternText : patterns) {
			this.includePatterns.add(Pattern.compile(patternText));
		}
	}

	public void setAspectJAdvisorFactory(AspectJAdvisorFactory aspectJAdvisorFactory) {
		Assert.notNull(aspectJAdvisorFactory, "AspectJAdvisorFactory must not be null");
		this.aspectJAdvisorFactory = aspectJAdvisorFactory;
	}

	@Override
	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		super.initBeanFactory(beanFactory);
		if (this.aspectJAdvisorFactory == null) {
			this.aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory(beanFactory);
		}
		this.aspectJAdvisorsBuilder =
				new BeanFactoryAspectJAdvisorsBuilderAdapter(beanFactory, this.aspectJAdvisorFactory);
	}


	@Override
	protected List<Advisor> findCandidateAdvisors() {
		// 添加根据超类规则找到的所有Spring切面.
		List<Advisor> advisors = super.findCandidateAdvisors();
		// 为bean工厂中的所有AspectJ切面构建增强.
		advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
		return advisors;
	}

	@Override
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		// 以前在构造函数中setProxyTargetClass(true), 但这种影响太大了. 相反，现在覆盖isInfrastructureClass以避免代理切面.
		// 我并不完全满意，因为没有充分的理由不增强切面, 除了它导致增强调用通过代理, 
		// 如果方面实现了例如Ordered接口, 它将由该接口代理并在运行时失败，因为没有在接口上定义advice方法. 可以放松对未来增强切面的限制.
		return (super.isInfrastructureClass(beanClass) || this.aspectJAdvisorFactory.isAspect(beanClass));
	}

	/**
	 * 检查给定的切面bean是否有资格进行自动代理.
	 * <p>如果未使用 &lt;aop:include&gt; 元素, 那么 "includePatterns"将会是{@code null}, 并包括所有bean.
	 * 如果"includePatterns"不是null, 那么其中一个模式必须匹配.
	 */
	protected boolean isEligibleAspectBean(String beanName) {
		if (this.includePatterns == null) {
			return true;
		}
		else {
			for (Pattern pattern : this.includePatterns) {
				if (pattern.matcher(beanName).matches()) {
					return true;
				}
			}
			return false;
		}
	}


	/**
	 * 委托给周围的AnnotationAwareAspectJAutoProxyCreator工具的BeanFactoryAspectJAdvisorsBuilderAdapter的子类.
	 */
	private class BeanFactoryAspectJAdvisorsBuilderAdapter extends BeanFactoryAspectJAdvisorsBuilder {

		public BeanFactoryAspectJAdvisorsBuilderAdapter(
				ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {

			super(beanFactory, advisorFactory);
		}

		@Override
		protected boolean isEligibleBean(String beanName) {
			return AnnotationAwareAspectJAutoProxyCreator.this.isEligibleAspectBean(beanName);
		}
	}
}
