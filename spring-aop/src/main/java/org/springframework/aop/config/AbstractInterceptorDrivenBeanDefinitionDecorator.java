package org.springframework.aop.config;

import java.util.List;

import org.w3c.dom.Node;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator BeanDefinitionDecorators}的基础实现类, 
 * 希望添加一个 {@link org.aopalliance.intercept.MethodInterceptor interceptor}到结果 bean.
 *
 * <p>此基类控制{@link ProxyFactoryBean} bean定义的创建, 并将原始文件包装为{@link ProxyFactoryBean}的{@code target}属性的内部bean定义.
 *
 * <p>正确处理链, 确保只创建一个{@link ProxyFactoryBean}定义.
 * 如果之前的{@link org.springframework.beans.factory.xml.BeanDefinitionDecorator}已经创建了
 * {@link org.springframework.aop.framework.ProxyFactoryBean}, 那么将拦截器简单地添加到现有定义中.
 *
 * <p>子类只需要为他们想要添加的拦截器创建{@code BeanDefinition}.
 */
public abstract class AbstractInterceptorDrivenBeanDefinitionDecorator implements BeanDefinitionDecorator {

	@Override
	public final BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definitionHolder, ParserContext parserContext) {
		BeanDefinitionRegistry registry = parserContext.getRegistry();

		// 获取root bean名称 - 将是生成的代理工厂bean的名称
		String existingBeanName = definitionHolder.getBeanName();
		BeanDefinition targetDefinition = definitionHolder.getBeanDefinition();
		BeanDefinitionHolder targetHolder = new BeanDefinitionHolder(targetDefinition, existingBeanName + ".TARGET");

		// 委托给拦截器定义的子类
		BeanDefinition interceptorDefinition = createInterceptorDefinition(node);

		// 生成名称并注册拦截器
		String interceptorName = existingBeanName + '.' + getInterceptorNameSuffix(interceptorDefinition);
		BeanDefinitionReaderUtils.registerBeanDefinition(
				new BeanDefinitionHolder(interceptorDefinition, interceptorName), registry);

		BeanDefinitionHolder result = definitionHolder;

		if (!isProxyFactoryBeanDefinition(targetDefinition)) {
			// 创建代理定义
			RootBeanDefinition proxyDefinition = new RootBeanDefinition();
			// 创建代理工厂bean定义
			proxyDefinition.setBeanClass(ProxyFactoryBean.class);
			proxyDefinition.setScope(targetDefinition.getScope());
			proxyDefinition.setLazyInit(targetDefinition.isLazyInit());
			// 设置目标
			proxyDefinition.setDecoratedDefinition(targetHolder);
			proxyDefinition.getPropertyValues().add("target", targetHolder);
			// 创建拦截器名称列表
			proxyDefinition.getPropertyValues().add("interceptorNames", new ManagedList<String>());
			// 从原始bean定义复制autowire设置.
			proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
			proxyDefinition.setPrimary(targetDefinition.isPrimary());
			if (targetDefinition instanceof AbstractBeanDefinition) {
				proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
			}
			// 将它包装在带有bean名称的BeanDefinitionHolder中
			result = new BeanDefinitionHolder(proxyDefinition, existingBeanName);
		}

		addInterceptorNameToList(interceptorName, result.getBeanDefinition());
		return result;
	}

	@SuppressWarnings("unchecked")
	private void addInterceptorNameToList(String interceptorName, BeanDefinition beanDefinition) {
		List<String> list = (List<String>)
				beanDefinition.getPropertyValues().getPropertyValue("interceptorNames").getValue();
		list.add(interceptorName);
	}

	private boolean isProxyFactoryBeanDefinition(BeanDefinition existingDefinition) {
		return ProxyFactoryBean.class.getName().equals(existingDefinition.getBeanClassName());
	}

	protected String getInterceptorNameSuffix(BeanDefinition interceptorDefinition) {
		return StringUtils.uncapitalize(ClassUtils.getShortName(interceptorDefinition.getBeanClassName()));
	}

	/**
	 * 子类应该实现此方法以返回他们希望应用于被装饰的bean的拦截器的{@code BeanDefinition}.
	 */
	protected abstract BeanDefinition createInterceptorDefinition(Node node);

}
