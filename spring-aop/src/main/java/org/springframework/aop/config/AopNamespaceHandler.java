package org.springframework.aop.config;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code aop}命名空间的{@code NamespaceHandler}.
 *
 * <p>为{@code <aop:config>}标签提供一个{@link org.springframework.beans.factory.xml.BeanDefinitionParser}.
 * 一个{@code config}标签可以包括嵌套的{@code pointcut}, {@code advisor} 和 {@code aspect}标签.
 *
 * <p>{@code pointcut}标签允许使用简单的语法创建命名的{@link AspectJExpressionPointcut} bean:
 * <pre class="code">
 * &lt;aop:pointcut id=&quot;getNameCalls&quot; expression=&quot;execution(* *..ITestBean.getName(..))&quot;/&gt;
 * </pre>
 *
 * <p>使用{@code advisor}标签, 你可以配置一个{@link org.springframework.aop.Advisor},
 * 并将它自动应用于{@link org.springframework.beans.factory.BeanFactory}中的所有相关bean.
 * {@code advisor}标签支持内联和引用{@link org.springframework.aop.Pointcut Pointcuts}:
 *
 * <pre class="code">
 * &lt;aop:advisor id=&quot;getAgeAdvisor&quot;
 *     pointcut=&quot;execution(* *..ITestBean.getAge(..))&quot;
 *     advice-ref=&quot;getAgeCounter&quot;/&gt;
 *
 * &lt;aop:advisor id=&quot;getNameAdvisor&quot;
 *     pointcut-ref=&quot;getNameCalls&quot;
 *     advice-ref=&quot;getNameCounter&quot;/&gt;</pre>
 */
public class AopNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * 为'{@code config}', '{@code spring-configured}', '{@code aspectj-autoproxy}'和'{@code scoped-proxy}'标签,
	 * 注册{@link BeanDefinitionParser BeanDefinitionParsers}.
	 */
	@Override
	public void init() {
		// In 2.0 XSD as well as in 2.1 XSD.
		registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
		registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
		registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());

		// Only in 2.0 XSD: moved to context namespace as of 2.1
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
	}

}
